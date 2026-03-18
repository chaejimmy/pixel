package com.pacedream.common.composables.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamIconSize
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@Composable
fun PaceDreamBadge(
    count: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PaceDreamColors.Error,
    contentColor: Color = Color.White,
) {
    if (count <= 0) return

    val text = if (count > 99) "99+" else count.toString()

    Box(
        modifier = modifier
            .sizeIn(minWidth = PaceDreamIconSize.SM, minHeight = PaceDreamIconSize.SM)
            .background(backgroundColor, RoundedCornerShape(PaceDreamRadius.Round))
            .padding(horizontal = PaceDreamSpacing.XS),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = PaceDreamTypography.Caption2,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PaceDreamDotBadge(
    modifier: Modifier = Modifier,
    color: Color = PaceDreamColors.Error,
) {
    Box(
        modifier = modifier
            .size(PaceDreamSpacing.SM)
            .background(color, CircleShape)
    )
}
