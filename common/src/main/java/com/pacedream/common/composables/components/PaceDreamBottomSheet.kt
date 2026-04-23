@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * Bottom sheet wrapper with PaceDream styling.
 *
 * The underlying `ModalBottomSheet` / `SheetState` APIs are still marked
 * `@ExperimentalMaterial3Api`.  To avoid forcing every call site to opt
 * in, this wrapper does NOT expose `SheetState` in its signature — it
 * always uses the default `rememberModalBottomSheetState()` internally,
 * and the file is opted in once at the top via `@file:OptIn`.
 */
@Composable
fun PaceDreamBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
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
