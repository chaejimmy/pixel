package com.shourov.apps.pacedream.feature.host.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacedream.common.composables.theme.*
import com.pacedream.common.icon.PaceDreamIcons

// =============================================================================
// Host Design System — Shared Components (iOS parity)
//
// All host screens must use these components to ensure visual consistency.
// Host accent = PaceDreamColors.HostAccent (#10B981, green).
// =============================================================================

// ── KPI Chip ─────────────────────────────────────────────────────────────────
// iOS: 160dp width, 14dp padding, 16dp radius, 1dp elevation, SM icon (17dp)
// tinted with HostAccent, value in Title2 (22sp bold), title in Caption semibold.

@Composable
fun HostKpiChip(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = PaceDreamColors.HostAccent,
    valueColor: Color? = null
) {
    Card(
        modifier = modifier.width(160.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = value,
                style = PaceDreamTypography.Title2,
                color = valueColor ?: PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

// ── Dashboard Section Header ─────────────────────────────────────────────────
// iOS: Subheadline SemiBold TextSecondary for title, optional "See all" link.

@Composable
fun HostSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onViewAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = PaceDreamColors.TextSecondary
        )
        if (onViewAll != null) {
            TextButton(onClick = onViewAll) {
                Text(
                    text = "See all",
                    style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
    }
}

// ── Settings Section Header ──────────────────────────────────────────────────
// iOS: Caption SemiBold TextTertiary — used above grouped card sections.

@Composable
fun HostSettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
        color = PaceDreamColors.TextTertiary,
        modifier = modifier.padding(
            start = PaceDreamSpacing.XS,
            bottom = PaceDreamSpacing.XS
        )
    )
}

// ── Profile / Tool Row ───────────────────────────────────────────────────────
// iOS: 24dp icon (no container), 16dp gap, Callout text, 18dp chevron.
// Used in profile tool/settings cards.

@Composable
fun HostProfileRow(
    icon: ImageVector,
    title: String,
    iconTint: Color = PaceDreamColors.HostAccent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
        Text(
            text = title,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Settings Row ─────────────────────────────────────────────────────────────
// iOS: 36dp circle icon container (10% alpha), SM icon, Callout SemiBold title,
// Caption subtitle, 18dp chevron TextTertiary.

@Composable
fun HostSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
        }
        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Row Divider ──────────────────────────────────────────────────────────────
// iOS: 0.5dp border, indented to start after icon+gap (56dp).

@Composable
fun HostRowDivider() {
    HorizontalDivider(
        color = PaceDreamColors.Border,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp)
    )
}

// ── Grouped Card Container ──────────────────────────────────────────────────
// iOS: 16dp radius, 1dp elevation, Card background. Wraps rows in settings/profile.

@Composable
fun HostGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        content = { Column(content = content) }
    )
}

// ── Alert Banner ─────────────────────────────────────────────────────────────
// iOS: 12dp radius, semantic color at 10% alpha bg, SM icon, Subheadline SemiBold.
// Optional dismiss/retry action.

@Composable
fun HostAlertBanner(
    text: String,
    color: Color,
    icon: ImageVector = PaceDreamIcons.Warning,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS)
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(PaceDreamIconSize.SM)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM)
            ) {
                Text(
                    text = actionLabel,
                    style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = color
                )
            }
        }
    }
}

// ── Empty State ──────────────────────────────────────────────────────────────
// iOS: 48dp icon at 40% alpha TextSecondary, Headline SemiBold title,
// Subheadline TextSecondary subtitle centered. Optional CTA button.

@Composable
fun HostEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaceDreamSpacing.XXXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(PaceDreamIconSize.XXL)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Text(
            text = title,
            style = PaceDreamTypography.Headline.copy(fontWeight = FontWeight.SemiBold),
            color = PaceDreamColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
        Text(
            text = subtitle,
            style = PaceDreamTypography.Subheadline,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        if (ctaLabel != null && onCta != null) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            HostCapsuleButton(
                icon = PaceDreamIcons.Add,
                title = ctaLabel,
                onClick = onCta
            )
        }
    }
}

