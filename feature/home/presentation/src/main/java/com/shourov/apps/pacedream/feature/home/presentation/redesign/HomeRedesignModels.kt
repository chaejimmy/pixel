package com.shourov.apps.pacedream.feature.home.presentation.redesign

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocalMoving
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material.icons.outlined.Work
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Home Redesign V1 — data model
 *
 * Primary taxonomy: Spaces / Items / Services.
 * Access patterns (hourly, daily, split, instant, delivery, etc.) are
 * secondary filter chips and inline badges on listing cards.
 */

enum class PrimaryType(val id: String, val label: String, val subtitle: String) {
    SPACES("spaces", "Spaces", "rooms & stays"),
    ITEMS("items", "Items", "gear & kit"),
    SERVICES("services", "Services", "help & skills"),
}

@Immutable
data class CategoryOption(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

@Immutable
data class AccessChip(
    val id: String,
    val label: String,
)

@Immutable
data class Listing(
    val id: String,
    val title: String,
    val area: String,
    val price: Int,
    val unit: String,
    val rating: Double,
    val reviews: Int,
    val host: String,
    val badges: List<String>,
    val tag: String? = null,
    val photos: Int = 4,
    val bookedToday: Int? = null,
    val dates: String? = null,
)

object HomeRedesignData {

    val SpaceCategories = listOf(
        CategoryOption("all",       "All",          Icons.Outlined.Apps),
        CategoryOption("meeting",   "Meeting room", Icons.Outlined.MeetingRoom),
        CategoryOption("desk",      "Desk",         Icons.Outlined.Work),
        CategoryOption("study",     "Study room",   Icons.Outlined.School),
        CategoryOption("nap",       "Nap pod",      Icons.Outlined.Bed),
        CategoryOption("apartment", "Apartment",    Icons.Outlined.Apartment),
        CategoryOption("short",     "Short stay",   Icons.Outlined.NightsStay),
        CategoryOption("luxury",    "Luxury",       Icons.Outlined.Hotel),
        CategoryOption("parking",   "Parking",      Icons.Outlined.LocalParking),
        CategoryOption("storage",   "Storage",      Icons.Outlined.Storage),
        CategoryOption("restroom",  "Restroom",     Icons.Outlined.Wc),
    )

    val ItemCategories = listOf(
        CategoryOption("all",     "All",     Icons.Outlined.Apps),
        CategoryOption("camera",  "Cameras", Icons.Outlined.PhotoCamera),
        CategoryOption("bike",    "Bikes",   Icons.Outlined.DirectionsBike),
        CategoryOption("drone",   "Drones",  Icons.Outlined.FlightTakeoff),
        CategoryOption("tools",   "Tools",   Icons.Outlined.Build),
        CategoryOption("outdoor", "Outdoor", Icons.Outlined.Park),
        CategoryOption("audio",   "Audio",   Icons.Outlined.Headphones),
    )

    val ServiceCategories = listOf(
        CategoryOption("all",      "All",         Icons.Outlined.Apps),
        CategoryOption("cleaning", "Cleaning",    Icons.Outlined.CleaningServices),
        CategoryOption("tutor",    "Tutoring",    Icons.Outlined.School),
        CategoryOption("photo",    "Photography", Icons.Outlined.CameraAlt),
        CategoryOption("repair",   "Repair",      Icons.Outlined.Build),
        CategoryOption("move",     "Moving",      Icons.Outlined.LocalMoving),
        CategoryOption("errand",   "Errands",     Icons.Outlined.DeliveryDining),
    )

    fun categoriesFor(type: PrimaryType): List<CategoryOption> = when (type) {
        PrimaryType.SPACES -> SpaceCategories
        PrimaryType.ITEMS -> ItemCategories
        PrimaryType.SERVICES -> ServiceCategories
    }

    val AccessChips: Map<PrimaryType, List<AccessChip>> = mapOf(
        PrimaryType.SPACES to listOf(
            AccessChip("hourly",   "By the hour"),
            AccessChip("daily",    "By the day"),
            AccessChip("split",    "Split cost"),
            AccessChip("instant",  "Instant book"),
            AccessChip("shared",   "Shared space"),
        ),
        PrimaryType.ITEMS to listOf(
            AccessChip("hourly",   "By the hour"),
            AccessChip("daily",    "By the day"),
            AccessChip("instant",  "Instant book"),
            AccessChip("delivery", "Delivery"),
            AccessChip("pickup",   "Pickup only"),
        ),
        PrimaryType.SERVICES to listOf(
            AccessChip("hourly",  "By the hour"),
            AccessChip("fixed",   "Fixed price"),
            AccessChip("instant", "Instant book"),
            AccessChip("onsite",  "On-site"),
            AccessChip("remote",  "Remote"),
        ),
    )

    data class BadgeMeta(val label: String)

    val BadgeMeta = mapOf(
        "hourly"   to BadgeMeta("Hourly"),
        "daily"    to BadgeMeta("Daily"),
        "split"    to BadgeMeta("Split cost"),
        "shared"   to BadgeMeta("Shared"),
        "instant"  to BadgeMeta("Instant book"),
        "delivery" to BadgeMeta("Delivery"),
        "pickup"   to BadgeMeta("Pickup"),
        "fixed"    to BadgeMeta("Fixed price"),
        "onsite"   to BadgeMeta("On-site"),
        "remote"   to BadgeMeta("Remote"),
    )

    val Listings: Map<PrimaryType, List<Listing>> = mapOf(
        PrimaryType.SPACES to listOf(
            Listing("sp1", "Sunlit loft desk",       "SoHo, New York",    14,  "hr",   4.92, 214, "Mira",  listOf("instant", "hourly"),  tag = "Superhost",    photos = 5, bookedToday = 18),
            Listing("sp2", "Quiet nap capsule",      "Shibuya, Tokyo",    9,   "hr",   4.88, 341, "Koji",  listOf("hourly", "instant"),  tag = "New",          photos = 4, bookedToday = 32),
            Listing("sp3", "Boardroom suite 12",     "Mission, SF",       42,  "hr",   4.71, 88,  "Alex",  listOf("hourly"),             photos = 6, bookedToday = 7),
            Listing("sp4", "Library study nook",     "Kreuzberg, Berlin", 7,   "hr",   4.95, 612, "Lena",  listOf("hourly", "shared"),   tag = "Popular",      photos = 5, bookedToday = 41),
            Listing("sp5", "Cabin weekend · 3 of 6", "Catskills, NY",     84,  "seat", 4.90, 51,  "Jesse", listOf("split"),              tag = "3 seats left", photos = 6, dates = "May 9 – 12"),
            Listing("sp6", "Seaside villa · 5 of 8", "Tulum, Mexico",     112, "seat", 4.85, 140, "Ana",   listOf("split", "daily"),     photos = 7, dates = "Jun 14 – 20"),
        ),
        PrimaryType.ITEMS to listOf(
            Listing("it1", "Canon R6 Mark II",  "Returns today",  38, "day", 4.90, 120, "Omar", listOf("instant", "daily", "pickup"), tag = "Instant book", photos = 4),
            Listing("it2", "Trek Verve 3 bike", "0.4 mi away",    22, "day", 4.80, 77,  "Nina", listOf("daily", "pickup"),            tag = "Available",    photos = 3),
            Listing("it3", "DJI Mini 4 Pro",    "1.2 mi away",    55, "day", 4.95, 204, "Sam",  listOf("daily", "delivery"),          photos = 5),
            Listing("it4", "Helinox Chair Two", "0.8 mi away",    6,  "day", 4.70, 45,  "Eva",  listOf("daily", "pickup"),            photos = 3),
        ),
        PrimaryType.SERVICES to listOf(
            Listing("sv1", "Apartment deep clean",   "Brooklyn · On-site",  90,  "visit",   4.93, 312, "Rosa", listOf("fixed", "onsite"),              tag = "Top rated", photos = 3),
            Listing("sv2", "SAT math tutoring",      "Remote · 1-on-1",     45,  "hr",      4.97, 180, "Yuki", listOf("hourly", "remote", "instant"),  photos = 2),
            Listing("sv3", "Portrait photo session", "SoHo · Outdoors",     180, "session", 4.88, 64,  "Luca", listOf("fixed", "onsite"),              tag = "Featured",  photos = 5),
            Listing("sv4", "Bike repair & tune-up",  "2 mi radius",         35,  "fixed",   4.82, 94,  "Theo", listOf("fixed", "onsite"),              photos = 2),
        ),
    )

    fun listingsFor(type: PrimaryType, access: String?): List<Listing> {
        val all = Listings[type].orEmpty()
        return if (access == null) all else all.filter { access in it.badges }
    }
}
