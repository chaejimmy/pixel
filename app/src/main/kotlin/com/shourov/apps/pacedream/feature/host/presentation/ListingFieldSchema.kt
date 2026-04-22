package com.shourov.apps.pacedream.feature.host.presentation

import com.shourov.apps.pacedream.model.PricingUnit

/**
 * Schema-driven field registry for the Create Listing wizard.
 *
 * Every subcategory (parking, camera, gym, etc.) declares exactly which
 * fields it supports. The wizard UI reads from this registry to decide
 * what to render, what to validate, and what to send to the backend.
 *
 * Goal: stop leaking unrelated fields (bedrooms on a parking listing,
 * guest capacity on a camera rental, etc.) and give each category the
 * shape its domain actually needs.
 */

/** The three top-level resource categories. */
enum class ListingCategory { SPACE, ITEM, SERVICE }

/**
 * Canonical enumeration of every field the wizard can render.
 * Each [SubcategorySchema] opts into a subset of these.
 */
enum class ListingField {
    // ── Universal ─────────────────────────────────────
    TITLE,
    DESCRIPTION,
    PHOTOS,
    PRICE,
    PRICING_UNIT,
    AMENITIES,

    // ── Location (skipped entirely for abstract services without a place) ──
    LOCATION,

    // ── Space / stay capacity (ONLY for accommodation-style spaces) ───────
    MAX_GUESTS,
    BEDROOMS,
    BATHROOMS,

    // ── Parking-specific ────────────────────────────────────────
    VEHICLE_CAPACITY,
    PARKING_COVERED,
    PARKING_EV_CHARGING,
    PARKING_ACCESS_24_7,
    PARKING_SIZE_LIMIT,
    PARKING_SECURITY_FEATURES,

    // ── Item / rental gear ─────────────────────────────────────
    DEPOSIT,
    CONDITION,
    PICKUP_DELIVERY,

    // ── Service ────────────────────────────────────────────────
    SERVICE_DURATION_MINUTES,

    // ── Wi-Fi access (Wi-Fi subcategory only) ─────────────────
    /**
     * Marks a subcategory as a Wi-Fi / internet-access product. The wizard
     * surfaces a dedicated "Wi-Fi Access" step (SSID, password, reveal
     * rules, experience tags, guest preview) and reframes the schedule
     * step as access validity rather than generic operating hours.
     */
    WIFI_ACCESS,

    // ── Schedule / availability ────────────────────────────────
    SCHEDULE_HOURLY_DURATIONS,
    SCHEDULE_HOURS_OF_OPERATION,
    SCHEDULE_CHECK_IN_OUT,
    SCHEDULE_STAY_LIMITS,
    SCHEDULE_MIN_MONTHS,
    SCHEDULE_AVAILABLE_FROM,
    SCHEDULE_AVAILABLE_DAYS,
    SCHEDULE_TIMEZONE,

    // ── Split-specific ─────────────────────────────────────────
    SPLIT_DEADLINE,
    SPLIT_REQUIREMENTS,
    SPLIT_TOTAL_COST,
}

/**
 * Declarative per-subcategory schema. Anything not in [fields] is
 * neither rendered nor validated nor sent to the backend.
 */
data class SubcategorySchema(
    val id: String,
    val category: ListingCategory,
    val displayLabel: String,
    val fields: Set<ListingField>,
    val allowedPricingUnits: List<PricingUnit>,
    /** Display-only copy for Step 1 Pricing. */
    val defaultPricingUnit: PricingUnit = allowedPricingUnits.first(),
    /**
     * Curated amenity suggestions for this subcategory.
     * Empty list hides the amenities section entirely.
     */
    val amenityOptions: List<String> = emptyList(),
    /** Subcategory needs schedule/availability step. */
    val needsSchedule: Boolean = false,
) {
    fun hasField(field: ListingField): Boolean = field in fields
}

/**
 * Field groups that get re-used across many subcategories.
 */
