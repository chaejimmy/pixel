#!/usr/bin/env node

/**
 * PaceDream Production Database Cleanup Script
 *
 * Removes faker.js test/seed data from the production MongoDB database.
 *
 * Usage:
 *   node cleanup-seed-data.js --dry-run          # Preview what would be deleted
 *   node cleanup-seed-data.js --delete            # Actually delete matched listings
 *
 * Environment:
 *   MONGODB_URI  - MongoDB connection string (required)
 *   DB_NAME      - Database name (default: pacedream)
 */

const { MongoClient, ObjectId } = require("mongodb");

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

const MONGODB_URI = process.env.MONGODB_URI;
const DB_NAME = process.env.DB_NAME || "pacedream";
const COLLECTION = "properties"; // listings collection

// Faker.js product adjectives that appear in generated names
const FAKER_ADJECTIVES = [
  "Handcrafted",
  "Awesome",
  "Fantastic",
  "Gorgeous",
  "Incredible",
  "Intelligent",
  "Licensed",
  "Practical",
  "Refined",
  "Rustic",
  "Sleek",
  "Small",
  "Tasty",
  "Unbranded",
  "Generic",
  "Ergonomic",
  "Bespoke",
  "Electronic",
  "Fresh",
  "Oriental",
  "Luxurious",
  "Modern",
  "Recycled",
];

// Faker.js material/noun fragments commonly paired with the adjectives above
const FAKER_NOUNS = [
  "Granite",
  "Steel",
  "Metal",
  "Marble",
  "Gold",
  "Aluminum",
  "Bronze",
  "Concrete",
  "Cotton",
  "Frozen",
  "Plastic",
  "Rubber",
  "Soft",
  "Wooden",
  "Ball",
  "Shirt",
  "Tuna",
  "Table",
  "Chair",
  "Chicken",
  "Cheese",
  "Bacon",
  "Bike",
  "Car",
  "Computer",
  "Fish",
  "Gloves",
  "Hat",
  "Keyboard",
  "Mouse",
  "Pants",
  "Pizza",
  "Salad",
  "Sausages",
  "Shoes",
  "Soap",
  "Towels",
];

// Known exact faker.js product names spotted in production
const KNOWN_FAKER_TITLES = [
  "Handcrafted Granite Ball",
  "Awesome Steel Shirt",
  "Fantastic Metal Tuna",
  "Small Marble Table",
  "Fresh Gold Chair",
  "Unbranded Aluminum Chicken",
];

// Faker.js city-name suffixes (from faker.js locale helpers)
const FAKER_CITY_SUFFIXES = [
  "view",
  "bury",
  "shire",
  "burgh",
  "berg",
  "stad",
  "ville",
  "fort",
  "mouth",
  "field",
  "land",
  "port",
  "town",
  "chester",
  "furt",
  "haven",
];

// Known faker.js cities already spotted in production
const KNOWN_FAKER_CITIES = [
  "Alfonzoview",
  "Hermistonbury",
  "Gleichnershire",
];

// Draft / incomplete form data patterns
const DRAFT_TITLE_PATTERNS = [
  "Create share listing",
  "Create borrow listing",
  "Step 1 of",
  "Step 2 of",
  "Step 3 of",
  "Step 4 of",
];

// ---------------------------------------------------------------------------
// Build the MongoDB query
// ---------------------------------------------------------------------------

