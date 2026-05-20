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
        // Legacy multi-module home — migrated in refactor/ds-migration-home.
        // The redesign/ subtree is carved out via `scanExcludes` below
        // pending the HomeRedesignTheme.kt palette decision (30 hex literals
        // form a self-contained internal palette that should not be expanded
        // into design-system tokens per "no new brand colors" rule).
        rootProject.file("feature/home"),
        // Legacy host module — migrated in refactor/ds-migration-host.
        // No subtree carve-outs needed here (host has no redesign/).
        rootProject.file("app/src/main/kotlin/com/shourov/apps/pacedream/feature/host"),
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
        // Migrated in refactor/ds-migration-listingdetail — 43 hits cleared
        // (30 hex literals on image overlays + status pills + ratings,
        // plus 13 Color.White/Black for hero overlays and CTA buttons).
        rootProject.file("app/src/main/kotlin/com/pacedream/app/feature/listingdetail"),
        // TODO: add `app/src/main/kotlin/com/pacedream/app/feature/<next>` after
        // each feature's design-system migration lands.
    ).filter { it.exists() }

    // Subtrees inside scanRoots that haven't been migrated yet.  The
    // allowlist pass skips files matching any of these patterns; the
    // strict radius pass still covers them so radius regressions are
    // caught everywhere.  Currently the only carve-out is the redesign/
    // subtree of feature/home — see the comment on scanRoots above.
    val scanExcludes = listOf("**/redesign/**")

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
        scanRoots.map { project.fileTree(it).matching { include("**/*.kt"); exclude(scanExcludes) } } +
            strictRadiusRoots.map { project.fileTree(it).matching { include("**/*.kt") } }
    )
    outputs.upToDateWhen { true } // Task is pure / idempotent.

    doLast {
        val violations = mutableListOf<String>()

        // Allowlist pass — all 5 rules over the migrated source roots,
        // skipping any subtrees explicitly carved out via scanExcludes.
        scanRoots.forEach { root ->
            project.fileTree(root).matching {
                include("**/*.kt")
                exclude(scanExcludes)
            }.forEach { file ->
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
        // strict-root directory.  scanExcludes deliberately do NOT apply
        // here — RoundedCornerShape(N.dp) regressions are blocked repo-wide
        // including inside any carved-out subtree.
        val coveredFiles = scanRoots.flatMap { root ->
            project.fileTree(root).matching {
                include("**/*.kt")
                exclude(scanExcludes)
            }.files
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

// ──────────────────────────────────────────────────────────────────────────────
// noGreenRebrandHex
//
// Belt-and-suspenders guard against re-introducing the apocryphal green
// primary hex that briefly appeared in DESIGN_SYSTEM_COVERAGE.md before
// being corrected. The canonical brand primary is purple #5527D7 (see
// common/.../theme/Color.kt:108 and DESIGN_SYSTEM_README.md). Any
// occurrence of the forbidden green (either the `#` form or the
// `0xFF...` Compose form) outside docs/audits/ — where historical audit
// copies are preserved verbatim — fails the build so doc drift can't
// reintroduce the wrong claim. The regex source on its own line below is
// the only allowed literal in this file: the leading `})` keeps it from
// self-matching, so the guard catches everything else.
//
// Run locally:  ./gradlew noGreenRebrandHex
// ──────────────────────────────────────────────────────────────────────────────
tasks.register("noGreenRebrandHex") {
    group = "verification"
    description = "Fails the build if the apocryphal green hex appears outside docs/audits/."

    val scanTree = project.fileTree(rootDir).apply {
        include(
            "**/*.md",
            "**/*.kt",
            "**/*.kts",
            "**/*.xml",
            "**/*.json",
            "**/*.yml",
            "**/*.yaml",
        )
        exclude(
            "**/build/**",
            "**/.gradle/**",
            "**/.git/**",
            "**/node_modules/**",
            "docs/audits/**",
        )
    }

    inputs.files(scanTree)
    outputs.upToDateWhen { true }

    doLast {
        val pattern = Regex("""(?i)(?:#|0x[fF]{2})336633""")
        val violations = mutableListOf<String>()
        scanTree.forEach { file ->
            file.readLines().forEachIndexed { idx, line ->
                if (pattern.containsMatchIn(line)) {
                    violations += "${file.relativeTo(rootDir).path}:${idx + 1}\n    $line"
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine(
                        "noGreenRebrandHex: forbidden green hex found in ${violations.size} location(s).",
                    )
                    appendLine(
                        "PaceDream Primary is purple #5527D7 (see Color.kt:108 + DESIGN_SYSTEM_README.md).",
                    )
                    appendLine("If this is a historical audit document, move it under docs/audits/.")
                    appendLine()
                    violations.forEach { appendLine(it) }
                },
            )
        }
        logger.lifecycle("noGreenRebrandHex: ✓ no forbidden green-hex occurrences outside docs/audits/.")
    }
}

// Wire the check into per-module `check` so CI surfaces regressions on
// the standard verification gate. Subprojects pick this up after their own
// plugins create the `check` task.
subprojects {
    afterEvaluate {
        tasks.findByName("check")?.dependsOn(rootProject.tasks.named("designSystemCheck"))
        tasks.findByName("check")?.dependsOn(rootProject.tasks.named("noGreenRebrandHex"))
    }
}

/**
 * One rule consumed by [designSystemCheck]. Carries the regex used to
 * flag the literal and a human-readable replacement suggestion that gets
 * printed in the failure message so engineers can fix the violation
 * without re-reading this file.
 */
data class BannedLiteral(val id: String, val pattern: Regex, val fix: String)
