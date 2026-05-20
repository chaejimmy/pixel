package com.shourov.apps.pacedream.designsystem.modifier

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamElevation
import com.pacedream.common.composables.theme.PaceDreamRadius

/**
 * Theme-aware card shadow.
 *
 * In light mode this draws a soft black shadow at low alpha. In dark mode a
 * black shadow on a dark surface produces a flat void, so we instead draw a
 * faint white luminous scrim so the card still separates from the
 * background. The [intensity] multiplier scales the shadow's alpha — pass
 * `> 1f` while a press interaction is active to deepen the shadow, or
 * `< 1f` for a more subtle resting card.
 *
 * @param elevation Shadow elevation. Defaults to [PaceDreamElevation.MD].
 * @param shape Shape used for the shadow's silhouette. Defaults to a
 *   [PaceDreamRadius.LG] rounded rectangle.
 * @param intensity Multiplier applied to the base shadow alpha. `1f` is the
 *   resting value. Clamped to `0f..3f` to prevent runaway opacity.
 */
fun Modifier.adaptiveShadow(
    elevation: Dp = PaceDreamElevation.MD,
    shape: Shape = RoundedCornerShape(PaceDreamRadius.LG),
    intensity: Float = 1f,
): Modifier = composed {
    val dark = isSystemInDarkTheme()
    val scale = intensity.coerceIn(0f, 3f)

    val baseAmbientAlpha = if (dark) 0.04f else 0.06f
    val baseSpotAlpha = if (dark) 0.05f else 0.08f
    val baseColor = if (dark) Color.White else Color.Black

    this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = baseColor.copy(alpha = (baseAmbientAlpha * scale).coerceIn(0f, 1f)),
        spotColor = baseColor.copy(alpha = (baseSpotAlpha * scale).coerceIn(0f, 1f)),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews — light + dark for every elevation step. Renders a single card
// per elevation token so the relative shadow strength is easy to inspect in
// Android Studio's preview pane (and roborazzi if wired up later).
// ─────────────────────────────────────────────────────────────────────────────

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF7F7F8)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF101012,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AdaptiveShadow_XS_Preview() {
    ShadowSample(label = "XS · ${PaceDreamElevation.XS}", elevation = PaceDreamElevation.XS)
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF7F7F8)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF101012,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AdaptiveShadow_SM_Preview() {
    ShadowSample(label = "SM · ${PaceDreamElevation.SM}", elevation = PaceDreamElevation.SM)
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF7F7F8)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF101012,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AdaptiveShadow_MD_Preview() {
    ShadowSample(label = "MD · ${PaceDreamElevation.MD}", elevation = PaceDreamElevation.MD)
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF7F7F8)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF101012,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AdaptiveShadow_LG_Preview() {
    ShadowSample(label = "LG · ${PaceDreamElevation.LG}", elevation = PaceDreamElevation.LG)
}

@Composable
private fun ShadowSample(label: String, elevation: Dp) {
    val shape = RoundedCornerShape(PaceDreamRadius.LG)
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .adaptiveShadow(elevation = elevation, shape = shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "adaptiveShadow preview",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