function buildMatchQuery() {
  // --- Criterion 1: Title matches known faker.js product names exactly ---
  const exactTitleConditions = KNOWN_FAKER_TITLES.map((t) => ({
    name: { $regex: escapeRegex(t), $options: "i" },
  }));

  // --- Criterion 1b: Title matches faker adjective + noun combos ---
  // Build regex patterns like "Handcrafted.*Granite|Handcrafted.*Ball|..."
  const adjectiveNounConditions = [];
  for (const adj of FAKER_ADJECTIVES) {
    for (const noun of FAKER_NOUNS) {
      adjectiveNounConditions.push({
        name: {
          $regex: `\\b${escapeRegex(adj)}\\b.*\\b${escapeRegex(noun)}\\b`,
          $options: "i",
        },
      });
    }
  }

  // --- Criterion 2: "Test Listing" with seed description ---
  const testListingCondition = {
    $and: [
      { name: { $regex: "Test Listing", $options: "i" } },
      {
        $or: [
          { description: { $regex: "Automated seed listing", $options: "i" } },
          { description: { $regex: "\\bseed\\b", $options: "i" } },
        ],
      },
    ],
  };

  // --- Criterion 3: Faker.js city names ---
  const knownFakerCityConditions = KNOWN_FAKER_CITIES.map((c) => ({
    "location.city": { $regex: `^${escapeRegex(c)}$`, $options: "i" },
  }));

  // Cities ending with faker suffixes combined with a capitalized prefix
  // e.g. "Alfonzoview", "Hermistonbury" — capital letter + letters + suffix
  const fakerCitySuffixPattern = FAKER_CITY_SUFFIXES.map(
    (s) => `[A-Z][a-z]+${escapeRegex(s)}`
  ).join("|");
  const fakerCitySuffixCondition = {
    "location.city": { $regex: `^(${fakerCitySuffixPattern})$` },
  };

  // --- Criterion 4: Draft / incomplete form data ---
  const draftConditions = DRAFT_TITLE_PATTERNS.map((p) => ({
    name: { $regex: escapeRegex(p), $options: "i" },
  }));

  // --- Criterion 5: host_id is null ---
  const nullHostCondition = { host_id: null };

  // Combine all criteria with $or
  return {
    $or: [
      ...exactTitleConditions,
      ...adjectiveNounConditions,
      testListingCondition,
      ...knownFakerCityConditions,
      fakerCitySuffixCondition,
      ...draftConditions,
      nullHostCondition,
    ],
  };
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

// ---------------------------------------------------------------------------
// Categorize a matched listing for readable output
// ---------------------------------------------------------------------------

function categorize(doc) {
  const name = (doc.name || "").toString();
  const desc = (doc.description || "").toString();
  const city = (doc.location && doc.location.city) || "";
  const reasons = [];

  // Check known faker titles
  for (const t of KNOWN_FAKER_TITLES) {
    if (name.toLowerCase().includes(t.toLowerCase())) {
      reasons.push(`faker title: "${t}"`);
    }
  }

  // Check adjective+noun combos (simplified check)
  for (const adj of FAKER_ADJECTIVES) {
    if (name.toLowerCase().includes(adj.toLowerCase())) {
      for (const noun of FAKER_NOUNS) {
        if (name.toLowerCase().includes(noun.toLowerCase())) {
          const combo = `${adj} ... ${noun}`;
          if (
            !reasons.some(
              (r) => r.startsWith("faker title:") || r.includes(combo)
            )
          ) {
            reasons.push(`faker adjective+noun combo: "${combo}"`);
          }
        }
      }
    }
  }

  // Test Listing with seed description
  if (name.toLowerCase().includes("test listing")) {
    if (
      desc.toLowerCase().includes("automated seed listing") ||
      /\bseed\b/i.test(desc)
    ) {
      reasons.push('test listing with "seed" description');
    }
  }

  // Faker city
  if (KNOWN_FAKER_CITIES.some((c) => c.toLowerCase() === city.toLowerCase())) {
    reasons.push(`known faker city: "${city}"`);
  }
  for (const suffix of FAKER_CITY_SUFFIXES) {
    const re = new RegExp(`^[A-Z][a-z]+${escapeRegex(suffix)}$`);
    if (re.test(city)) {
      if (!reasons.some((r) => r.startsWith("known faker city"))) {
        reasons.push(`faker-pattern city: "${city}"`);
      }
      break;
    }
  }

  // Draft patterns
  for (const p of DRAFT_TITLE_PATTERNS) {
    if (name.toLowerCase().includes(p.toLowerCase())) {
      reasons.push(`draft/incomplete form data: "${p}"`);
    }
  }

  // Null host_id
  if (doc.host_id == null) {
    reasons.push("host_id is null");
  }

  return reasons.length > 0 ? reasons.join("; ") : "matched query (uncategorized)";
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  const args = process.argv.slice(2);
  const dryRun = args.includes("--dry-run");
  const doDelete = args.includes("--delete");

  if (!dryRun && !doDelete) {
    console.error(
      "Usage: node cleanup-seed-data.js [--dry-run | --delete]\n" +
        "\n" +
        "  --dry-run   Query and log all matching listings without deleting\n" +
        "  --delete    Delete all matching listings (requires prior dry-run review)\n" +
        "\n" +
        "Environment variables:\n" +
        "  MONGODB_URI   MongoDB connection string (required)\n" +
        "  DB_NAME       Database name (default: pacedream)"
    );
    process.exit(1);
  }

  if (!MONGODB_URI) {
    console.error(
      "ERROR: MONGODB_URI environment variable is required.\n" +
        'Example: MONGODB_URI="mongodb+srv://user:pass@cluster.mongodb.net" node cleanup-seed-data.js --dry-run'
    );
    process.exit(1);
  }

  const mode = dryRun ? "DRY-RUN" : "DELETE";
  console.log(`\n========================================`);
  console.log(`  PaceDream Seed Data Cleanup [${mode}]`);
  console.log(`========================================\n`);
  console.log(`Database : ${DB_NAME}`);
  console.log(`Collection: ${COLLECTION}`);
  console.log(`Timestamp : ${new Date().toISOString()}\n`);

  const client = new MongoClient(MONGODB_URI);

  try {
    await client.connect();
    console.log("Connected to MongoDB.\n");

    const db = client.db(DB_NAME);
    const collection = db.collection(COLLECTION);

    const query = buildMatchQuery();

    // ---- Dry-run: find and report ----
    const matchedDocs = await collection
      .find(query, {
        projection: {
          _id: 1,
          name: 1,
          description: 1,
          "location.city": 1,
          "location.state": 1,
          host_id: 1,
          createdAt: 1,
        },
      })
      .toArray();

    if (matchedDocs.length === 0) {
      console.log("No matching seed/test listings found. Database is clean.");
      return;
    }

    console.log(`Found ${matchedDocs.length} listing(s) matching cleanup criteria:\n`);
    console.log("-".repeat(100));

    const auditLog = [];

    for (let i = 0; i < matchedDocs.length; i++) {
      const doc = matchedDocs[i];
      const id = doc._id.toString();
      const title = doc.name || "(no title)";
      const city =
        (doc.location && doc.location.city) || "(no city)";
      const state =
        (doc.location && doc.location.state) || "";
      const hostId = doc.host_id || "null";
      const reason = categorize(doc);

      console.log(
        `  ${(i + 1).toString().padStart(3)}. ID: ${id}`
      );
      console.log(`       Title   : ${title}`);
      console.log(
        `       Location: ${city}${state ? ", " + state : ""}`
      );
      console.log(`       Host ID : ${hostId}`);
      console.log(`       Reason  : ${reason}`);
      console.log();

      auditLog.push({
        _id: id,
        title,
        city,
        state,
        host_id: hostId,
        reason,
        timestamp: new Date().toISOString(),
      });
    }

    console.log("-".repeat(100));
    console.log(`\nTotal listings to be removed: ${matchedDocs.length}\n`);

    if (dryRun) {
      console.log(
        "DRY-RUN complete. No data was modified.\n" +
          "Review the listings above, then run with --delete to remove them."
      );

      // Write audit log to file for review
      const fs = require("fs");
      const logPath = `cleanup-dryrun-${Date.now()}.json`;
      fs.writeFileSync(
        logPath,
        JSON.stringify(auditLog, null, 2)
      );
      console.log(`\nDry-run report saved to: ${logPath}`);
      return;
    }

    // ---- Delete mode ----
    console.log("Proceeding with deletion...\n");

    const idsToDelete = matchedDocs.map((d) => d._id);

    // Delete from main collection
    const deleteResult = await collection.deleteMany({
      _id: { $in: idsToDelete },
    });

    console.log(
      `Deleted ${deleteResult.deletedCount} listing(s) from '${COLLECTION}' collection.`
    );

    // ---- Clean up related collections / caches ----
    const relatedCollections = [
      "bookings",
      "wishlists",
      "reviews",
      "messages",
      "search_cache",
      "property_cache",
    ];

    for (const relCol of relatedCollections) {
      try {
        const colExists = await db
          .listCollections({ name: relCol })
          .hasNext();
        if (!colExists) continue;

        const relCollection = db.collection(relCol);

        // Try property_id first, then listing_id as field name
        let relResult = await relCollection.deleteMany({
          property_id: { $in: idsToDelete.map((id) => id.toString()) },
        });

        if (relResult.deletedCount === 0) {
          relResult = await relCollection.deleteMany({
            listing_id: { $in: idsToDelete.map((id) => id.toString()) },
          });
        }

        // Also try with ObjectId references
        if (relResult.deletedCount === 0) {
          relResult = await relCollection.deleteMany({
            $or: [
              { property_id: { $in: idsToDelete } },
              { listing_id: { $in: idsToDelete } },
            ],
          });
        }

        if (relResult.deletedCount > 0) {
          console.log(
            `  Cleaned ${relResult.deletedCount} related record(s) from '${relCol}'.`
          );
        }
      } catch (err) {
        // Collection may not exist or have different schema — skip
        console.log(
          `  Skipped '${relCol}': ${err.message}`
        );
      }
    }

    // ---- Attempt to clear any Atlas Search index entries ----
    // Atlas Search indexes automatically sync with collection changes,
    // but if there's a manual search_index collection, clear it too.
    try {
      const searchIndexCol = db.collection("search_index");
      const searchResult = await searchIndexCol.deleteMany({
        $or: [
          { property_id: { $in: idsToDelete.map((id) => id.toString()) } },
          { property_id: { $in: idsToDelete } },
          { _id: { $in: idsToDelete } },
        ],
      });
      if (searchResult.deletedCount > 0) {
        console.log(
          `  Cleared ${searchResult.deletedCount} search index entry/entries.`
        );
      }
    } catch {
      // No manual search index — Atlas Search will auto-sync
    }

    // ---- Write audit log ----
    const fs = require("fs");
    const logPath = `cleanup-deleted-${Date.now()}.json`;
    const fullAuditLog = {
      operation: "delete",
      timestamp: new Date().toISOString(),
      database: DB_NAME,
      collection: COLLECTION,
      total_deleted: deleteResult.deletedCount,
      listings: auditLog,
    };
    fs.writeFileSync(logPath, JSON.stringify(fullAuditLog, null, 2));

    console.log(`\nAudit log saved to: ${logPath}`);
    console.log(
      `\nCleanup complete. ${deleteResult.deletedCount} seed/test listing(s) removed.`
    );
  } catch (err) {
    console.error("ERROR:", err.message);
    process.exit(1);
  } finally {
    await client.close();
    console.log("\nMongoDB connection closed.");
  }
}

main();
