# PaceDream Android Design System

This document outlines the enhanced design system implementation for the PaceDream Android app, designed to match the iOS app's visual design and user experience.

## 🎨 Design System Overview

The PaceDream design system provides a comprehensive set of design tokens, components, and patterns that ensure consistency across the Android app while maintaining visual parity with the iOS version.

### Key Features

- **Brand Colors**: Consistent color palette matching iOS app
- **Typography**: iOS-style typography system with proper hierarchy
- **Spacing**: Systematic spacing scale for consistent layouts
- **Components**: Reusable UI components with multiple variants
- **Theming**: Support for light/dark themes and dynamic theming

## 🎯 Brand Colors

### Primary Colors
- **PaceDream Primary**: `#5527D7` - Main brand color
- **PaceDream Secondary**: `#3B82F6` - Secondary actions
- **PaceDream Accent**: `#7B4DFF` - Accent elements

### Neutral Colors
- **Background**: `#FFFFFF` - Main background
- **Surface**: `#F8F9FA` - Card and surface backgrounds
- **Text Primary**: `#1A1A1A` - Main text color
- **Text Secondary**: `#6B7280` - Secondary text
- **Text Tertiary**: `#9CA3AF` - Tertiary text

### Status Colors
- **Success**: `#10B981` - Success states
- **Warning**: `#F59E0B` - Warning states
- **Error**: `#EF4444` - Error states
- **Info**: `#3B82F6` - Information states

## 📝 Typography

The typography system follows iOS design patterns with proper hierarchy:

### Text Styles
- **Large Title**: 34sp, Bold - Main headings
- **Title 1**: 28sp, Bold - Section headings
- **Title 2**: 22sp, Bold - Subsection headings
- **Title 3**: 20sp, SemiBold - Card titles
- **Headline**: 17sp, SemiBold - Important text
- **Body**: 17sp, Normal - Regular text
- **Callout**: 16sp, Normal - Secondary text
- **Subheadline**: 15sp, Normal - Supporting text
- **Caption**: 12sp, Normal - Small text

## 📏 Spacing System

Consistent spacing scale for layouts:

- **XS**: 4dp - Minimal spacing
- **SM**: 8dp - Small spacing
- **MD**: 16dp - Medium spacing
- **LG**: 24dp - Large spacing
- **XL**: 32dp - Extra large spacing
- **XXL**: 48dp - Section spacing
- **XXXL**: 64dp - Page spacing

## 🔘 Corner Radius

Consistent corner radius for different element types:

- **XS**: 4dp - Small elements
- **SM**: 8dp - Buttons, chips
- **MD**: 12dp - Cards, inputs
- **LG**: 16dp - Large cards
- **XL**: 20dp - Modals, sheets
- **XXL**: 24dp - Large containers
- **Round**: 50dp - Pills, avatars

## 🧩 Components

### Core Components

#### 1. Hero Header
```kotlin
PaceDreamHeroHeader(
    title = "Welcome to PaceDream",
    subtitle = "Find your perfect stay",
    onNotificationClick = { /* Handle notification */ }
)
```

#### 2. Search Bar
```kotlin
PaceDreamSearchBar(
    query = searchQuery,
    onQueryChange = { newQuery -> /* Update query */ },
    onSearchClick = { /* Handle search */ },
    onFilterClick = { /* Handle filter */ },
    placeholder = "Search properties..."
)
```

#### 3. Metric Card
```kotlin
PaceDreamMetricCard(
    title = "Available Rooms",
    value = "24",
    icon = Icons.Default.Home,
    color = PaceDreamColors.Primary
)
```

#### 4. Category Pill
```kotlin
PaceDreamCategoryPill(
    title = "Rest Room",
    icon = Icons.Default.Bed,
    isSelected = true,
    onClick = { /* Handle selection */ }
)
```

#### 5. Section Header
```kotlin
PaceDreamSectionHeader(
    title = "Categories",
    onViewAllClick = { /* Handle view all */ }
)
```

### Property Components

#### 1. Property Card
```kotlin
PaceDreamPropertyCard(
    title = "Modern Apartment",
    location = "New York, NY",
    price = "$120/night",
    rating = 4.8,
    reviewCount = 124,
    onClick = { /* Handle click */ }
)
```

#### 2. Destination Card
```kotlin
PaceDreamDestinationCard(
    name = "London",
    imageUrl = "https://example.com/london.jpg",
    onClick = { /* Handle click */ }
)
```

#### 3. Recent Search Item
```kotlin
PaceDreamRecentSearchItem(
    location = "San Francisco, CA",
    onClick = { /* Handle click */ }
)
```

### Enhanced Components

#### 1. Enhanced Dashboard Header
```kotlin
EnhancedDashboardHeader(
    userName = "Darryl Rutledge",
    onSearchClick = { /* Handle search */ },
    onFilterClick = { /* Handle filter */ },
    onNotificationClick = { /* Handle notification */ }
)
```

#### 2. Enhanced Property Card
```kotlin
EnhancedPropertyCard(
    propertyName = "Luxury Villa",
    location = "Miami, FL",
    price = "$250/night",
    rating = 4.9f,
    amenities = listOf("WiFi", "Pool", "Gym"),
    isFavorite = true,
    onFavoriteClick = { /* Handle favorite */ },
    onClick = { /* Handle click */ }
)
```

#### 3. Host Card
```kotlin
HostCard(
    hostName = "Sarah Johnson",
    hostTitle = "Superhost",
    rating = 4.9f,
    responseTime = "Within an hour",
    isSuperhost = true,
    onContactClick = { /* Handle contact */ },
    onProfileClick = { /* Handle profile */ }
)
```

