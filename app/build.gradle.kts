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

android {
    defaultConfig {
        applicationId = "com.shourov.apps.pacedream"
        versionCode = 13
        versionName = "0.1.3" // X.Y.Z; X = Major, Y = minor, Z = Patch level

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

    signingConfigs {
        // Release signing – loaded from secrets.properties or CI environment.
        // When the keystore properties are absent the config is still created but
        // with safe defaults so that the build file parses without error; the
        // release buildType falls back to the debug signing config in that case.
        create("release") {
            val ksFile = secretsProps.getProperty("RELEASE_KEYSTORE_FILE", "")
            if (ksFile.isNotBlank()) {
                storeFile = rootProject.file(ksFile)
                storePassword = secretsProps.getProperty("RELEASE_KEYSTORE_PASSWORD", "")
                keyAlias = secretsProps.getProperty("RELEASE_KEY_ALIAS", "")
                keyPassword = secretsProps.getProperty("RELEASE_KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = PaceDreamBuildType.DEBUG.applicationIdSuffix
        }
        release {
            isMinifyEnabled = true
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
    implementation(projects.feature.guest)
    implementation(projects.feature.payment)
    implementation(projects.feature.createAccount)
    implementation(projects.feature.host)
    implementation(projects.feature.wishlist)
    implementation(projects.feature.inbox)
    implementation(projects.feature.webflow)
    implementation(project(":feature:auth"))

    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(projects.core.model)
    implementation(projects.core.analytics)
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
