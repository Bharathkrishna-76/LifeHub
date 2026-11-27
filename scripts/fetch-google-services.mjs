#!/usr/bin/env node
/**
 * Ensures app/google-services.json exists during GitHub Actions builds when
 * Google AI Studio omits google-services.json from GitHub sync.
 *
 * Resolution order:
 * 1. Existing app/google-services.json in repo
 * 2. GOOGLE_SERVICES_JSON / GOOGLE_SERVICES_JSON_BASE64 secret (raw JSON or base64)
 * 3. FIREBASE_API_KEY secret (generates clean config for com.aistudio.lifehub.mzkpqr)
 * 4. FIREBASE_SERVICE_ACCOUNT secret (attempts Firebase Management API fetch)
 */

import crypto from "node:crypto";
import fs from "node:fs";

const OUT_PATH = "app/google-services.json";
const PROJECT_ID = "gen-lang-client-0916654373";
const PROJECT_NUMBER = "316108061298";
const APP_ID = "1:316108061298:android:6323dbe0b71b4e02f7e0eb";
const PACKAGE_NAME = "com.aistudio.lifehub.mzkpqr";

function tryParseGoogleServices(raw) {
  const trimmed = (raw || "").trim();
  if (!trimmed) return null;

  let candidate = trimmed;
  if (!trimmed.startsWith("{")) {
    const cleanedB64 = trimmed.replace(/\s+/g, "");
    if (!/^[A-Za-z0-9+/=]+$/.test(cleanedB64)) {
      return null;
    }
    candidate = Buffer.from(cleanedB64, "base64").toString("utf8").trim();
  }

  try {
    const parsed = JSON.parse(candidate);
    if (parsed?.project_info?.project_id && Array.isArray(parsed?.client)) {
      return parsed;
    }
  } catch {
    return null;
  }
  return null;
}

function buildFromApiKey(apiKey) {
  return {
    project_info: {
      project_number: PROJECT_NUMBER,
      project_id: PROJECT_ID,
      storage_bucket: `${PROJECT_ID}.firebasestorage.app`,
    },
    client: [
      {
        client_info: {
          mobilesdk_app_id: APP_ID,
          android_client_info: {
            package_name: PACKAGE_NAME,
          },
        },
        oauth_client: [
          {
            client_id: `${PROJECT_NUMBER}-tj7qa6r2r7n15vpf53p4hqpuvcdfeh3k.apps.googleusercontent.com`,
            client_type: 1,
            android_info: {
              package_name: PACKAGE_NAME,
              certificate_hash: "c6d6389f1e3f26b32073d16d674abf1be9411916",
            },
          },
          {
            client_id: `${PROJECT_NUMBER}-ulkmkil2448mhl54pi9b2skhtet9s1al.apps.googleusercontent.com`,
            client_type: 3,
          },
        ],
        api_key: [{ current_key: apiKey }],
        services: {
          appinvite_service: {
            other_platform_oauth_client: [
              {
                client_id: `${PROJECT_NUMBER}-ulkmkil2448mhl54pi9b2skhtet9s1al.apps.googleusercontent.com`,
                client_type: 3,
              },
            ],
          },
        },
      },
    ],
    configuration_version: "1",
  };
}

