package com.shourov.apps.pacedream.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shourov.apps.pacedream.core.database.migration.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates that MIGRATION_1_2 preserves cached bookings rows when the receipt
 * breakdown columns (subtotal / serviceFee / cleaningFee / taxAmount) are added
 * to the existing `bookings` table.  Exercises the migration function directly
 * against a v1-shaped SQLite database so the test is self-contained and does
 * not require a checked-in schema export for v1.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom1To2Test {

    @Test
    fun migrate1To2_preservesExistingRows_andAddsReceiptColumns() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "migration-test-${System.nanoTime()}.db"
        context.deleteDatabase(dbName)

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(V1_BOOKINGS_TABLE)
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                })
                .build()
        )

        try {
            val db = openHelper.writableDatabase
            db.execSQL(
                "INSERT INTO bookings (id, userName, totalPrice, status) " +
                    "VALUES ('b1', 'Ada', 99.0, 'CONFIRMED')"
            )

            // Sanity: receipt columns don't exist in v1 — querying them must fail.
            try {
                db.query("SELECT subtotal FROM bookings").close()
                fail("Expected SQL exception — subtotal column should not exist at v1")
            } catch (_: Exception) {
                // expected
            }

            // Run the exact migration shipped in production.
            MIGRATION_1_2.migrate(db)

            // The pre-existing row survives the ALTER TABLE pass intact.
            db.query("SELECT userName, totalPrice FROM bookings WHERE id = 'b1'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Ada", c.getString(0))
                assertEquals(99.0, c.getDouble(1), 0.0001)
            }

            // The four new columns exist and default to NULL for legacy rows.
            db.query(
                "SELECT subtotal, serviceFee, cleaningFee, taxAmount " +
                    "FROM bookings WHERE id = 'b1'"
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertTrue("subtotal should be null for legacy row", c.isNull(0))
                assertTrue("serviceFee should be null for legacy row", c.isNull(1))
                assertTrue("cleaningFee should be null for legacy row", c.isNull(2))
                assertTrue("taxAmount should be null for legacy row", c.isNull(3))
            }

            // New writes can populate the new columns.
            db.execSQL(
                "INSERT INTO bookings " +
                    "(id, userName, totalPrice, status, subtotal, serviceFee, cleaningFee, taxAmount) " +
                    "VALUES ('b2', 'Lin', 220.0, 'CONFIRMED', 180.0, 20.0, 10.0, 10.0)"
            )
            db.query(
                "SELECT subtotal, serviceFee, cleaningFee, taxAmount " +
                    "FROM bookings WHERE id = 'b2'"
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(180.0, c.getDouble(0), 0.0001)
                assertEquals(20.0, c.getDouble(1), 0.0001)
                assertEquals(10.0, c.getDouble(2), 0.0001)
                assertEquals(10.0, c.getDouble(3), 0.0001)
            }
        } finally {
            openHelper.close()
            context.deleteDatabase(dbName)
        }
    }

    companion object {
        private val V1_BOOKINGS_TABLE = """
            CREATE TABLE IF NOT EXISTS bookings (
                id TEXT NOT NULL PRIMARY KEY,
                userProfilePic INTEGER,
                userName TEXT,
                checkOutTime TEXT,
                checkInTime TEXT,
                bookingStatus TEXT,
                price TEXT,
                propertyImage TEXT,
                propertyName TEXT,
                hostName TEXT,
                currency TEXT,
                totalPrice REAL,
                startDate TEXT,
                endDate TEXT,
                status TEXT
            )
        """.trimIndent()
    }
}