private object FieldSets {
    val Core: Set<ListingField> = setOf(
        ListingField.TITLE,
        ListingField.DESCRIPTION,
        ListingField.PHOTOS,
        ListingField.PRICE,
        ListingField.PRICING_UNIT,
    )

    val PhysicalPlace: Set<ListingField> = setOf(ListingField.LOCATION)

    val StayCapacity: Set<ListingField> = setOf(
        ListingField.MAX_GUESTS,
        ListingField.BEDROOMS,
        ListingField.BATHROOMS,
    )

    val Parking: Set<ListingField> = setOf(
        ListingField.VEHICLE_CAPACITY,
        ListingField.PARKING_COVERED,
        ListingField.PARKING_EV_CHARGING,
        ListingField.PARKING_ACCESS_24_7,
        ListingField.PARKING_SIZE_LIMIT,
        ListingField.PARKING_SECURITY_FEATURES,
    )

    val ItemRental: Set<ListingField> = setOf(
        ListingField.DEPOSIT,
        ListingField.CONDITION,
        ListingField.PICKUP_DELIVERY,
    )

    val Service: Set<ListingField> = setOf(
        ListingField.SERVICE_DURATION_MINUTES,
    )

    val HourlySchedule: Set<ListingField> = setOf(
        ListingField.SCHEDULE_HOURLY_DURATIONS,
        ListingField.SCHEDULE_HOURS_OF_OPERATION,
        ListingField.SCHEDULE_AVAILABLE_DAYS,
        ListingField.SCHEDULE_TIMEZONE,
    )

    val StaySchedule: Set<ListingField> = setOf(
        ListingField.SCHEDULE_STAY_LIMITS,
        ListingField.SCHEDULE_CHECK_IN_OUT,
        ListingField.SCHEDULE_AVAILABLE_DAYS,
        ListingField.SCHEDULE_TIMEZONE,
    )

    val MonthlySchedule: Set<ListingField> = setOf(
        ListingField.SCHEDULE_MIN_MONTHS,
        ListingField.SCHEDULE_AVAILABLE_FROM,
        ListingField.SCHEDULE_TIMEZONE,
    )
}

object ListingSchemaRegistry {

    // ── Space subcategories ─────────────────────────────────────

    private val Parking = SubcategorySchema(
        id = "parking",
        category = ListingCategory.SPACE,
        displayLabel = "Parking",
        fields = FieldSets.Core +
            FieldSets.PhysicalPlace +
            FieldSets.Parking +
            ListingField.AMENITIES +
            FieldSets.HourlySchedule,
        allowedPricingUnits = listOf(PricingUnit.HOUR, PricingUnit.DAY),
        amenityOptions = listOf(
            "Well Lit", "Gated Access", "Security Camera", "Valet Service",
        ),
        needsSchedule = true,
    )

    private val EvParking = SubcategorySchema(
        id = "ev_parking",
        category = ListingCategory.SPACE,
        displayLabel = "EV Parking",
        fields = FieldSets.Core +
            FieldSets.PhysicalPlace +
            FieldSets.Parking +
            ListingField.AMENITIES +
            FieldSets.HourlySchedule,
        allowedPricingUnits = listOf(PricingUnit.HOUR, PricingUnit.DAY),
        amenityOptions = listOf(
            "Level 2 Charger", "DC Fast Charger", "Tesla Compatible", "Well Lit",
        ),
        needsSchedule = true,
    )

    private val Restroom = SubcategorySchema(
        id = "restroom",
        category = ListingCategory.SPACE,
        displayLabel = "Restroom",
        fields = FieldSets.Core + FieldSets.PhysicalPlace +
            ListingField.AMENITIES + FieldSets.HourlySchedule,
        allowedPricingUnits = listOf(PricingUnit.HOUR),
        amenityOptions = listOf(
            "Toilet Paper", "Hand Soap", "Hand Towels", "Air Freshener", "Paper Towels",
        ),
        needsSchedule = true,
    )

