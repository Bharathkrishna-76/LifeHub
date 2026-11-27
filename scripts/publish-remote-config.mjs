#!/usr/bin/env node
/**
 * Zero-dependency Firebase Remote Config auto-publisher for GitHub Actions.
 * Authenticates via a Google Service Account JSON (FIREBASE_SERVICE_ACCOUNT),
 * merges the 6 centralized APK update parameters into the live Remote Config template,
 * and publishes atomically using ETag optimistic concurrency.
 */

import crypto from "node:crypto";
import fs from "node:fs";

const SCOPE = "https://www.googleapis.com/auth/firebase.remoteconfig";
const TOKEN_URI = "https://oauth2.googleapis.com/token";

function parseServiceAccount(raw) {
  const trimmed = (raw || "").trim();
  if (!trimmed) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT environment variable is empty.");
  }
  const jsonText = trimmed.startsWith("{")
    ? trimmed
    : Buffer.from(trimmed, "base64").toString("utf8");

  let parsed;
  try {
    parsed = JSON.parse(jsonText);
  } catch (err) {
    throw new Error(
      `FIREBASE_SERVICE_ACCOUNT is not valid JSON (${err.message}). Paste the entire Service Account private key JSON from Firebase Console -> Project Settings -> Service accounts -> Generate new private key.`
    );
  }

  if (parsed.project_info && parsed.client) {
    throw new Error(
      "FIREBASE_SERVICE_ACCOUNT contains google-services.json instead of a Service Account Private Key JSON. Open Firebase Console -> Project Settings -> Service accounts -> Generate new private key and paste that JSON."
    );
  }

  if (!parsed.client_email || !parsed.private_key || !parsed.project_id) {
    throw new Error(
      "Service account JSON is missing client_email, private_key, or project_id."
    );
  }

  parsed.private_key = String(parsed.private_key).replace(/\\n/g, "\n");
  return parsed;
}

function base64Url(input) {
  return Buffer.from(input)
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

async function getAccessToken(serviceAccount) {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = base64Url(
    JSON.stringify({
      iss: serviceAccount.client_email,
      scope: SCOPE,
      aud: TOKEN_URI,
      iat: now,
      exp: now + 3600,
    })
  );

  const unsignedJwt = `${header}.${claim}`;
  const signer = crypto.createSign("RSA-SHA256");
  signer.update(unsignedJwt);
  signer.end();
  const signature = signer
    .sign(serviceAccount.private_key)
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");

  const assertion = `${unsignedJwt}.${signature}`;
  const body = new URLSearchParams({
    grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
    assertion,
  });

  const resp = await fetch(TOKEN_URI, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: body.toString(),
  });

  if (!resp.ok) {
    const errBody = await resp.text();
    throw new Error(
      `OAuth token exchange failed with HTTP ${resp.status}: ${errBody.slice(0, 300)}`
    );
  }
  const data = await resp.json();
  if (!data.access_token) {
    throw new Error("OAuth response did not include an access_token.");
  }
  return data.access_token;
}

function validateDownloadUrl(url) {
  let parsed;
  try {
    parsed = new URL(url);
  } catch {
    throw new Error("APK_DOWNLOAD_URL is not a valid URL.");
  }
  if (parsed.protocol !== "https:" || parsed.hostname.toLowerCase() !== "github.com") {
    throw new Error("APK_DOWNLOAD_URL must use https://github.com/...");
  }
  const lowerPath = parsed.pathname.toLowerCase();
  if (!lowerPath.includes("/releases/download/") || !lowerPath.endsWith(".apk")) {
    throw new Error("APK_DOWNLOAD_URL must point to a GitHub Releases .apk asset.");
  }
}

function resolveProjectId(serviceAccount) {
  if (process.env.FIREBASE_PROJECT_ID?.trim()) {
    return process.env.FIREBASE_PROJECT_ID.trim();
  }
  if (fs.existsSync("app/google-services.json")) {
    try {
      const gs = JSON.parse(fs.readFileSync("app/google-services.json", "utf8"));
      if (gs?.project_info?.project_id) {
        return gs.project_info.project_id;
      }
    } catch {
      // Fall back to service account project_id
    }
  }
  return serviceAccount.project_id;
}

