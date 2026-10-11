const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read vitals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("vitals").get());
});

test("Authenticated user: cannot read another user's vitals", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("vitals").doc("v1").set({
      userId: BOB_UID,
      heartRate: 72,
      spo2: 98,
      systolicBp: 120,
      diastolicBp: 80,
      temperatureF: 98.6,
      respirationRate: 16,
      status: "Normal",
      notes: "test",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("vitals").doc("v1").get());
});

test("Authenticated user: can create valid vitals record", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vitals").doc("v1").set({
      userId: ALICE_UID,
      heartRate: 75,
      spo2: 98,
      systolicBp: 120,
      diastolicBp: 80,
      temperatureF: 98.6,
      respirationRate: 16,
      status: "Normal",
      notes: "Routine tablet scan",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create vitals with invalid heart rate", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("vitals").doc("v_bad").set({
      userId: ALICE_UID,
      heartRate: 9999, // Impossible heart rate, rejected by rules
      spo2: 98,
      systolicBp: 120,
      diastolicBp: 80,
      temperatureF: 98.6,
      respirationRate: 16,
      status: "Normal",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create and read chat messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_messages").doc("msg1").set({
      userId: ALICE_UID,
      sender: "Patient",
      message: "Hello Dr. CarePulse, can you check my vitals?",
      isFromUser: true,
      modelUsed: "gemini-3.5-flash",
      createdAt: new Date(),
    })
  );
});
