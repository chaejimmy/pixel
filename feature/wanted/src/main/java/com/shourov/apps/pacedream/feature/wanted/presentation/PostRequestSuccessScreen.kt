@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.wanted.BuildConfig

/**
 * Confirmation screen shown after a request is successfully posted.
 *
 * Mirrors the web `/post-request/success?id={requestId}` page:
 *  - Big success checkmark + reassuring copy
 *  - Primary action: "View my requests" (where the user can track offers)
 *  - Secondary: "Post another" / "Back to home"
 *
 * Visual style follows PaceDreamDesignSystem tokens — no raw hex, no new
 * patterns.
 */
@Composable
fun PostRequestSuccessScreen(
    requestId: String,
    onViewMyRequests: () -> Unit,
    onPostAnother: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Request posted",
                        style = PaceDreamTypography.Headline,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = PaceDreamIcons.Close,
                            contentDescription = "Close",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background,
                ),
            )
        },
        containerColor = PaceDreamColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = PaceDreamSpacing.LG),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = PaceDreamColors.Success.copy(alpha = 0.15f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PaceDreamIcons.CheckCircle,
                    contentDescription = null,
                    tint = PaceDreamColors.Success,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(Modifier.height(PaceDreamSpacing.MD))

            Text(
                text = "Your request is live",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = "Providers can now see your request and send offers. " +
                    "We'll let you know in your Inbox when an offer arrives.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
            )

            Spacer(Modifier.height(PaceDreamSpacing.LG))

            Button(
                onClick = onViewMyRequests,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary,
                    contentColor = PaceDreamColors.OnPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Track my requests",
                    style = PaceDreamTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(PaceDreamSpacing.SM))

            OutlinedButton(
                onClick = onPostAnother,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Post another",
                    style = PaceDreamTypography.Headline,
                )
            }

            // QA / support correlation only — never shipped in release.
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(PaceDreamSpacing.MD))
                Text(
                    text = "ID · ${requestId.take(8)}",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextTertiary,
                )
            }
        }
    }
}
