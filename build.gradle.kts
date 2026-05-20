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
// `app/src/main/kotlin/.../feature/**` source file re-introduces a hardcoded
// design-system literal. Implemented as a self-contained Gradle task so we
// don't have to bootstrap a Detekt plugin to enforce four regex patterns.
//
// Banned patterns (inside feature/** code):
//   * `Color(0x...)`             — use `PaceDreamColors.*` (or `CategoryColors.*`)
//   * `Color.White` / `Color.Black` — use `MaterialTheme.colorScheme.*`,
//                                     `OnBrandSurface`, or `scrimOnImage(...)`
//                                     (override per-line with `// allow-token`)
//   * `fontSize = N.sp`          — use a `PaceDreamTypography.*` token directly
//   * `RoundedCornerShape(N.dp)` — wrap a `PaceDreamRadius.*` token instead
//
// Escape hatches:
//   * Files under `core/designsystem/**` and `common/.../theme/**` are the
//     token sources themselves and never scanned (they live outside the
//     `feature/**` roots below).
//   * A file whose first 30 lines contain
//       `// @DesignSystemEscape (reason="...")`
//     is skipped entirely.  The 30-line window accommodates Apache 2.0
//     copyright headers; place the marker on the line immediately above
//     the `package` declaration.  The reason text is required and surfaced
//     in the task log so reviewers see why a file was opted out.
//   * A single line ending with `// allow-token` suppresses the `Color.White`
//     and `Color.Black` rules on that line — for cases where the literal is
//     semantically correct (system UI overlays, image scrims with explicit
//     alpha, etc.).
//
// Run locally:  ./gradlew designSystemCheck
// CI wires this into per-module `check` below so a regression fails the
// same green-bar that runs unit tests.
// ──────────────────────────────────────────────────────────────────────────────
tasks.register("designSystemCheck") {
    group = "verification"
    description = "Fails the build if feature/** re-introduces banned design-system literals."

    // Every `feature/**` source root in the repo.  Unlike the previous
    // narrow allowlist, the rule now scans **all** feature code by default
    // — legacy violations are opted out via the per-file `@DesignSystemEscape`
    // marker instead of a path allowlist, so new files can never silently
    // regress just because their module isn't on a list.
    val scanRoots = listOf(
        rootProject.file("feature"),
        rootProject.file("app/src/main/kotlin/com/pacedream/app/feature"),
        rootProject.file("app/src/main/kotlin/com/shourov/apps/pacedream/feature"),
    ).filter { it.exists() }

    val rules = listOf(
        BannedLiteral(
            id = "Color(0x...) hex literal",
            // Negative lookbehind keeps `setToolbarColor(0x...)` and other
            // identifiers ending in `Color` from triggering — only a bare
            // `Color(0x...)` constructor call counts.
            pattern = Regex("""(?<![A-Za-z_])Color\(0x[0-9A-Fa-f]+"""),
            fix = "use PaceDreamColors.* or CategoryColors.* — never a raw hex",
            lineEscapable = false,
        ),
        BannedLiteral(
            id = "Color.White",
            pattern = Regex("""\bColor\.White\b"""),
            fix = "Color.White → MaterialTheme.colorScheme.surface, OnBrandSurface, or add `// allow-token` if intentional",
            lineEscapable = true,
        ),
        BannedLiteral(
            id = "Color.Black",
            pattern = Regex("""\bColor\.Black\b"""),
            fix = "Color.Black → MaterialTheme.colorScheme.onSurface, scrimOnImage(alpha), or add `// allow-token` if intentional",
            lineEscapable = true,
        ),
        BannedLiteral(
            id = "fontSize = N.sp inline literal",
            pattern = Regex("""fontSize\s*=\s*\d+\.sp"""),
            fix = "use a PaceDreamTypography.* style instead of overriding fontSize",
            lineEscapable = false,
        ),
        BannedLiteral(
            id = "RoundedCornerShape(N.dp)",
            pattern = Regex("""RoundedCornerShape\(\s*\d+\.dp"""),
            fix = "use RoundedCornerShape(PaceDreamRadius.*)",
            lineEscapable = false,
        ),
    )

    inputs.files(
        scanRoots.map { project.fileTree(it).matching { include("**/*.kt") } }
    )
    outputs.upToDateWhen { true } // Task is pure / idempotent.

    doLast {
        val violations = mutableListOf<String>()
        var filesScanned = 0
        val escapedFiles = mutableListOf<Pair<String, String>>() // (path, reason)

        // Header marker matched against the first `escapeHeaderScanLines`
        // lines of each file.  The reason is required so reviewers know
        // why the file was opted out and what would need to change to
        // remove the escape.
        val escapeHeaderPattern =
            Regex("""//\s*@DesignSystemEscape\s*\(\s*reason\s*=\s*"([^"]+)"\s*\)""")
        // Generous enough for an Apache 2.0 license header above the
        // package declaration, where the escape marker is conventionally
        // placed.
        val escapeHeaderScanLines = 30
        // Inline opt-out token suffixing the same line as the literal.
        val inlineAllowToken = "// allow-token"

        scanRoots.forEach { root ->
            project.fileTree(root).matching { include("**/*.kt") }.forEach { file ->
                val lines = file.readLines()
                val header = lines.take(escapeHeaderScanLines).joinToString("\n")
                val escapeMatch = escapeHeaderPattern.find(header)
                if (escapeMatch != null) {
                    escapedFiles += file.relativeTo(rootDir).path to escapeMatch.groupValues[1]
                    return@forEach
                }
                filesScanned++
                lines.forEachIndexed { idx, line ->
                    rules.forEach { rule ->
                        if (!rule.pattern.containsMatchIn(line)) return@forEach
                        if (rule.lineEscapable && line.contains(inlineAllowToken)) return@forEach
                        val rel = file.relativeTo(rootDir).path
                        violations += "$rel:${idx + 1}  [${rule.id}]  →  ${rule.fix}\n    ${line.trim()}"
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
                appendLine("Fix the violations above using the suggested replacement, or — if the literal")
                appendLine("is genuinely unavoidable — add one of the escape hatches documented at the top")
                appendLine("of build.gradle.kts (`// @DesignSystemEscape (reason=\"...\")` at file top, or")
                appendLine("`// allow-token` on the same line for `Color.White` / `Color.Black`).")
            }
            throw GradleException(msg)
        }
        logger.lifecycle(
            "designSystemCheck: ✓ scanned $filesScanned file(s) across ${scanRoots.size} feature " +
                "root(s); ${escapedFiles.size} file(s) opted out via @DesignSystemEscape; zero violations."
        )
        if (escapedFiles.isNotEmpty() && logger.isInfoEnabled) {
            escapedFiles.forEach { (path, reason) ->
                logger.info("  escape: $path  ($reason)")
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// darkModeSnapshotCheck
//
// CI aggregator for dark-mode Compose `@Preview` snapshot tests.  The repo
// uses Roborazzi (already wired through `gradle/libs.versions.toml` and
// applied to `:app` and `:core:designsystem`); this task collects the
// `verifyRoborazziDebug` outputs across every subproject that opts in.
//
// Conventions (see DESIGN_SYSTEM_README.md):
//   * Every screen-level composable — a `*Screen.kt` top-level
//     `@Composable fun XScreen(...)` invoked from the nav graph — should
//     have a paired test file under `src/test/java/.../<Feature>ScreenDarkModeSnapshotTest.kt`.
//   * Each test uses `androidx.compose.ui.test.junit4.createComposeRule`
//     wrapped in `PaceDreamTheme(darkTheme = true)` and `captureRoboImage()`
//     to record the dark-mode reference image.
//   * Reference images are checked in under `src/test/snapshots/`; the
//     `verifyRoborazziDebug` task fails CI when a pixel diff exceeds the
//     Roborazzi default threshold.
//
// Run locally:  ./gradlew darkModeSnapshotCheck
// CI wires this into `check` so dark-mode regressions block a PR on the
// same green-bar as designSystemCheck.
// ──────────────────────────────────────────────────────────────────────────────
val darkModeSnapshotCheck = tasks.register("darkModeSnapshotCheck") {
    group = "verification"
    description =
        "Verifies dark-mode @Preview snapshots across every subproject with Roborazzi configured."

    doLast {
        // `dependsOn` does the actual work; this `doLast` only logs whether
        // any Roborazzi-enabled subproject contributed a verify task so a
        // silent "no tests configured" doesn't go unnoticed in CI logs.
        val ran = taskDependencies.getDependencies(this).count { it.name == "verifyRoborazziDebug" }
        if (ran == 0) {
            logger.warn(
                "darkModeSnapshotCheck: no subproject exposes a verifyRoborazziDebug task. " +
                    "Add a dark-mode @Preview snapshot test under src/test/java/... in each " +
                    "screen-level composable's module (see DESIGN_SYSTEM_README.md)."
            )
        } else {
            logger.lifecycle("darkModeSnapshotCheck: ✓ ran $ran Roborazzi verify task(s).")
        }
    }
}

// Wire both verification gates into per-subproject `check` once each
// subproject has finished configuring its plugins. `designSystemCheck` is a
// pure root-level task; `darkModeSnapshotCheck` additionally collects each
// subproject's `verifyRoborazziDebug` task (created lazily by the Roborazzi
// plugin after Android variants are configured) so screen-level snapshot
// regressions fail the same green-bar as the banned-literal scan.
subprojects {
    afterEvaluate {
        tasks.findByName("verifyRoborazziDebug")?.let { verifyTask ->
            darkModeSnapshotCheck.configure { dependsOn(verifyTask) }
        }
        tasks.findByName("check")?.dependsOn(
            rootProject.tasks.named("designSystemCheck"),
            darkModeSnapshotCheck,
        )
    }
}

/**
 * One rule consumed by [designSystemCheck]. Carries the regex used to
 * flag the literal, a human-readable replacement suggestion that gets
 * printed in the failure message so engineers can fix the violation
 * without re-reading this file, and a flag controlling whether a
 * line-level `// allow-token` suppresses the rule on the matching line.
 */
data class BannedLiteral(
    val id: String,
    val pattern: Regex,
    val fix: String,
    val lineEscapable: Boolean,
)