    private val NapPod = SubcategorySchema(
        id = "nap_pod",
        category = ListingCategory.SPACE,
        displayLabel = "Nap pod",
        fields = FieldSets.Core + FieldSets.PhysicalPlace +
            ListingField.AMENITIES + FieldSets.HourlySchedule,
        allowedPricingUnits = listOf(PricingUnit.HOUR),
        amenityOptions = listOf(
            "Noise Cancellation", "Soundproof Walls", "White Noise Machine",
            "Earplugs", "Calming Music", "Reclining Chair", "Blanket", "Pillow",
            "Eye Mask", "Temperature Control",
        ),
        needsSchedule = true,
    )

    private val MeetingRoom = SubcategorySchema(
        id = "meeting_room",
        category = ListingCategory.SPACE,
        displayLabel = "Meeting room",
        fields = FieldSets.Core +
            FieldSets.PhysicalPlace +
            setOf(ListingField.MAX_GUESTS) +
            ListingField.AMENITIES +
            FieldSets.HourlySchedule,
        allowedPricingUnits = listOf(PricingUnit.HOUR, PricingUnit.DAY),
        amenityOptions = listOf(
            "WiFi", "Power Outlets", "Projector", "Whiteboard", "Monitor/TV",
            "Desk Space", "Chairs",
        ),
        needsSchedule = true,
    )

    private val Gym = SubcategorySchema(
        id = "gym",
        category = ListingCategory.SPACE,
        displayLabel = "Gym",
        fields = FieldSets.Core + FieldSets.PhysicalPlace +
            ListingField.AMENITIES + FieldSets.HourlySchedule,
        allowedPricingUnits = listOf(PricingUnit.HOUR, PricingUnit.DAY),
        amenityOptions = listOf(
            "Exercise Equipment", "Locker Room", "Showers", "Water Fountain",
            "Towel Service", "Air Conditioning",
        ),
        needsSchedule = true,
    )

    private val ShortStay = SubcategorySchema(
        id = "short_stay",
        category = ListingCategory.SPACE,
        displayLabel = "Short stay",
        fields = FieldSets.Core + FieldSets.PhysicalPlace +
            FieldSets.StayCapacity +
            ListingField.AMENITIES + FieldSets.StaySchedule,
        allowedPricingUnits = listOf(PricingUnit.DAY, PricingUnit.WEEK),
        amenityOptions = listOf(
            "WiFi", "Kitchen", "Parking", "Washer/Dryer", "Heating",
            "Air Conditioning", "TV", "Bathroom Essentials", "Bed Linens",
        ),
        needsSchedule = true,
    )

    private val Apartment = SubcategorySchema(
        id = "apartment",
        category = ListingCategory.SPACE,
        displayLabel = "Apartment",
        fields = FieldSets.Core + FieldSets.PhysicalPlace +
            FieldSets.StayCapacity +
            ListingField.AMENITIES + FieldSets.MonthlySchedule + FieldSets.StaySchedule,
        allowedPricingUnits = listOf(PricingUnit.DAY, PricingUnit.WEEK, PricingUnit.MONTH),
        amenityOptions = listOf(
            "WiFi", "Kitchen", "Parking", "Washer/Dryer", "Heating",
            "Air Conditioning", "TV", "Bathroom Essentials", "Bed Linens",
        ),
        needsSchedule = true,
    )

    /**
     * Wi-Fi access is the product, not an amenity. The wizard opts out of
     * the generic AMENITIES picker and opts into [ListingField.WIFI_ACCESS]
     * so the dedicated SSID / password / experience-tag step replaces
     * "pick a few amenity chips" with something guests actually value.
     */
    private val Wifi = SubcategorySchema(
        id = "wifi",
        category = ListingCategory.SPACE,
        displayLabel = "Wi-Fi",
        fields = FieldSets.Core + FieldSets.PhysicalPlace +
            ListingField.WIFI_ACCESS + FieldSets.HourlySchedule,
        allowedPricingUnits = listOf(PricingUnit.HOUR, PricingUnit.DAY),
        amenityOptions = emptyList(),
        needsSchedule = true,
    )