// ── Capsule Button (CTA Pill) ────────────────────────────────────────────────
// iOS: Full-round shape, 14h/10v padding, 0dp elevation, SM icon, 14sp bold.

@Composable
fun HostCapsuleButton(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = PaceDreamColors.HostAccent
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(PaceDreamIconSize.SM)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White,
            style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Full-width Action Button ─────────────────────────────────────────────────
// iOS: MD radius, full-width, icon + text + chevron.

@Composable
fun HostFullWidthButton(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = PaceDreamColors.HostAccent,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
    }
}

// ── Switch to Guest Mode Row ─────────────────────────────────────────────────
// iOS: Card row with HostAccent icon/text, chevron.

@Composable
fun HostSwitchModeRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HostGroupedCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.SwapHoriz,
                contentDescription = null,
                tint = PaceDreamColors.HostAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Text(
                text = "Switch to Guest Mode",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.HostAccent,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Sign Out Row ─────────────────────────────────────────────────────────────
// iOS: Card row with Error icon/text, no chevron.

@Composable
fun HostSignOutRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HostGroupedCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.ExitToApp,
                contentDescription = null,
                tint = PaceDreamColors.Error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Text(
                text = "Sign Out",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.Error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Sign Out Confirmation Dialog ─────────────────────────────────────────────
// iOS: "Sign out?" / "You can sign back in anytime."

@Composable
fun HostSignOutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Sign out?", style = PaceDreamTypography.Title3)
        },
        text = {
            Text(
                "You can sign back in anytime.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Error),
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            ) {
                Text("Sign Out", style = PaceDreamTypography.Button)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PaceDreamColors.TextPrimary)
            }
        },
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        containerColor = PaceDreamColors.Card
    )
}

// ── Payout Badge ─────────────────────────────────────────────────────────────
// iOS: Caption bold, Success, 14% alpha bg, full-round pill.

@Composable
fun HostPayoutBadge(
    text: String,
    color: Color = PaceDreamColors.HostAccent,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = modifier
            .background(
                color.copy(alpha = 0.14f),
                shape = RoundedCornerShape(PaceDreamRadius.Round)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

// ── Guest Initials Circle ────────────────────────────────────────────────────
// iOS: 40dp circle, HostAccent 16% alpha bg, bold initials.

@Composable
fun HostInitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 40,
    color: Color = PaceDreamColors.HostAccent
) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { "G" }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = color,
            fontWeight = FontWeight.Bold,
            style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp)
        )
    }
}

// ── Host Filter Chip ─────────────────────────────────────────────────────────
// iOS: Round shape, HostAccent selected bg, white label. Unselected: Card bg, Border.

@Composable
fun HostFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = PaceDreamTypography.Caption,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PaceDreamColors.HostAccent,
            selectedLabelColor = Color.White,
            containerColor = PaceDreamColors.Card,
            labelColor = PaceDreamColors.TextPrimary
        ),
        elevation = FilterChipDefaults.filterChipElevation(
            elevation = if (selected) PaceDreamElevation.SM else 0.dp
        ),
        border = if (!selected) {
            FilterChipDefaults.filterChipBorder(
                borderColor = PaceDreamColors.Border,
                enabled = true,
                selected = false
            )
        } else null
    )
}

// ── Segmented Control ────────────────────────────────────────────────────────
// iOS: Surface bg, SM radius, 4dp internal padding, Card bg for active tab.

@Composable
fun HostSegmentedControl(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM)
            .background(PaceDreamColors.Surface, RoundedCornerShape(PaceDreamRadius.SM))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Surface(
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(index) },
                color = if (selected) PaceDreamColors.Card else PaceDreamColors.Surface,
                shadowElevation = if (selected) PaceDreamElevation.XS else 0.dp,
                shape = RoundedCornerShape(PaceDreamRadius.SM)
            ) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Subheadline,
                    color = if (selected) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}
