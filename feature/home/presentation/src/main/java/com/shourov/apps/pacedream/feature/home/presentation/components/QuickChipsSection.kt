package com.shourov.apps.pacedream.feature.home.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.shourov.apps.pacedream.designsystem.OnBrandSurface
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*

/**
 * Primary category rail aligned with the PaceDream marketplace taxonomy:
 * Spaces, Items, Services.
 *
 * Each chip uses its pillar accent color and toggles a selection state that
 * the parent can use to filter the content below.
 */

data class CategoryChipData(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val description: String,
)

val marketplaceCategories = listOf(
    CategoryChipData(
        key = "spaces",
        title = "Spaces",
        icon = Icons.Default.Business,
        accentColor = PaceDreamColors.Primary,
        description = "Parking, rooms, pods & more",
    ),
    CategoryChipData(
        key = "items",
        title = "Items",
        icon = Icons.Default.Inventory2,
        accentColor = PaceDreamColors.Info,
        description = "Cameras, gear, tools & tech",
    ),
    CategoryChipData(
        key = "services",
        title = "Services",
        icon = Icons.Default.CleaningServices,
        accentColor = PaceDreamColors.Success,
        description = "Cleaning, moving, fitness & more",
    ),
)

@Composable
fun QuickChipsSection(
    onChipClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedChip by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            marketplaceCategories.forEach { chip ->
                val isSelected = selectedChip == chip.key
                CategoryChip(
                    data = chip,
                    isSelected = isSelected,
                    onClick = {
                        selectedChip = if (selectedChip == chip.key) null else chip.key
                        onChipClick(chip.key)
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    data: CategoryChipData,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) OnBrandSurface else PaceDreamTextPrimary,
        animationSpec = tween(200),
        label = "chipContent",
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 0.dp,
        animationSpec = tween(200),
        label = "chipElev",
    )

    val chipShape = RoundedCornerShape(PaceDreamRadius.LG)

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = chipShape,
        shadowElevation = elevation,
    ) {
        Box(
            modifier = Modifier
                .clip(chipShape)
                .then(
                    if (isSelected) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(data.accentColor, data.accentColor.copy(alpha = 0.85f))
                            )
                        )
                    } else {
                        Modifier
                            .background(PaceDreamSurface)
                            .border(
                                width = 1.dp,
                                color = data.accentColor.copy(alpha = 0.18f),
                                shape = chipShape,
                            )
                    }
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Icon container with accent background when unselected
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                        .background(
                            if (isSelected) OnBrandSurface.copy(alpha = 0.2f)
                            else data.accentColor.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = null,
                        tint = if (isSelected) OnBrandSurface else data.accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = data.title,
                        style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = contentColor,
                    )
                    Text(
                        text = data.description,
                        style = PaceDreamTypography.Caption2,
                        color = if (isSelected) OnBrandSurface.copy(alpha = 0.8f)
                        else PaceDreamTextSecondary,
                    )
                }
            }
        }
    }
}

@Preview(name = "QuickChipsSection — light", showBackground = true)
@Composable
private fun QuickChipsSectionLightPreview() {
    PaceDreamTheme(darkTheme = false) {
        QuickChipsSection()
    }
}

@Preview(
    name = "QuickChipsSection — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun QuickChipsSectionDarkPreview() {
    PaceDreamTheme(darkTheme = true) {
        QuickChipsSection()
    }
}
