package com.shourov.apps.pacedream.feature.home.presentation.redesign

import com.shourov.apps.pacedream.feature.home.domain.models.RentedGearModel
import com.shourov.apps.pacedream.feature.home.domain.models.SplitStayModel
import com.shourov.apps.pacedream.feature.home.domain.models.rooms.RoomModel

/**
 * Mappers — domain models → presentational [Listing].
 *
 * Keep these small and total. Backend values may be partial, so we fall back
 * to sensible placeholders rather than throwing.
 */

/** RoomModel → Listing for the Spaces rail. */
internal fun RoomModel.toListing(): Listing {
    val primaryPrice = price?.firstOrNull()
    val unit = when (primaryPrice?.frequency?.lowercase()) {
        "hour", "hourly" -> "hr"
        "day", "daily", "night" -> "night"
        "week", "weekly" -> "wk"
        "month", "monthly" -> "mo"
        null -> "night"
        else -> primaryPrice.frequency.lowercase()
    }
    val badges = buildList {
        when (primaryPrice?.frequency?.lowercase()) {
            "hour", "hourly" -> add("hourly")
            "day", "daily", "night", "week", "weekly" -> add("daily")
        }
        if (available) add("instant")
    }
    val city = location.city.takeIf { it.isNotBlank() }
    val state = location.state.takeIf { it.isNotBlank() }
    val area = listOfNotNull(city, state).joinToString(", ").ifBlank { location.country }

    return Listing(
        id = id,
        title = title.takeIf { it.isNotBlank() } ?: summary.ifBlank { "Space" },
        area = area,
        price = primaryPrice?.amount ?: 0,
        unit = unit,
        rating = if (rating > 0) rating.toDouble() else 0.0,
        reviews = 0,
        host = owner,
        badges = badges,
        tag = if (available) null else "Unavailable",
        photos = gallery.images.size.coerceAtLeast(1),
        imageUrls = gallery.images,
    )
}

/** SplitStayModel → Listing for the "Split cost" rail. */
internal fun SplitStayModel.toListing(): Listing {
    val safeImages = images.orEmpty()
    val area = listOfNotNull(
        location?.takeIf { it.isNotBlank() },
        city?.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { "Shared stay" }

    val unit = priceUnit?.let {
        when {
            it.contains("night", ignoreCase = true) -> "seat"
            it.contains("month", ignoreCase = true) -> "mo"
            else -> "seat"
        }
    } ?: "seat"

    return Listing(
        id = _id.orEmpty(),
        title = name ?: "Split stay",
        area = area,
        price = price?.toInt() ?: 0,
        unit = unit,
        rating = rating?.toDouble() ?: 0.0,
        reviews = reviewCount ?: 0,
        host = hostName.orEmpty(),
        badges = buildList {
            add("split")
            if (roomType?.contains("shared", ignoreCase = true) == true) add("shared")
        },
        tag = maxGuests?.let { "${it - 1} seats left" },
        photos = safeImages.size.coerceAtLeast(1),
        imageUrls = safeImages,
    )
}

/** RentedGearModel → Listing for the Items rail. */
internal fun RentedGearModel.toListing(): Listing {
    val safeImages = images.orEmpty()
    val area = listOfNotNull(
        city?.takeIf { it.isNotBlank() },
        location.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { country.ifBlank { "Nearby" } }

    val badges = buildList {
        add("daily")
        add("pickup")
    }

    return Listing(
        id = id,
        title = name.ifBlank { product ?: "Item" },
        area = area,
        price = hourlyRate * 8, // rough day rate; backend daily rate isn't exposed on RentedGear
        unit = "day",
        rating = 0.0,
        reviews = 0,
        host = hostId,
        badges = badges,
        tag = null,
        photos = safeImages.size.coerceAtLeast(1),
        imageUrls = safeImages,
    )
}
