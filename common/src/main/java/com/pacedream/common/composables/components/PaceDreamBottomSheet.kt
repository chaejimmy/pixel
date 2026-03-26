package com.pacedream.common.composables.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaceDreamBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = PaceDreamRadius.XL,
            topEnd = PaceDreamRadius.XL
        ),
        containerColor = PaceDreamColors.Card,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Box(
                    modifier = Modifier
                        .width(PaceDreamRadius.XXL)
                        .height(PaceDreamSpacing.XS)
                        .background(
                            PaceDreamColors.OnSurfaceVariant,
                            RoundedCornerShape(PaceDreamRadius.Round)
                        )
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD)
                .padding(bottom = PaceDreamSpacing.XL)
        ) {
            title?.let {
                Text(
                    text = it,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    modifier = Modifier.padding(bottom = PaceDreamSpacing.MD)
                )
            }
            content()
        }
    }
}