    private val StorageSpace = SubcategorySchema(
        id = "storage_space",
        category = ListingCategory.SPACE,
        displayLabel = "Storage space",
        fields = FieldSets.Core + FieldSets.PhysicalPlace +
            ListingField.AMENITIES + FieldSets.MonthlySchedule,
        allowedPricingUnits = listOf(PricingUnit.DAY, PricingUnit.WEEK, PricingUnit.MONTH),
        amenityOptions = listOf(
            "Climate Controlled", "Security Camera", "Gated Access", "24/7 Access",
            "Ground Floor", "Drive-Up Access",
        ),
        needsSchedule = true,
    )

    private val SpaceOthers = SubcategorySchema(
        id = "others",
        category = ListingCategory.SPACE,
        displayLabel = "Other space",
        fields = FieldSets.Core + FieldSets.PhysicalPlace +
            ListingField.AMENITIES + FieldSets.HourlySchedule,
        allowedPricingUnits = listOf(PricingUnit.HOUR, PricingUnit.DAY),
        amenityOptions = listOf("WiFi", "AC", "Parking", "Clean", "Accessible"),
        needsSchedule = true,
    )

    // ── Item subcategories ──────────────────────────────────────

    private fun itemSchema(
        id: String,
        displayLabel: String,
        amenityOptions: List<String>,
    ) = SubcategorySchema(
        id = id,
        category = ListingCategory.ITEM,
        displayLabel = displayLabel,
        fields = FieldSets.Core +
            FieldSets.ItemRental +
            ListingField.AMENITIES +
            FieldSets.HourlySchedule,
        allowedPricingUnits = listOf(PricingUnit.DAY, PricingUnit.WEEK),
        amenityOptions = amenityOptions,
        needsSchedule = true,
    )

    private val SportsGear = itemSchema(
        "sports_gear", "Sports gear",
        listOf("Adjustable Straps", "Carrying Case", "Extra Batteries", "Charger", "Quick Release"),
    )
    private val Camera = itemSchema(
        "camera", "Camera",
        listOf("Lens Included", "Extra Batteries", "Memory Card", "Camera Bag", "Tripod", "Filters", "Remote Control"),
    )
    private val Tech = itemSchema(
        "tech", "Tech",
        listOf("Charger Included", "Case/Cover", "Screen Protector", "Headphones", "Warranty", "Extra Cable"),
    )
    private val Instrument = itemSchema(
        "instrument", "Instrument",
        listOf("Case/Bag", "Strap Included", "Tuner", "Extra Strings", "Metronome", "Stand"),
    )
    private val Tools = itemSchema(
        "tools", "Tools",
        listOf("Carrying Case", "Safety Gear", "Extra Blades", "Charger", "Manual Included", "Extension Cord"),
    )
    private val Games = itemSchema(
        "games", "Games",
        listOf("All Pieces Included", "Instructions", "Extra Controllers", "Carrying Case", "Batteries"),
    )
    private val Toys = itemSchema(
        "toys", "Toys",
        listOf("Batteries Included", "All Parts Included", "Carrying Case", "Safety Certified", "Instructions"),
    )
    private val Micromobility = itemSchema(
        "micromobility", "Micromobility",
        listOf("Helmet Included", "Lock Included", "Charger", "Lights", "Bell", "Basket"),
    )
    private val ItemOthers = itemSchema(
        "others_item", "Other item",
        emptyList(),
    )

    // ── Service subcategories ──────────────────────────────────

    /**
     * Services live at the ergonomic level — no bedrooms, no location
     * capacity.  The host may travel to the guest, so location is optional
     * and stored as a service-area hint.
     */
    private fun serviceSchema(
        id: String,
        displayLabel: String,
        amenityOptions: List<String>,
        includeLocation: Boolean = true,
    ) = SubcategorySchema(
        id = id,
        category = ListingCategory.SERVICE,
        displayLabel = displayLabel,
        fields = FieldSets.Core +
            FieldSets.Service +
            (if (includeLocation) FieldSets.PhysicalPlace else emptySet()) +
            (if (amenityOptions.isNotEmpty()) setOf(ListingField.AMENITIES) else emptySet()) +
            FieldSets.HourlySchedule,
        allowedPricingUnits = listOf(PricingUnit.HOUR),
        amenityOptions = amenityOptions,
        needsSchedule = true,
    )

