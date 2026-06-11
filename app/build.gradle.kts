import com.shourov.apps.pacedream.PaceDreamBuildType
import java.util.Properties

plugins {
    alias(libs.plugins.pacedream.android.application)
    alias(libs.plugins.pacedream.android.application.compose)
    alias(libs.plugins.pacedream.android.application.flavors)
    alias(libs.plugins.pacedream.android.application.jacoco)
    alias(libs.plugins.pacedream.android.hilt)
    alias(libs.plugins.pacedream.android.application.firebase)
    id("com.google.android.gms.oss-licenses-plugin")
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.kotlin.serialization)
}

// ── Secrets loading ──────────────────────────────────────────────────
// Priority: secrets.properties (gitignored, local/CI) > secrets.defaults.properties (checked in, placeholders)
val secretsProps = Properties()
listOf("secrets.defaults.properties", "secrets.properties").forEach { name ->
    rootProject.file(name).takeIf { it.exists() }?.inputStream()?.use { secretsProps.load(it) }
}

// ── Keystore loading ─────────────────────────────────────────────────
// Dedicated keystore.properties (gitignored) is the canonical source for the
// four signing values; secrets.properties is honored as a fallback so older
// CI configs keep working. The actual property keys are identical in both
// files (RELEASE_KEYSTORE_FILE / _PASSWORD / RELEASE_KEY_ALIAS / _PASSWORD).
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }
        ?.inputStream()?.use { load(it) }
}
fun signingValue(key: String): String =
    keystoreProps.getProperty(key, "").ifBlank { secretsProps.getProperty(key, "") }

// ── Release-build secrets guard ─────────────────────────────────────
// Refuses to ship a release APK/AAB built with development placeholders.
// We hook into the task graph (not configuration) so debug/CI inspection
// runs are unaffected — this only triggers when a release-shaped task is
// actually being executed.  Failures point operators directly at the
// missing secret so CI can surface the issue.
fun resolvedSecret(propertyName: String, secretKey: String): String =
    (project.findProperty(propertyName) as? String)
        ?: secretsProps.getProperty(secretKey, "")

gradle.taskGraph.whenReady {
    val isReleaseBuild = allTasks.any { task ->
        val name = task.name
        // Match assembleRelease / bundleRelease / assembleProdRelease /
        // bundleProdRelease etc.  Avoid matching debug variants.  Staging
        // release builds are exempt: they are QA artifacts that point at the
        // staging backend and legitimately use non-production credentials.
        (name.startsWith("assemble") || name.startsWith("bundle")) &&
            name.endsWith("Release", ignoreCase = false) &&
            !name.contains("Staging") &&
            !name.contains("AndroidTest", ignoreCase = true)
    }
    if (!isReleaseBuild) return@whenReady

    val stripeKey = resolvedSecret("stripePublishableKey", "STRIPE_PUBLISHABLE_KEY")
    val auth0ClientId = resolvedSecret("auth0ClientId", "AUTH0_CLIENT_ID")
    val auth0Domain = secretsProps.getProperty("AUTH0_DOMAIN", "")
    val pdEnvironment = secretsProps.getProperty("PD_ENVIRONMENT", "")

    val problems = mutableListOf<String>()
    when {
        stripeKey.isBlank() ->
            problems += "STRIPE_PUBLISHABLE_KEY is blank (set -PstripePublishableKey=pk_live_… or secrets.properties)"
        stripeKey.startsWith("pk_test_") ->
            problems += "STRIPE_PUBLISHABLE_KEY starts with pk_test_ (test keys are not allowed in release builds)"
    }
    when {
        auth0Domain.isBlank() ->
            problems += "AUTH0_DOMAIN is blank (set AUTH0_DOMAIN in secrets.properties)"
        auth0Domain.startsWith("dev-") ->
            problems += "AUTH0_DOMAIN starts with dev- ('$auth0Domain' is a development tenant, not allowed in release builds)"
    }
    if (auth0ClientId.isBlank()) {
        problems += "AUTH0_CLIENT_ID is blank (set -Pauth0ClientId=… or secrets.properties)"
    }
    if (pdEnvironment.equals("development", ignoreCase = true)) {
        problems += "PD_ENVIRONMENT=development in secrets — set PD_ENVIRONMENT=production for release builds"
    }

    if (problems.isNotEmpty()) {
        throw GradleException(
            buildString {
                appendLine("Refusing to build a release variant with placeholder/development secrets:")
                problems.forEach { appendLine("  • $it") }
                appendLine()
                appendLine("Set production values via secrets.properties or -P… gradle properties before running a release build.")
            }
        )
    }
}

