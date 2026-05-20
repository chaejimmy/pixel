/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package com.shourov.apps.pacedream.feature.home.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression guard for the deal-card CTA wiring.
 *
 * Background: the three deals composables — [DealsCard], [LastMinuteDealCard],
 * and [RentedGearDealsCard] — each render an inline primary CTA ("Rent Now" /
 * "Book Now") alongside an outer card click surface. Earlier they shared the
 * outer card's `onClick`, leaving the inline button hardcoded to `onClick = {}`
 * so it looked tappable but did nothing.
 *
 * This test locks in the API contract by reading the source file directly and
 * asserting:
 *   1. No `onClick = {}` empty-lambda assignments exist outside `@Preview`
 *      blocks. (Acceptance criterion from the wiring task.)
 *   2. Each of the three composables exposes a required `onCtaClick: () -> Unit`
 *      parameter (no `= {}` default), so callers cannot silently forget to
 *      wire the inline CTA.
 *   3. The inline `ProcessButton` on each card routes its click through
 *      `onCtaClick`, not the outer `onClick`.
 *
 * A source-file scan is deliberate here: the composables currently have no
 * production callsites in this module, so a behavioural Compose test would
 * have nothing to invoke. The lint-as-test instead pins the contract so the
 * next caller cannot regress the file back to `onClick = {}`.
 */
class DealsCardContractTest {

    @Test
    fun `DealsCard source has no empty-lambda onClick outside previews`() {
        val source = readDealsCardSource()
        val stripped = stripPreviewBlocks(source)
        val violations = EMPTY_LAMBDA_ONCLICK.findAll(stripped)
            .map { it.value }
            .toList()
        assertTrue(
            "DealsCard.kt must not contain `onClick = {}` outside @Preview blocks; " +
                "found ${violations.size} match(es): $violations",
            violations.isEmpty(),
        )
    }

    @Test
    fun `each deals composable requires a non-default onCtaClick parameter`() {
        val source = readDealsCardSource()
        listOf("DealsCard", "LastMinuteDealCard", "RentedGearDealsCard").forEach { composable ->
            val signature = extractComposableSignature(source, composable)
            assertNotNull(
                "Composable $composable not found in DealsCard.kt",
                signature,
            )
            assertTrue(
                "$composable must declare `onCtaClick: () -> Unit` (required, no default). " +
                    "Signature was:\n$signature",
                signature!!.contains(Regex("""onCtaClick\s*:\s*\(\s*\)\s*->\s*Unit\s*,?\s*\)""")),
            )
            assertTrue(
                "$composable.onCtaClick must NOT have a `= {}` default — that would let " +
                    "callers silently leave the inline CTA dead.",
                !signature.contains(Regex("""onCtaClick\s*:\s*\(\s*\)\s*->\s*Unit\s*=\s*\{\s*\}""")),
            )
        }
    }

    @Test
    fun `inline CTA buttons are wired to onCtaClick, not onClick`() {
        val source = readDealsCardSource()
        // The three ProcessButton blocks (one per composable) must each pass
        // `onClick = onCtaClick`. Anything else (e.g. `onClick = onClick` or
        // `onClick = {}`) would re-introduce the bug this test exists to prevent.
        val onCtaWires = Regex("""onClick\s*=\s*onCtaClick""").findAll(source).count()
        assertEquals(
            "Expected exactly 3 `onClick = onCtaClick` wirings (one per ProcessButton " +
                "in DealsCard / LastMinuteDealCard / RentedGearDealsCard).",
            3,
            onCtaWires,
        )
    }

    private fun readDealsCardSource(): String {
        // Gradle test working dir is the module root (feature/home/presentation/).
        // Walk a couple of parents just in case a CI runner invokes the tests
        // from a different directory.
        val relative = "src/main/java/com/shourov/apps/pacedream/feature/home/" +
            "presentation/components/DealsCard.kt"
        val candidates = generateSequence(File(".").absoluteFile) { it.parentFile }
            .take(6)
            .map { File(it, relative) }
            .toList() + listOf(
            File("feature/home/presentation/$relative"),
        )
        val found = candidates.firstOrNull { it.exists() }
        assertNotNull(
            "Could not locate DealsCard.kt — looked in: ${candidates.map { it.absolutePath }}",
            found,
        )
        return found!!.readText()
    }

    /**
     * Removes every `@Preview`-annotated composable from the source so the
     * empty-lambda check ignores intentional preview-only stubs. Strips by
     * balanced-brace counting starting at the first `{` after the `@Preview`
     * annotation chain, so it handles nested braces inside preview bodies.
     */
    private fun stripPreviewBlocks(source: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < source.length) {
            val previewIdx = source.indexOf("@Preview", i)
            if (previewIdx < 0) {
                builder.append(source, i, source.length)
                break
            }
            builder.append(source, i, previewIdx)
            val bodyStart = source.indexOf('{', previewIdx)
            if (bodyStart < 0) {
                // Malformed source: bail out and let the rest be checked.
                builder.append(source, previewIdx, source.length)
                break
            }
            var depth = 0
            var j = bodyStart
            while (j < source.length) {
                when (source[j]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            j++
                            break
                        }
                    }
                }
                j++
            }
            i = j
        }
        return builder.toString()
    }

    /**
     * Extracts the parameter list (from `fun Name(` through the matching `)`)
     * for the named composable. Returns null if not found.
     */
    private fun extractComposableSignature(source: String, name: String): String? {
        val funIdx = source.indexOf("fun $name(")
        if (funIdx < 0) return null
        val openParen = source.indexOf('(', funIdx)
        var depth = 0
        var j = openParen
        while (j < source.length) {
            when (source[j]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return source.substring(funIdx, j + 1)
                }
            }
            j++
        }
        return null
    }

    private companion object {
        // Matches `onClick = {}` with any whitespace inside the braces or
        // around the equals sign. Word-boundary on `onClick` so we don't
        // catch parameter declarations like `onCtaClick`.
        private val EMPTY_LAMBDA_ONCLICK = Regex("""\bonClick\s*=\s*\{\s*\}""")
    }
}
