package com.pacedream.common.composables.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks in the PaceDream brand primary as purple #5527D7 and the WCAG
 * contrast guarantees we ship on top of it. If somebody flips the token
 * to a value that breaks contrast (e.g. the apocryphal green hex that
 * briefly appeared in DESIGN_SYSTEM_COVERAGE.md), this fails before the
 * change reaches a screen.
 *
 * Source of truth: PaceDreamPrimary in
 * common/src/main/java/com/pacedream/common/composables/theme/Color.kt
 * and DESIGN_SYSTEM_README.md "Primary Colors".
 */
class DesignSystemContrastTest {

    private val expectedPrimaryArgb: Int = 0xFF5527D7.toInt()
    private val whiteArgb: Int = 0xFFFFFFFF.toInt()

    @Test
    fun `Primary token is the documented purple 5527D7`() {
        assertEquals(
            "PaceDreamColors.Primary must remain #5527D7 — see Color.kt:108 and DESIGN_SYSTEM_README.md",
            expectedPrimaryArgb,
            PaceDreamColors.Primary.toArgb(),
        )
    }

    @Test
    fun `white on Primary meets WCAG body text contrast (>= 4_5 to 1)`() {
        val ratio = contrastRatio(whiteArgb, PaceDreamColors.Primary.toArgb())
        assertTrue(
            "White text on Primary contrast = ${"%.2f".format(ratio)}, must be >= 4.5:1 for body text",
            ratio >= 4.5,
        )
    }

    @Test
    fun `white on Primary meets WCAG large text contrast (>= 3 to 1)`() {
        val ratio = contrastRatio(whiteArgb, PaceDreamColors.Primary.toArgb())
        assertTrue(
            "White text on Primary contrast = ${"%.2f".format(ratio)}, must be >= 3.0:1 for large text",
            ratio >= 3.0,
        )
    }

    private fun contrastRatio(fgArgb: Int, bgArgb: Int): Double {
        val l1 = relativeLuminance(fgArgb)
        val l2 = relativeLuminance(bgArgb)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(argb: Int): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        return 0.2126 * srgbToLinear(r) + 0.7152 * srgbToLinear(g) + 0.0722 * srgbToLinear(b)
    }

    private fun srgbToLinear(channel: Double): Double =
        if (channel <= 0.03928) channel / 12.92 else Math.pow((channel + 0.055) / 1.055, 2.4)
}