function base64Url(input) {
  return Buffer.from(input)
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

function parseServiceAccount(raw) {
  const trimmed = (raw || "").trim();
  if (!trimmed) return null;
  try {
    const jsonText = trimmed.startsWith("{")
      ? trimmed
      : Buffer.from(trimmed.replace(/\s+/g, ""), "base64").toString("utf8");
    const parsed = JSON.parse(jsonText);
    if (!parsed.client_email || !parsed.private_key || !parsed.project_id) {
      return null;
    }
    parsed.private_key = String(parsed.private_key).replace(/\\n/g, "\n");
    return parsed;
  } catch {
    return null;
  }
}

async function fetchConfigViaServiceAccount(sa) {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = base64Url(
    JSON.stringify({
      iss: sa.client_email,
      scope: "https://www.googleapis.com/auth/firebase.readonly https://www.googleapis.com/auth/cloud-platform.read-only",
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    })
  );
  const unsignedJwt = `${header}.${claim}`;
  const signer = crypto.createSign("RSA-SHA256");
  signer.update(unsignedJwt);
  signer.end();
  const signature = signer
    .sign(sa.private_key)
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");

  const tokenResp = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${unsignedJwt}.${signature}`,
    }).toString(),
  });
  if (!tokenResp.ok) {
    throw new Error(`OAuth token request failed (HTTP ${tokenResp.status})`);
  }
  const tokenJson = await tokenResp.json();
  const accessToken = tokenJson.access_token;
  if (!accessToken) throw new Error("Missing access_token");

  const projectId = sa.project_id || PROJECT_ID;
  const configUrl = `https://firebase.googleapis.com/v1beta1/projects/${encodeURIComponent(projectId)}/androidApps/${encodeURIComponent(APP_ID)}/config`;
  const cfgResp = await fetch(configUrl, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!cfgResp.ok) {
    const errText = await cfgResp.text();
    throw new Error(`Firebase Management API HTTP ${cfgResp.status}: ${errText.slice(0, 200)}`);
  }
  const cfgData = await cfgResp.json();
  if (!cfgData.configFileContents) {
    throw new Error("Missing configFileContents in Firebase response");
  }
  const decoded = Buffer.from(cfgData.configFileContents, "base64").toString("utf8");
  const parsed = JSON.parse(decoded);
  if (!parsed?.project_info?.project_id) {
    throw new Error("Decoded configFileContents is not valid google-services.json");
  }
  return parsed;
}

async function main() {
  if (fs.existsSync(OUT_PATH)) {
    const existing = tryParseGoogleServices(fs.readFileSync(OUT_PATH, "utf8"));
    if (existing) {
      console.log(`Using existing ${OUT_PATH} for project ${existing.project_info.project_id}`);
      return;
    }
  }

  const rawGs = process.env.GOOGLE_SERVICES_JSON || process.env.GOOGLE_SERVICES_JSON_BASE64 || "";
  if (rawGs.trim()) {
    const parsedGs = tryParseGoogleServices(rawGs);
    if (parsedGs) {
      fs.writeFileSync(OUT_PATH, JSON.stringify(parsedGs, null, 2));
      console.log(`Configured ${OUT_PATH} from GOOGLE_SERVICES_JSON secret (${parsedGs.project_info.project_id}).`);
      return;
    }
    console.warn("::warning::GOOGLE_SERVICES_JSON secret was not valid complete JSON; trying FIREBASE_API_KEY / FIREBASE_SERVICE_ACCOUNT fallback.");
  }

  const apiKey = (process.env.FIREBASE_API_KEY || "").trim();
  if (apiKey) {
    const generated = buildFromApiKey(apiKey);
    fs.writeFileSync(OUT_PATH, JSON.stringify(generated, null, 2));
    console.log(`Generated ${OUT_PATH} from FIREBASE_API_KEY secret for ${PACKAGE_NAME}.`);
    return;
  }

  const saRaw = process.env.FIREBASE_SERVICE_ACCOUNT || "";
  const sa = parseServiceAccount(saRaw);
  if (sa) {
    try {
      const fetched = await fetchConfigViaServiceAccount(sa);
      fs.writeFileSync(OUT_PATH, JSON.stringify(fetched, null, 2));
      console.log(`Auto-fetched ${OUT_PATH} from Firebase Management API (${fetched.project_info.project_id}).`);
      return;
    } catch (err) {
      console.warn(`::warning::Could not auto-fetch google-services.json via FIREBASE_SERVICE_ACCOUNT (${err.message}).`);
    }
  } else if (saRaw.trim()) {
    console.warn("::warning::FIREBASE_SERVICE_ACCOUNT is also not valid Service Account JSON.");
  }

  console.error(
    "::error::Could not configure app/google-services.json. Easiest fix: in GitHub Settings -> Secrets and variables -> Actions, add a secret named FIREBASE_API_KEY containing your 39-character Firebase Web/Android API key (starts with AIza...) from Firebase Console -> Project Settings."
  );
  process.exit(1);
}

main().catch((err) => {
  console.error(`::error::Failed to prepare app/google-services.json: ${err.message}`);
  process.exit(1);
});
