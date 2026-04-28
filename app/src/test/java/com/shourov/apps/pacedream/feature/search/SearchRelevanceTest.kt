package com.shourov.apps.pacedream.feature.search

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks in [SearchViewModel.rankByRelevance] semantics: drop
 * description-only false positives but preserve the server's
 * relevance ordering so paginated pages stay consistent with each
 * other.
 */
class SearchRelevanceTest {

    private fun item(
        id: String,
        title: String,
        category: String? = null,
        location: String? = null,
    ) = SearchResultItem(
        id = id,
        title = title,
        location = location,
        imageUrl = null,
        priceText = null,
        rating = null,
        category = category,
    )

    @Test
    fun `blank query is a passthrough`() {
        val items = listOf(item("a", "Beach loft"), item("b", "Garage"))
        assertEquals(items, SearchViewModel.rankByRelevance(items, "   "))
    }

    @Test
    fun `description-only matches are dropped when a structured match exists`() {
        // The first listing matches gym in title, the rest only "matched"
        // on description on the backend (so they appear unrelated to the
        // client-side title / category / location signals).
        val gym = item("gym", "City Gym Drop-In")
        val salonNearGym = item("salon", "Cute Salon")
        val storage = item("storage", "Garage Storage")
        val ranked = SearchViewModel.rankByRelevance(
            listOf(gym, salonNearGym, storage),
            "gym",
        )
        assertEquals(listOf(gym), ranked)
    }

    @Test
    fun `title beats category beats location within a page`() {
        // For sort=null queries we cannot trust the backend to return
        // results in relevance order, so the helper boosts strong
        // structured matches above weaker ones on the same page.
        val locationHit = item("c", "Studio loft", location = "Near the gym")
        val categoryHit = item("b", "Workout space", category = "Gym")
        val titleHit = item("a", "City Gym")
        val ranked = SearchViewModel.rankByRelevance(
            // Server order is intentionally weakest-first to prove the
            // re-sort runs.
            listOf(locationHit, categoryHit, titleHit),
            "gym",
        )
        assertEquals(listOf(titleHit, categoryHit, locationHit), ranked)
    }

    @Test
    fun `title startsWith outranks title contains within a page`() {
        val mid = item("mid", "Cool gym time")          // word-boundary in title (80)
        val start = item("start", "Gymnastics studio")  // startsWith title (100)
        val end = item("end", "Outdoor gym")            // word-boundary in title (80)
        val ranked = SearchViewModel.rankByRelevance(
            listOf(mid, start, end),
            "gym",
        )
        assertEquals(start, ranked.first())
        assertEquals(setOf(mid, end), ranked.drop(1).toSet())
    }

    @Test
    fun `no structured match falls back to the raw page`() {
        // If the entire page only matches on description we keep the
        // page as the server returned it instead of showing nothing.
        val a = item("a", "Cute salon")
        val b = item("b", "Garage parking")
        val ranked = SearchViewModel.rankByRelevance(listOf(a, b), "gym")
        assertEquals(listOf(a, b), ranked)
    }
}