    private val HomeHelp = serviceSchema(
        "home_help", "Home help",
        listOf("Materials Included", "Indoor", "Outdoor", "Beginner Friendly", "Equipment Provided", "Flexible Schedule"),
    )
    private val MovingHelp = serviceSchema(
        "moving_help", "Moving help",
        listOf("Materials Included", "Equipment Provided", "Flexible Schedule", "Indoor", "Outdoor"),
    )
    private val CleaningOrganizing = serviceSchema(
        "cleaning_organizing", "Cleaning & organizing",
        listOf("Materials Included", "Equipment Provided", "Indoor", "Flexible Schedule"),
    )
    private val EverydayHelp = serviceSchema(
        "everyday_help", "Everyday help",
        listOf("Materials Included", "Flexible Schedule", "Indoor", "Outdoor", "Beginner Friendly"),
    )
    private val Fitness = serviceSchema(
        "fitness", "Fitness",
        listOf("Equipment Provided", "Indoor", "Outdoor", "Beginner Friendly", "Group Session", "Flexible Schedule"),
    )
    private val Learning = serviceSchema(
        "learning", "Learning",
        listOf("Materials Included", "Indoor", "Beginner Friendly", "Group Session", "Flexible Schedule"),
        includeLocation = false,
    )
    private val Creative = serviceSchema(
        "creative", "Creative",
        listOf("Materials Included", "Equipment Provided", "Indoor", "Beginner Friendly", "Group Session", "Flexible Schedule"),
    )
    private val ServiceOthers = serviceSchema(
        "others_service", "Other service",
        emptyList(),
    )

    // ── Registry lookup ────────────────────────────────────────

    /**
     * Canonical id → schema. Keyed by the subcategory string the backend
     * persists, with legacy aliases folded into [aliasMap].
     */
    private val byId: Map<String, SubcategorySchema> = listOf(
        // Spaces
        Parking, EvParking, Restroom, NapPod, MeetingRoom, Gym,
        ShortStay, Apartment, Wifi, StorageSpace, SpaceOthers,
        // Items
        SportsGear, Camera, Tech, Instrument, Tools, Games, Toys,
        Micromobility, ItemOthers,
        // Services
        HomeHelp, MovingHelp, CleaningOrganizing, EverydayHelp, Fitness,
        Learning, Creative, ServiceOthers,
    ).associateBy { it.id }

    /**
     * Backward-compatible aliases.  The UI ships a single "others" button
     * per resource-kind picker, but the schema tracks them separately so
     * that field sets do not collide.
     */
    private val aliasMap: Map<String, String> = mapOf(
        // Resource-kind-aware disambiguation — the picker emits "others",
        // but the wizard already knows the resource kind, so we can pick
        // the right schema at call-sites with [schemaFor(kind, subId)].
        "sportsgear" to "sports_gear",
    )

    /**
     * Resolve a schema by subcategory id.  Returns a safe default only
     * when nothing maps — callers should prefer the resource-kind-aware
     * overload to keep "others" disambiguated.
     */
    fun schemaFor(subCategoryId: String): SubcategorySchema {
        val key = subCategoryId.lowercase()
        val resolved = aliasMap[key] ?: key
        return byId[resolved] ?: SpaceOthers
    }

    /**
     * Resource-kind-aware resolution.  Use this from the wizard so the
     * shared "others" id routes to the correct category schema instead
     * of always falling back to [SpaceOthers].
     */
    fun schemaFor(category: ListingCategory, subCategoryId: String): SubcategorySchema {
        val key = subCategoryId.lowercase()
        val resolved = aliasMap[key] ?: key
        val direct = byId[resolved]
        if (direct != null && direct.category == category) return direct
        return when (category) {
            ListingCategory.SPACE -> SpaceOthers
            ListingCategory.ITEM -> ItemOthers
            ListingCategory.SERVICE -> ServiceOthers
        }
    }
}
