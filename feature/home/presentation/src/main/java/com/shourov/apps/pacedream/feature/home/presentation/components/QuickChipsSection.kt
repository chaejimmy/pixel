package com.shourov.apps.pacedream.feature.home.presentation.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*

/**
 * Quick filter chips matching iOS QuickChips.swift
 *
 * Horizontal scroll of colored category chips below the search bar.
 * Tapping a chip triggers a category search.
 */

private data class QuickChipData(
    val title: String,
    val icon: ImageVector,
    val color: Color,
)

private val quickChips = listOf(
    QuickChipData("Rooms", Icons.Default.Bed, Color(0xFF3B82F6)),
    QuickChipData("Parking", Icons.Default.DirectionsCar, Color(0xFFF59E0B)),
    QuickChipData("Gear", Icons.Default.Build, Color(0xFF10B981)),
    QuickChipData("Experiences", Icons.Default.Star, Color(0xFFEC4899)),
    QuickChipData("Nearby", Icons.Default.LocationOn, Color(0xFF8B5CF6)),
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        quickChips.forEach { chip ->
            val isSelected = selectedChip == chip.title
            QuickChip(
                title = chip.title,
                icon = chip.icon,
                color = chip.color,
                isSelected = isSelected,
                onClick = {
                    selectedChip = if (selectedChip == chip.title) null else chip.title
                    onChipClick(chip.title)
                },
            )
        }
    }
}

@Composable
private fun QuickChip(
    title: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) color else Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = if (isSelected) 0.dp else 1.dp,
        border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.3f))
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else color,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = title,
                style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.Medium),
                color = if (isSelected) Color.White else PaceDreamTextPrimary,
            )
        }
    }
}
