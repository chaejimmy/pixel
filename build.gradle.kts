buildscript {
    repositories {
        google()
        mavenCentral()
        // Android Build Server
        maven { url = uri("../pacedream-prebuilts/m2repository") }
    }
    dependencies {
        classpath(libs.google.oss.licenses.plugin) {
            exclude(group = "com.google.protobuf")
        }
    }

}

// Lists all plugins used throughout the project
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.dependencyGuard) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.secrets) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.module.graph) apply true
    alias(libs.plugins.jetbrains.kotlin.android) apply false // Plugin applied to allow module graph generation
}

// ──────────────────────────────────────────────────────────────────────────────
// designSystemCheck
//
// Repo-wide lint gate that fails CI if a Compose `feature/**` or
// `app/feature/**` source file re-introduces a hardcoded design-system
// literal. Replaces the audit's requested Detekt rule with a self-contained
// Gradle task so we don't have to bootstrap an entirely new plugin to enforce
// five regex patterns.
//
// Banned patterns:
//   * `Color(0x...)`             — use `PaceDreamColors.*` (or `CategoryColors.*`)
//   * `Color.White` / `Color.Black` — use `MaterialTheme.colorScheme.*`,
//                                     `OnBrandSurface`, or `scrimOnImage(...)`
//   * `fontSize = N.sp`          — use a `PaceDreamTypography.*` token directly
//   * `RoundedCornerShape(N.dp)` — wrap a `PaceDreamRadius.*` token instead
//
// Run locally:  ./gradlew designSystemCheck
// CI wires this into the regular `check` lifecycle below so a regression
// fails the same green-bar that runs unit tests.
// ──────────────────────────────────────────────────────────────────────────────
tasks.register("designSystemCheck") {
    group = "verification"
    description = "Fails the build if feature/** re-introduces banned design-system literals."

    // Scoped allowlist of source roots that have completed the design-system
    // migration. Phase 3 migrated `app/.../pacedream/app/feature/home`; future
    // phases should add their feature dir here once they've cleaned up the
    // existing literals. The pre-existing violations in other features
    // (~257 `Color.White`, ~37 `Color.Black`) are intentionally left out of
    // scope — flipping them on without migration would block every PR.
    val scanRoots = listOf(
        rootProject.file("app/src/main/kotlin/com/pacedream/app/feature/home"),
        // Zero-violation modules per DESIGN_SYSTEM_COVERAGE.md — locked in
        // as CI insurance so a future PR cannot regress them.
        rootProject.file("feature/search"),
        rootProject.file("feature/notifications"),
        rootProject.file("feature/notification"),
        rootProject.file("app/src/main/kotlin/com/shourov/apps/pacedream/feature/payment"),
        rootProject.file("app/src/main/kotlin/com/shourov/apps/pacedream/feature/destinations"),
        rootProject.file("app/src/main/kotlin/com/shourov/apps/pacedream/feature/bookingdetail"),
        rootProject.file("app/src/main/kotlin/com/pacedream/app/feature/listing"),
        rootProject.file("app/src/main/kotlin/com/pacedream/app/feature/hostprofile"),
        rootProject.file("app/src/main/kotlin/com/pacedream/app/feature/faq"),
        // TODO: add `app/src/main/kotlin/com/pacedream/app/feature/<next>` after
        // each feature's design-system migration lands.
    ).filter { it.exists() }

    // STRICT corner-radius enforcement.  Phase B cleared the
    // RoundedCornerShape(N.dp) column to 0 across every feature/** module
    // (feature/wanted, feature/chat, feature/wishlist were the last three).
    // To prevent regression, the radius rule alone scans every feature
    // source root — not just the migration allowlist above — so a stray
    // `RoundedCornerShape(N.dp)` anywhere in feature code fails CI.  The
    // other four rules stay scoped to scanRoots because their columns are
    // not yet zero across all modules.
    val strictRadiusRoots = listOf(
        rootProject.file("feature"),
        rootProject.file("app/src/main/kotlin/com/pacedream/app/feature"),
        rootProject.file("app/src/main/kotlin/com/shourov/apps/pacedream/feature"),
    ).filter { it.exists() }

    // Each rule pairs a regex with the suggested replacement so the failure
    // message is actionable. Regexes are anchored to avoid matching tokens —
    // e.g. `RoundedCornerShape(PaceDreamRadius.LG)` is allowed; only a `.dp`
    // literal inside the constructor is flagged.
    val rules = listOf(
        BannedLiteral(
            id = "Color(0x...) hex literal",
            pattern = Regex("""Color\(0x"""),
            fix = "use PaceDreamColors.* or CategoryColors.* — never a raw hex"
        ),
        BannedLiteral(
            id = "Color.White",
            pattern = Regex("""\bColor\.White\b"""),
            fix = "use MaterialTheme.colorScheme.* or OnBrandSurface (for image overlays)"
        ),
        BannedLiteral(
            id = "Color.Black",
            pattern = Regex("""\bColor\.Black\b"""),
            fix = "use MaterialTheme.colorScheme.* or scrimOnImage(alpha)"
        ),
        BannedLiteral(
            id = "fontSize = N.sp inline literal",
            pattern = Regex("""fontSize\s*=\s*\d+\.sp"""),
            fix = "use a PaceDreamTypography.* style instead of overriding fontSize"
        ),
        BannedLiteral(
            id = "RoundedCornerShape(N.dp)",
            pattern = Regex("""RoundedCornerShape\(\s*\d+\.dp"""),
            fix = "use RoundedCornerShape(PaceDreamRadius.*)"
        ),
    )

    val radiusRule = rules.single { it.id == "RoundedCornerShape(N.dp)" }

    inputs.files(
        (scanRoots + strictRadiusRoots)
            .map { project.fileTree(it).matching { include("**/*.kt") } }
    )
    outputs.upToDateWhen { true } // Task is pure / idempotent.

    doLast {
        val violations = mutableListOf<String>()

        // Allowlist pass — all 5 rules over the migrated source roots.
        scanRoots.forEach { root ->
            project.fileTree(root).matching { include("**/*.kt") }.forEach { file ->
                // Skip auto-generated previews if any happen to live in feature
                // dirs; the @Preview itself is unlikely to introduce these
                // literals because previews call into the same Composables.
                file.readLines().forEachIndexed { idx, line ->
                    rules.forEach { rule ->
                        if (rule.pattern.containsMatchIn(line)) {
                            val rel = file.relativeTo(rootDir).path
                            violations += "$rel:${idx + 1}  [${rule.id}]  →  ${rule.fix}\n    $line"
                        }
                    }
                }
            }
        }

        // Strict pass — radius rule only, across every feature/** source tree.
        // Skip files already covered by scanRoots so duplicate hits are not
        // reported twice when a module is both on the allowlist and inside a
        // strict-root directory.
        val coveredFiles = scanRoots.flatMap { root ->
            project.fileTree(root).matching { include("**/*.kt") }.files
        }.toSet()
        strictRadiusRoots.forEach { root ->
            project.fileTree(root).matching { include("**/*.kt") }.forEach { file ->
                if (file in coveredFiles) return@forEach
                file.readLines().forEachIndexed { idx, line ->
                    if (radiusRule.pattern.containsMatchIn(line)) {
                        val rel = file.relativeTo(rootDir).path
                        violations += "$rel:${idx + 1}  [${radiusRule.id} — STRICT]  →  ${radiusRule.fix}\n    $line"
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            val msg = buildString {
                appendLine("designSystemCheck found ${violations.size} banned literal(s) in feature code:")
                appendLine()
                violations.forEach { appendLine(it) }
                appendLine()
                appendLine("Fix the violations above, or add the file to an explicit allowlist if the")
                appendLine("literal is unavoidable (e.g. a design-system module itself).")
            }
            throw GradleException(msg)
        }
        logger.lifecycle(
            "designSystemCheck: ✓ scanned ${scanRoots.size} allowlisted root(s) " +
                "+ ${strictRadiusRoots.size} strict-radius root(s); zero violations."
        )
    }
}

// Wire the check into per-module `check` so CI surfaces regressions on
// the standard verification gate. Subprojects pick this up after their own
// plugins create the `check` task.
subprojects {
    afterEvaluate {
        tasks.findByName("check")?.dependsOn(rootProject.tasks.named("designSystemCheck"))
    }
}

/**
 * One rule consumed by [designSystemCheck]. Carries the regex used to
 * flag the literal and a human-readable replacement suggestion that gets
 * printed in the failure message so engineers can fix the violation
 * without re-reading this file.
 */
data class BannedLiteral(val id: String, val pattern: Regex, val fix: String)
