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

function validGamePayload(userId, now) {
  return {
    userId,
    title: "Pokemon Chrono Quartz",
    regionName: "Aethelgard",
    synopsis: "An HD-2D and Super FX2 hybrid adventure across console eras.",
    consoleMatrixPreset: "Omni-Era HD-2D + Super FX2 + 32X Voxel",
    activeChipsCsv: "Super FX2,SA-1,Sega 32X,SGB2 Border,MSU-1 Audio",
    pixel3dBlendRatio: 65,
    dexCount: 251,
    mapCount: 18,
    versionTag: "v1.2.0-FUSION",
    createdAt: now,
    updatedAt: now,
  };
}

function validDexPayload(userId, now) {
  return {
    userId,
    dexNumber: 1026,
    name: "Vectragon",
    category: "Super FX Wyrm Pokemon",
    primaryType: "Dragon",
    secondaryType: "Electric",
    abilityName: "Polygon Overclock",
    signatureMove: "Mode-7 Hyper Beam",
    loreDescription: "Born from fused 16-bit sprites and 3D wireframe co-processors.",
    baseHp: 95,
    baseAttack: 115,
    baseDefense: 90,
    baseSpAtk: 130,
    baseSpDef: 85,
    baseSpeed: 110,
    baseSpriteId: 384,
    fusionStylePreset: "SNES Super FX2 + GBA Vibrant + Switch Bloom",
    enhancementChip: "Super FX2 GSU-2",
    primaryHex: "#00F5D4",
    secondaryHex: "#FF2A85",
    createdAt: now,
    updatedAt: now,
  };
}

function validAssetPayload(userId, now) {
  return {
    userId,
    name: "Neo-Kanto Cyber Gym Tileset",
    category: "Tilesets",
    consoleLineage: "GBC Hi-Color + GBA + DS 3D + Switch HD-2D",
    chipAddon: "SA-1 + MSU-1 + Fan Voxelizer",
    spriteSeedId: 150,
    shaderFormula: "CRT_SCANLINE:0.25|VOXEL_DEPTH:0.75|SGB_PALETTE:CYBER_GOLD",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write fan_games, custom_dex, or custom_assets", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("fan_games").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("custom_dex").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("custom_assets").get());
});

test("Authenticated owner can create, list, and update fan_games with valid payload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("fan_games").doc("game_1");
  await assertSucceeds(docRef.set(validGamePayload(ALICE_UID, now)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("fan_games").get());
  await assertSucceeds(
    docRef.update({
      title: "Pokemon Chrono Quartz Deluxe",
      pixel3dBlendRatio: 80,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user access to fan_games, custom_dex, and custom_assets is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("fan_games").doc("game_1").set(validGamePayload(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("custom_dex").doc("dex_1").set(validDexPayload(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("custom_assets").doc("asset_1").set(validAssetPayload(ALICE_UID, now))
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("fan_games").doc("game_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("fan_games").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("custom_dex").doc("dex_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("custom_assets").doc("asset_1").get());
});

test("Shadow update with ghost field or invalid blend ratio is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("fan_games").doc("game_1");
  await assertSucceeds(docRef.set(validGamePayload(ALICE_UID, now)));

  // Ghost field injection
  await assertFails(docRef.update({ isAdmin: true, updatedAt: new Date() }));
  // Invalid blend ratio (> 100)
  await assertFails(docRef.update({ pixel3dBlendRatio: 150, updatedAt: new Date() }));
});
