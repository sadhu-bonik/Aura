const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");
const path = require("path");
const fs = require("fs");

const keyPath = process.argv[2];
if (!keyPath) {
  console.error("Usage: node seed-creators.js <path-to-service-account-key.json>");
  process.exit(1);
}

const serviceAccount = require(path.resolve(keyPath));
const app = initializeApp({
  credential: cert(serviceAccount),
  storageBucket: `${serviceAccount.project_id}.firebasestorage.app`,
});
const db = getFirestore();
const bucket = getStorage().bucket();

const creators = [
  { userId: "testCreator3", displayName: "Priya Designs", email: "creator3@test.local" },
  { userId: "testCreator4", displayName: "Marcus Builds", email: "creator4@test.local" },
  { userId: "testCreator5", displayName: "Lena Vlogs", email: "creator5@test.local" },
];

// Videos are assigned round-robin to creators.
// Place .mp4 files in this scripts/ folder before running.

async function deleteAllPortfolioItems() {
  console.log("Deleting existing portfolioItems...");
  const snap = await db.collection("portfolioItems").get();
  if (snap.empty) {
    console.log("  (none found)");
    return;
  }
  const batch = db.batch();
  snap.docs.forEach((doc) => batch.delete(doc.ref));
  await batch.commit();
  console.log(`  Deleted ${snap.size} docs.`);
}

async function uploadVideo(localPath, storagePath) {
  console.log(`  Uploading ${path.basename(localPath)} -> ${storagePath}`);
  await bucket.upload(localPath, {
    destination: storagePath,
    metadata: { contentType: "video/mp4" },
  });
  const file = bucket.file(storagePath);
  await file.makePublic();
  return `https://storage.googleapis.com/${bucket.name}/${storagePath}`;
}

async function seed() {
  // 1. Find local .mp4 files
  const scriptsDir = __dirname;
  const mp4Files = fs.readdirSync(scriptsDir)
    .filter((f) => f.toLowerCase().endsWith(".mp4"))
    .sort();

  if (mp4Files.length === 0) {
    console.error("No .mp4 files found in scripts/ folder. Add some videos and re-run.");
    process.exit(1);
  }
  console.log(`Found ${mp4Files.length} video(s): ${mp4Files.join(", ")}\n`);

  // 2. Delete existing portfolioItems
  await deleteAllPortfolioItems();

  // 3. Seed creators
  const creatorBatch = db.batch();
  const now = Timestamp.now();
  for (const c of creators) {
    creatorBatch.set(db.collection("users").doc(c.userId), {
      userId: c.userId,
      email: c.email,
      role: "creator",
      displayName: c.displayName,
      profileImageUrl: "",
      createdAt: now,
      isProfileComplete: true,
    }, { merge: true });
    console.log(`+ users/${c.userId}  (${c.displayName})`);
  }
  await creatorBatch.commit();

  // 4. Upload videos and create portfolioItems
  console.log("\nUploading videos to Firebase Storage...");
  const videoBatch = db.batch();

  for (let i = 0; i < mp4Files.length; i++) {
    const fileName = mp4Files[i];
    const creator = creators[i % creators.length];
    const localPath = path.join(scriptsDir, fileName);
    const storagePath = `portfolioItems/${creator.userId}/${fileName}`;

    const mediaUrl = await uploadVideo(localPath, storagePath);

    const ref = db.collection("portfolioItems").doc();
    const title = path.basename(fileName, ".mp4").replace(/[-_]/g, " ");
    videoBatch.set(ref, {
      itemId: ref.id,
      creatorId: creator.userId,
      title,
      description: "",
      mediaUrl,
      mediaType: "video",
      thumbnailUrl: "",
      isPublic: true,
      createdAt: new Timestamp(now.seconds - i * 60, 0),
    });
    console.log(`+ portfolioItems/${ref.id}  "${title}" -> ${creator.displayName}`);
  }

  await videoBatch.commit();
  console.log(`\nDone — ${creators.length} creators, ${mp4Files.length} videos seeded from Storage.`);
}

seed().catch((err) => { console.error(err); process.exit(1); });
