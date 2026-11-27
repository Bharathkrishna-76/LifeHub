const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_lifehub_123";
const BOB_UID = "bob_lifehub_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

test("Unauthenticated user cannot read or write any user-scoped collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = pastDate();

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set({
        userId: ALICE_UID,
        title: "Coffee",
        amount: 250,
        type: "EXPENSE",
        category: "FOOD",
        monthKey: "2026-11",
        timestampEpochMs: Date.now() - 2000,
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").get()
  );
});

test("Transactions: Kotlin-aligned create, range query for selected month, partial update, and security guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);
  const txRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("transactions")
    .doc("tx_nov_001");

  // Valid create (no @DocumentId `id` key, optional `note` omitted when blank/null)
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      title: "Matcha Latte",
      amount: 320.5,
      type: "EXPENSE",
      category: "FOOD",
      monthKey: "2026-11",
      timestampEpochMs: 1795500000000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Month-scoped range query ordered by timestampEpochMs DESC limited to 50
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .where("timestampEpochMs", ">=", 1793491200000)
      .where("timestampEpochMs", "<", 1796083200000)
      .orderBy("timestampEpochMs", "desc")
      .limit(50)
      .get()
  );

  // Incremental delta sync query ordered by updatedAt ASC (single-field index, Spark-plan friendly)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .where("updatedAt", ">", pastDate(60000))
      .orderBy("updatedAt", "asc")
      .limit(100)
      .get()
  );

  // Valid partial update (edit transaction) preserving createdAt and userId
  await assertSucceeds(
    txRef.update({
      title: "Iced Matcha Latte",
      amount: 350.0,
      category: "FOOD",
      note: "With oat milk",
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost field (shadow update)
  await assertFails(
    txRef.update({
      isVerified: true,
      updatedAt: pastDate(500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    txRef.update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Reject @DocumentId `id` in payload on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_bad_id")
      .set({
        id: "tx_bad_id",
        userId: ALICE_UID,
        title: "Ghost ID",
        amount: 100,
        type: "EXPENSE",
        category: "FOOD",
        monthKey: "2026-11",
        timestampEpochMs: 1795500000000,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_future")
      .set({
        userId: ALICE_UID,
        title: "Future Tx",
        amount: 100,
        type: "EXPENSE",
        category: "FOOD",
        monthKey: "2026-11",
        timestampEpochMs: 1795500000000,
        createdAt: new Date(Date.now() + 120000),
        updatedAt: new Date(Date.now() + 120000),
      })
  );

  // Cross-user isolation: Bob cannot read, list, update, or delete Alice's transactions
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_nov_001").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_nov_001").delete()
  );
});

test("Profile, Habits, Goals, Tasks, Reminders, Achievements, Settings: full CRUD & ownership isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);

  // Profile
  const profileRef = aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("main");
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Aarav",
      email: "aarav@example.com",
      monthlyBudget: 25000,
      currencySymbol: "₹",
      totalXp: 100,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    profileRef.update({ monthlyBudget: 30000, totalXp: 350, updatedAt: pastDate(1000) })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("profile").doc("main").get()
  );

  // Habit
  const habitRef = aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("hab_1");
  await assertSucceeds(
    habitRef.set({
      userId: ALICE_UID,
      title: "Morning Run",
      iconKey: "run",
      colorHex: "#00F5A0",
      targetDaysPerWeek: 5,
      currentStreak: 3,
      longestStreak: 7,
      completedDates: ["2026-11-23", "2026-11-24", "2026-11-25"],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    habitRef.update({
      currentStreak: 4,
      completedDates: ["2026-11-23", "2026-11-24", "2026-11-25", "2026-11-26"],
      updatedAt: pastDate(1000),
    })
  );

  // Goal
  const goalRef = aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("goal_1");
  await assertSucceeds(
    goalRef.set({
      userId: ALICE_UID,
      title: "New Laptop",
      category: "LAPTOP",
      targetAmount: 80000,
      savedAmount: 15000,
      completed: false,
      targetMonth: "2027-03",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    goalRef.update({ savedAmount: 80000, completed: true, updatedAt: pastDate(1000) })
  );

  // Task
  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1");
  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      title: "Pay credit card bill",
      cadence: "DAILY",
      priority: "HIGH",
      status: "TODO",
      dueDateEpochMs: 1795600000000,
      recurring: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    taskRef.update({ status: "IN_PROGRESS", updatedAt: pastDate(1000) })
  );

  // Reminder
  const remRef = aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc("rem_1");
  await assertSucceeds(
    remRef.set({
      userId: ALICE_UID,
      title: "Apartment Rent",
      category: "RENT",
      amount: 18000,
      dueDateEpochMs: 1796100000000,
      recurringMonthly: true,
      paid: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(remRef.update({ paid: true, updatedAt: pastDate(1000) }));

  // Achievement
  const achRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("achievements")
    .doc("first_expense");
  await assertSucceeds(
    achRef.set({
      userId: ALICE_UID,
      code: "first_expense",
      xpAwarded: 100,
      unlocked: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Settings
  const setRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("settings")
    .doc("preferences");
  await assertSucceeds(
    setRef.set({
      userId: ALICE_UID,
      themeMode: "DARK",
      notificationsEnabled: true,
      currencySymbol: "₹",
      monthlyBudget: 25000,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    setRef.update({ themeMode: "LIGHT", updatedAt: pastDate(1000) })
  );

  // Cross-user write denial on goals
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("goals")
      .doc("goal_1")
      .update({ savedAmount: 0, updatedAt: pastDate(1000) })
  );
});