async function main() {
  const serviceAccount = parseServiceAccount(process.env.FIREBASE_SERVICE_ACCOUNT);
  const projectId = resolveProjectId(serviceAccount);

  if (serviceAccount.project_id !== projectId) {
    console.warn(
      `::warning::Service account project_id (${serviceAccount.project_id}) differs from app/google-services.json project_id (${projectId}).`
    );
  }

  const latestVersionCode = Number.parseInt(process.env.LATEST_VERSION_CODE || "", 10);
  if (!Number.isFinite(latestVersionCode) || latestVersionCode < 1) {
    throw new Error("LATEST_VERSION_CODE must be a positive integer.");
  }

  const latestVersionName = (process.env.LATEST_VERSION_NAME || "").trim();
  if (!latestVersionName) {
    throw new Error("LATEST_VERSION_NAME is required.");
  }

  const apkDownloadUrl = (process.env.APK_DOWNLOAD_URL || "").trim();
  validateDownloadUrl(apkDownloadUrl);

  const releaseNotes =
    (process.env.RELEASE_NOTES || "").trim() ||
    `Version ${latestVersionName} improvements and bug fixes.`;
  const updateEnabled =
    (process.env.UPDATE_ENABLED || "true").trim().toLowerCase() === "true";
  const requestedMinVersionRaw = (process.env.MINIMUM_VERSION_CODE || "").trim();

  const accessToken = await getAccessToken(serviceAccount);
  const endpoint = `https://firebaseremoteconfig.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/remoteConfig`;

  // 1. Fetch current template + ETag to preserve existing unrelated parameters
  const getResp = await fetch(endpoint, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  let etag = "*";
  let template = { parameters: {} };

  if (getResp.status === 404) {
    console.log("No existing Remote Config template found; creating initial template.");
  } else if (!getResp.ok) {
    const errText = await getResp.text();
    throw new Error(
      `Failed to read Firebase Remote Config for project "${projectId}" (HTTP ${getResp.status}): ${errText.slice(0, 300)}`
    );
  } else {
    etag = getResp.headers.get("etag") || "*";
    template = (await getResp.json()) || {};
    template.parameters = template.parameters || {};
  }

  // Preserve existing minimum_version_code unless explicitly overridden in workflow input
  const existingMinCode = template.parameters.minimum_version_code?.defaultValue?.value || "1";
  let minimumVersionCode = existingMinCode;
  if (requestedMinVersionRaw) {
    const parsedMin = Number.parseInt(requestedMinVersionRaw, 10);
    if (!Number.isFinite(parsedMin) || parsedMin < 1) {
      throw new Error("MINIMUM_VERSION_CODE must be a positive integer when provided.");
    }
    minimumVersionCode = String(parsedMin);
  }

  template.parameters.latest_version_code = {
    defaultValue: { value: String(latestVersionCode) },
    description: "Integer Android versionCode of the newest APK release.",
    valueType: "NUMBER",
  };
  template.parameters.latest_version_name = {
    defaultValue: { value: latestVersionName },
    description: "Human-readable version name displayed to users.",
    valueType: "STRING",
  };
  template.parameters.apk_download_url = {
    defaultValue: { value: apkDownloadUrl },
    description: "Direct HTTPS GitHub Releases .apk asset URL.",
    valueType: "STRING",
  };
  template.parameters.release_notes = {
    defaultValue: { value: releaseNotes },
    description: "Changelog displayed in the in-app update dialog.",
    valueType: "STRING",
  };
  template.parameters.update_enabled = {
    defaultValue: { value: updateEnabled ? "true" : "false" },
    description: "Master switch to enable or pause in-app update prompts.",
    valueType: "BOOLEAN",
  };
  template.parameters.minimum_version_code = {
    defaultValue: { value: minimumVersionCode },
    description: "Versions strictly below this build number see a mandatory update screen.",
    valueType: "NUMBER",
  };

  // Strip server-managed version metadata before PUT
  delete template.version;

  // 2. Publish updated template atomically
  const putResp = await fetch(endpoint, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json; charset=utf-8",
      "If-Match": etag,
    },
    body: JSON.stringify(template),
  });

  if (!putResp.ok) {
    const errText = await putResp.text();
    throw new Error(
      `Failed to publish Firebase Remote Config (HTTP ${putResp.status}): ${errText.slice(0, 300)}`
    );
  }

  console.log(
    `Published Firebase Remote Config for project "${projectId}": ` +
      `v${latestVersionName} (code ${latestVersionCode}), minCode=${minimumVersionCode}, enabled=${updateEnabled}`
  );
}

main().catch((err) => {
  console.error(`::error::Remote Config auto-publish failed: ${err.message}`);
  process.exit(1);
});