#### 4. Amenity Components
```kotlin
AmenityChip(
    amenity = "WiFi",
    icon = Icons.Default.Wifi,
    isSelected = true,
    onClick = { /* Handle selection */ }
)

AmenityList(
    amenities = listOf("WiFi", "Parking", "Pool", "Gym"),
    selectedAmenities = setOf("WiFi", "Pool"),
    onAmenityClick = { amenity -> /* Handle click */ }
)
```

## 🎨 Usage Examples

### Basic Dashboard Screen
```kotlin
@Composable
fun MyDashboardScreen() {
    EnhancedDashboardScreen(
        roomsState = roomsState,
        gearsState = gearsState,
        onTimeBasedRoomsChanged = { type -> /* Handle change */ },
        onRentedGearsChanged = { type -> /* Handle change */ },
        onPropertyClick = { id -> /* Navigate to property */ },
        onCategoryClick = { category -> /* Filter by category */ },
        onViewAllClick = { section -> /* Navigate to section */ }
    )
}
```

### Custom Layout with Components
```kotlin
@Composable
fun CustomPropertyList() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
            .padding(PaceDreamSpacing.LG)
    ) {
        item {
            PaceDreamSectionHeader(
                title = "Featured Properties",
                onViewAllClick = { /* Handle view all */ }
            )
        }
        
        items(properties) { property ->
            EnhancedPropertyCard(
                propertyName = property.name,
                location = property.location,
                price = property.price,
                rating = property.rating,
                onClick = { /* Handle click */ }
            )
        }
    }
}
```

## 🔧 Integration

### 1. Add Dependencies
Ensure your module includes the design system dependencies:

```kotlin
dependencies {
    implementation(project(":common"))
    implementation(project(":core:designsystem"))
}
```

### 2. Apply Theme
Wrap your app with the PaceDream theme:

```kotlin
@Composable
fun MyApp() {
    PaceDreamTheme {
        // Your app content
    }
}
```

### 3. Use Components
Import and use the components in your composables:

```kotlin
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
```

## 📱 Responsive Design

The design system includes responsive breakpoints and adaptive components:

- **Phone**: 360dp - 600dp
- **Tablet**: 600dp - 840dp
- **Desktop**: 840dp+

## 🌙 Dark Theme Support

All components automatically adapt to dark theme:

```kotlin
PaceDreamTheme(
    darkTheme = isSystemInDarkTheme(),
    content = { /* Your content */ }
)
```

## 🎯 Best Practices

1. **Consistency**: Always use design system components instead of custom implementations
2. **Accessibility**: Components include proper accessibility labels and support
3. **Performance**: Components are optimized for Compose performance
4. **Maintainability**: Centralized design tokens make updates easy
5. **Testing**: Components include proper test tags for UI testing

## 🔒 CI Guard-Rails

Two repo-wide Gradle gates keep the design system from regressing.  Both
are wired into the standard `check` lifecycle, so a regression fails the
same build that runs unit tests.

### `designSystemCheck` — banned literals in feature code

Scans every `*.kt` file under `feature/**` and
`app/src/main/kotlin/.../feature/**` for inline design-system literals:

| Pattern | Suggested replacement |
|---|---|
| `Color(0x…)` | `PaceDreamColors.*` or `CategoryColors.*` |
| `Color.White` | `MaterialTheme.colorScheme.surface` or `OnBrandSurface` |
| `Color.Black` | `MaterialTheme.colorScheme.onSurface` or `scrimOnImage(alpha)` |
| `fontSize = N.sp` | a `PaceDreamTypography.*` style — never override `fontSize` inline |
| `RoundedCornerShape(N.dp)` | `RoundedCornerShape(PaceDreamRadius.*)` |

Escape hatches (use sparingly):

* **File-level** — `// @DesignSystemEscape (reason="…")` placed above the
  `package` declaration opts the whole file out.  The `reason` is
  required and logged so reviewers see why the file is exempt.
* **Line-level** — appending `// allow-token` to a line bypasses the
  `Color.White` / `Color.Black` rules on that one line (useful for
  system-UI overlays and image scrims where the literal is the correct
  semantic colour).

Run locally: `./gradlew designSystemCheck`.  The failure message names
the file, line number, and suggested replacement so engineers don't
have to re-read the rule.

### `darkModeSnapshotCheck` — dark-mode `@Preview` snapshot tests

Aggregates Roborazzi `verifyRoborazziDebug` tasks across every subproject
that has Roborazzi configured.  Every screen-level composable
(`*Screen.kt` invoked from the nav graph) should ship with a paired
test:

```kotlin
class FooScreenDarkModeSnapshotTest {
    @get:Rule val composeRule = createComposeRule()
    @get:Rule val roborazziRule = RoborazziRule(
        composeRule = composeRule,
        captureRoot = composeRule.onRoot(),
        options = RoborazziRule.Options(captureType = RoborazziRule.CaptureType.LastImage()),
    )

    @Test fun darkMode() {
        composeRule.setContent {
            PaceDreamTheme(darkTheme = true) { FooScreen(state = previewState()) }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
```

Reference images live under `src/test/snapshots/`; a pixel-diff above
the Roborazzi default threshold fails CI.  Run locally:
`./gradlew darkModeSnapshotCheck`.

## 🚀 Future Enhancements

- [ ] Animation system integration
- [ ] Advanced theming options
- [ ] Component variants for different screen sizes
- [ ] Accessibility improvements
- [ ] Performance optimizations

## 📚 Resources

- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [iOS Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/)

---

This design system ensures that the PaceDream Android app maintains visual consistency with the iOS version while providing a native Android experience.