android {
    defaultConfig {
        applicationId = "com.shourov.apps.pacedream"
        versionCode = 15
        versionName = "0.1.5" // X.Y.Z; X = Major, Y = minor, Z = Patch level

        vectorDrawables {
            useSupportLibrary = true
        }

        // Auth0 manifest placeholders (consumed by Auth0 SDK RedirectActivity)
        manifestPlaceholders["auth0Domain"] = secretsProps.getProperty("AUTH0_DOMAIN", "pacedream.us.auth0.com")
        manifestPlaceholders["auth0Scheme"] = "pacedream"

        // BuildConfig fields – sourced from secrets.properties or gradle properties (CI)
        // Synced with iOS xcconfig pattern: defaults in secrets.defaults.properties,
        // overrides in secrets.properties or -P flags.
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY",
            "\"${(project.findProperty("stripePublishableKey") as? String)
                ?: secretsProps.getProperty("STRIPE_PUBLISHABLE_KEY", "")}\"")
        buildConfigField("String", "AUTH0_CLIENT_ID",
            "\"${(project.findProperty("auth0ClientId") as? String)
                ?: secretsProps.getProperty("AUTH0_CLIENT_ID", "")}\"")
        buildConfigField("String", "ONESIGNAL_APP_ID",
            "\"${(project.findProperty("onesignalAppId") as? String)
                ?: secretsProps.getProperty("ONESIGNAL_APP_ID", "")}\"")

        // Environment tag (iOS parity: PD_ENVIRONMENT in xcconfig)
        buildConfigField("String", "PD_ENVIRONMENT",
            "\"${secretsProps.getProperty("PD_ENVIRONMENT", "development")}\"")

        // Frontend base URL (iOS parity: FRONTEND_BASE_URL in xcconfig)
        buildConfigField("String", "FRONTEND_BASE_URL",
            "\"${secretsProps.getProperty("FRONTEND_BASE_URL", "https://www.pacedream.com")}\"")

        // Backend base URL for the new networking stack (AppConfig reads this;
        // the legacy core/network stack has its own SERVICE_URL BuildConfig).
        buildConfigField("String", "SERVICE_URL",
            "\"${secretsProps.getProperty("SERVICE_URL", "https://pacedream-backend.onrender.com/v1/")}\"")

        // Cloudinary config (iOS parity: CLOUDINARY_CLOUD_NAME / CLOUDINARY_UPLOAD_PRESET in xcconfig)
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME",
            "\"${secretsProps.getProperty("CLOUDINARY_CLOUD_NAME", "")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET",
            "\"${secretsProps.getProperty("CLOUDINARY_UPLOAD_PRESET", "")}\"")

        // Google Maps API key – provided via the static XML resource
        // (app/src/main/res/values/google_maps.xml) which the manifest
        // meta-data picks up via @string/google_maps_key.
        // NOTE: resValue is not used because resvalues build feature is disabled.
    }

    productFlavors {
        // QA flavor: same app id as prod (google-services.json constraint —
        // see PaceDreamFlavor.kt), but the environment tag and URLs point at
        // staging. STAGING_* keys default to the production values so the
        // flavor builds even when they are not configured.
        getByName("staging") {
            buildConfigField("String", "PD_ENVIRONMENT", "\"staging\"")
            buildConfigField("String", "SERVICE_URL",
                "\"${secretsProps.getProperty("STAGING_SERVICE_URL",
                    secretsProps.getProperty("SERVICE_URL", "https://pacedream-backend.onrender.com/v1/"))}\"")
            buildConfigField("String", "FRONTEND_BASE_URL",
                "\"${secretsProps.getProperty("STAGING_FRONTEND_BASE_URL",
                    secretsProps.getProperty("FRONTEND_BASE_URL", "https://www.pacedream.com"))}\"")
        }
    }

    signingConfigs {
        // Release signing – loaded from keystore.properties first, then
        // secrets.properties as a fallback, then CI gradle properties (-P).
        // When none of those provide a keystore the config is still created
        // but with safe defaults so the build file parses without error; the
        // release buildType falls back to the debug signing config in that case.
        create("release") {
            val ksFile = signingValue("RELEASE_KEYSTORE_FILE")
            if (ksFile.isNotBlank()) {
                storeFile = rootProject.file(ksFile)
                storePassword = signingValue("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = PaceDreamBuildType.DEBUG.applicationIdSuffix
        }
        release {
            isMinifyEnabled = true
            // Resource shrinking only runs when minification is on (it is).
            // Resources referenced dynamically (e.g.
            // `Resources.getIdentifier("google_maps_key", "string", …)` in
            // core/location/PlacesAutocompleteService.kt) are explicitly
            // preserved via app/src/main/res/raw/keep.xml so the shrinker
            // does not strip them.
            isShrinkResources = true
            applicationIdSuffix = PaceDreamBuildType.RELEASE.applicationIdSuffix
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // Use release signing config when a keystore is configured; fall back to
            // debug signing so open-source contributors can still build locally.
            val releaseKs = signingConfigs.named("release").get()
            signingConfig = if (releaseKs.storeFile?.exists() == true) {
                releaseKs
            } else {
                signingConfigs.named("debug").get()
            }
            // Ensure Baseline Profile is fresh for release builds.
            baselineProfile.automaticGenerationDuringBuild = true
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.shourov.apps.pacedream"
}

dependencies {

    // feature dependencies
    implementation(projects.feature.signin)
    implementation(projects.feature.booking)
    implementation(projects.feature.notification)
    implementation(projects.feature.notifications)
    implementation(projects.feature.guest)
    implementation(projects.feature.createAccount)
    implementation(projects.feature.host)
    implementation(projects.feature.wishlist)
    implementation(projects.feature.inbox)
    implementation(projects.feature.webflow)
    implementation(projects.feature.wifi)
    implementation(projects.feature.wanted)
    implementation(project(":feature:auth"))

    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(projects.core.model)
    implementation(projects.core.analytics)
    implementation(projects.core.location)
    implementation(projects.common)
//    implementation(projects.sync.work)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.window.core)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.coil.kt)
    implementation(libs.lottie.compose)

    // Firebase Messaging for push notifications
    implementation(libs.firebase.cloud.messaging)

    // OneSignal SDK for push notification delivery (iOS parity)
    implementation(libs.onesignal)

    // Kotlinx Serialization for JSON parsing
    implementation(libs.kotlinx.serialization.json)

    // Retrofit for HostModule
    implementation(libs.retrofit.core)
    implementation(libs.gson.convert)
    implementation(libs.retrofit.gson)

    // Auth0 SDK for authentication
    implementation(libs.auth0)

    // Timber for logging
    implementation(libs.timber)

    // AndroidX Security Crypto for EncryptedSharedPreferences
    implementation(libs.androidx.security.crypto)

    // OkHttp Logging Interceptor
    implementation(libs.okhttp.logging)

    // Coil Compose for AsyncImage
    implementation(libs.coil.kt.compose)

    // Google Maps (detail map preview)
    implementation(libs.google.play.maps)
    implementation(libs.google.maps.compose)
    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // AndroidX Browser for CustomTabs
    implementation(libs.androidx.browser)

    // Stripe Android SDK for Payment Methods
    implementation("com.stripe:stripe-android:22.5.0")

    // WorkManager (background reconciliation for captured-but-unconfirmed payments)
    implementation(libs.androidx.work.ktx)
    implementation(libs.hilt.ext.work)
    ksp(libs.hilt.ext.compiler)

    implementation(libs.identity.credential)
    implementation(project(":feature:home"))
    implementation(project(":feature:home:presentation"))
    implementation(project(":core:database"))

    ksp(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.testManifest)

    kspTest(libs.hilt.compiler)

    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.hilt.android.testing)
    // JVM unit tests: JUnit + Robolectric drive the Collections/Trip delete
    // dialog composables and ViewModels without a connected device; Turbine
    // backs StateFlow assertions in CheckoutViewModel's PaymentReconciliationTest.
    // Keep these JVM-only — the app module has no androidTest source set.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)



    baselineProfile(projects.benchmarks)
}

baselineProfile {
    // Don't build on every iteration of a full assemble.
    // Instead enable generation directly for the release build variant.
    automaticGenerationDuringBuild = false
}

dependencyGuard {
    configuration("prodReleaseRuntimeClasspath")
}
