package com.pacedream.common.composables.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamIconSize
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@Composable
fun PaceDreamDialog(
    onDismiss: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "OK",
    dismissText: String? = null,
    onConfirm: () -> Unit,
    icon: ImageVector? = null,
    iconTint: Color = PaceDreamColors.Primary,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.XL),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.XL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(PaceDreamIconSize.XXL)
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                }

                Text(
                    text = title,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                Text(
                    text = message,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissText?.let {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = it,
                                style = PaceDreamTypography.Button,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                        shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                        modifier = Modifier.height(PaceDreamButtonHeight.SM),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = confirmText,
                            style = PaceDreamTypography.Button,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaceDreamDestructiveDialog(
    onDismiss: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "Delete",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    PaceDreamDialog(
        onDismiss = onDismiss,
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        icon = icon,
        iconTint = PaceDreamColors.Error,
        modifier = modifier,
    )
}
