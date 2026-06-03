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
import com.pacedream.common.composables.theme.paceDreamDisplayFontFamily
import com.pacedream.common.composables.theme.paceDreamFontFamily
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.wanted.BuildConfig
import com.shourov.apps.pacedream.feature.wanted.model.ModerationStatus

/**
 * Confirmation screen shown after a request is successfully posted.
 *
 * Mirrors the web `/post-request/success?id={requestId}` page:
 *  - Big success checkmark + moderation-aware reassuring copy
 *  - Primary action: "Track my requests" (where the user can monitor
 *    moderation + offers)
 *  - Secondary: "Post another"
 *
 * The web platform queues every new request for review before it is
 * publishable. The success screen surfaces that explicitly so the user
 * doesn't expect immediate visibility on the public feed.
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
    moderationStatus: ModerationStatus = ModerationStatus.PendingReview,
) {
    val pendingReview = moderationStatus != ModerationStatus.Approved
    val accentColor = if (pendingReview) PaceDreamColors.Warning else PaceDreamColors.Success
    val icon = if (pendingReview) PaceDreamIcons.Info else PaceDreamIcons.CheckCircle
    val title = if (pendingReview) "Submitted for review" else "Your request is live"
    val body = if (pendingReview) {
        "Thanks — a reviewer will publish your request shortly, usually within " +
            "a few hours. We'll notify you the moment it's live and providers " +
            "can start sending offers."
    } else {
        "Providers can now see your request and send offers. We'll let you " +
            "know in your Inbox when an offer arrives."
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (pendingReview) "Submitted" else "Request posted",
                        style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
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
                    // intentional: 72dp success badge — a fixed hero-icon size.
                    .size(72.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    // intentional: 40dp glyph centered in the 72dp badge.
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(Modifier.height(PaceDreamSpacing.MD))

            Text(
                text = title,
                style = PaceDreamTypography.Title2.copy(fontFamily = paceDreamDisplayFontFamily),
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = body,
                style = PaceDreamTypography.Body.copy(fontFamily = paceDreamFontFamily),
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
                    style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
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
                    style = PaceDreamTypography.Headline.copy(fontFamily = paceDreamFontFamily),
                )
            }

            // QA / support correlation only — never shipped in release.
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(PaceDreamSpacing.MD))
                Text(
                    text = "ID · ${requestId.take(8)}",
                    style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                    color = PaceDreamColors.TextTertiary,
                )
            }
        }
    }
}
