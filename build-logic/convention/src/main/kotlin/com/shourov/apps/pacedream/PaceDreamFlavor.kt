package com.shourov.apps.pacedream

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import com.shourov.apps.pacedream.FlavorDimension.contentType

@Suppress("EnumEntryName")
enum class FlavorDimension {
    contentType
}

// The app talks to the production backend by default (`prod`, the flavor
// that ships). The `staging` flavor points the networking BuildConfig
// fields at the staging backend via the STAGING_* keys in
// secrets(.defaults).properties — see core/network/build.gradle.kts and
// app/build.gradle.kts — so QA can exercise pre-production APIs without
// code changes.
@Suppress("EnumEntryName")
enum class PaceDreamFlavor(
    val dimension: FlavorDimension,
    val applicationIdSuffix: String? = null,
) {
    // No applicationIdSuffix on staging: google-services.json only registers
    // the base package (and the .debug buildType suffix), and the
    // google-services plugin fails the build for unregistered package names.
    staging(contentType),
    prod(contentType)
}

fun configureFlavors(
    commonExtension: CommonExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: PaceDreamFlavor) -> Unit = {},
) {
    commonExtension.flavorDimensions += contentType.name
    commonExtension.productFlavors.apply {
        PaceDreamFlavor.values().forEach {
            create(it.name).apply {
                dimension = it.dimension.name
                flavorConfigurationBlock(this, it)
                if (commonExtension is ApplicationExtension && this is ApplicationProductFlavor) {
                    if (it.applicationIdSuffix != null) {
                        applicationIdSuffix = it.applicationIdSuffix
                    }
                }
            }
        }
    }
}
