package com.shourov.apps.pacedream.feature.home.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        accentColor = Color(0xFF5527D7),
        description = "Parking, rooms, pods & more",
    ),
    CategoryChipData(
        key = "items",
        title = "Items",
        icon = Icons.Default.Inventory2,
        accentColor = Color(0xFF3B82F6),
        description = "Cameras, gear, tools & tech",
    ),
    CategoryChipData(
        key = "services",
        title = "Services",
        icon = Icons.Default.CleaningServices,
        accentColor = Color(0xFF10B981),
        description = "Cleaning, moving, fitness & more",
    ),
)

@Composable
fun QuickChipsSection(
    onChipClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedChip by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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

@Composable
private fun CategoryChip(
    data: CategoryChipData,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) data.accentColor else Color.White,
        animationSpec = tween(200),
        label = "chipBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else PaceDreamTextPrimary,
        animationSpec = tween(200),
        label = "chipContent",
    )

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                listOf(
                    data.accentColor.copy(alpha = 0.25f),
                    data.accentColor.copy(alpha = 0.25f),
                )
            )
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else data.accentColor,
                modifier = Modifier.size(18.dp),
            )
            Column {
                Text(
                    text = data.title,
                    style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor,
                )
                Text(
                    text = data.description,
                    style = PaceDreamTypography.Caption2,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else PaceDreamTextSecondary,
                )
            }
        }
    }
}
