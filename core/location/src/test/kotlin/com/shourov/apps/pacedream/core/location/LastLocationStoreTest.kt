package com.shourov.apps.pacedream.core.location

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Round-trips [SavedLocation] through [LastLocationStore] using
 * Robolectric's in-memory SharedPreferences — covers save / load /
 * clear and the coordinate-nullability quirk where a manually entered
 * city has no lat/lng.
 */
@RunWith(RobolectricTestRunner::class)
class LastLocationStoreTest {

    private lateinit var store: LastLocationStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = LastLocationStore(context)
        store.clear()
    }

    @Test
    fun `load returns null when nothing saved`() {
        assertNull(store.load())
    }

    @Test
    fun `save then load round-trips full payload`() {
        val saved = SavedLocation(
            label = "Brooklyn, NY",
            city = "Brooklyn",
            region = "NY",
            country = "USA",
            lat = 40.6782,
            lng = -73.9442,
        )
        store.save(saved)

        val loaded = store.load()
        assertNotNull(loaded)
        assertEquals("Brooklyn, NY", loaded!!.label)
        assertEquals("Brooklyn", loaded.city)
        assertEquals("NY", loaded.region)
        assertEquals("USA", loaded.country)
        assertEquals(40.6782, loaded.lat!!, 0.000001)
        assertEquals(-73.9442, loaded.lng!!, 0.000001)
    }

    @Test
    fun `save without coordinates yields null lat-lng on load`() {
        val saved = SavedLocation(
            label = "Manually typed place",
            city = "Manually typed place",
            region = "",
            country = "",
            lat = null,
            lng = null,
        )
        store.save(saved)

        val loaded = store.load()
        assertNotNull(loaded)
        assertNull(loaded!!.lat)
        assertNull(loaded.lng)
    }

    @Test
    fun `clear removes prior saves`() {
        store.save(
            SavedLocation(
                label = "Tokyo",
                city = "Tokyo",
                region = "",
                country = "Japan",
                lat = 35.0,
                lng = 139.0,
            )
        )
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun `save replaces previous coordinates with null when re-saved without them`() {
        store.save(
            SavedLocation(
                label = "Brooklyn",
                city = "Brooklyn",
                region = "NY",
                country = "USA",
                lat = 40.0,
                lng = -73.0,
            )
        )
        store.save(
            SavedLocation(
                label = "Free text",
                city = "Free text",
                region = "",
                country = "",
                lat = null,
                lng = null,
            )
        )
        val loaded = store.load()
        assertNotNull(loaded)
        assertEquals("Free text", loaded!!.label)
        assertNull(loaded.lat)
        assertNull(loaded.lng)
    }
}
