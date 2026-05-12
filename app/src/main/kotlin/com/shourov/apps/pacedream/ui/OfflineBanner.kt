package com.shourov.apps.pacedream.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.core.network.observer.ConnectionState
import com.shourov.apps.pacedream.core.network.observer.ConnectivityObserver
import com.shourov.apps.pacedream.designsystem.OnBrandSurface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    observer: ConnectivityObserver,
) : ViewModel() {
    val state: StateFlow<ConnectionState> = observer.state
}

/**
 * Thin top-of-screen banner that surfaces the current connectivity state.
 * Slides in when the observer reports anything other than [ConnectionState.Available]
 * and dismisses itself on reconnect — no manual dismiss handler needed.
 */
@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier,
    viewModel: ConnectivityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OfflineBanner(state = state, modifier = modifier)
}

@Composable
internal fun OfflineBanner(
    state: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val visible = state != ConnectionState.Available
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.testTag("offlineBanner"),
    ) {
        val message = when (state) {
            ConnectionState.Unavailable -> "You're offline. Some content may be unavailable."
            ConnectionState.Losing -> "Connection unstable…"
            ConnectionState.Available -> ""
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaceDreamColors.Error)
                .padding(
                    horizontal = PaceDreamSpacing.MD,
                    vertical = PaceDreamSpacing.SM,
                )
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = PaceDreamIcons.Warning,
                contentDescription = null,
                tint = OnBrandSurface,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = message,
                style = PaceDreamTypography.Caption,
                color = OnBrandSurface,
            )
        }
    }
}

@Preview(name = "Offline — light", showBackground = true)
@Composable
private fun OfflineBannerLightPreview() {
    PaceDreamTheme(darkTheme = false) {
        OfflineBanner(state = ConnectionState.Unavailable)
    }
}

@Preview(
    name = "Offline — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun OfflineBannerDarkPreview() {
    PaceDreamTheme(darkTheme = true) {
        OfflineBanner(state = ConnectionState.Unavailable)
    }
}

@Preview(name = "Losing — light", showBackground = true)
@Composable
private fun OfflineBannerLosingPreview() {
    PaceDreamTheme(darkTheme = false) {
        OfflineBanner(state = ConnectionState.Losing)
    }
}
