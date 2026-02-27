package com.shourov.apps.pacedream.signin.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.buttons.ProcessButton
import com.shourov.apps.pacedream.core.data.AccountCreationProcess
import com.shourov.apps.pacedream.core.data.AccountSetupScreenData
import com.shourov.apps.pacedream.core.ui.AccountCreationHeader
import com.shourov.apps.pacedream.core.ui.R
import com.shourov.apps.pacedream.signin.AccountSetupViewModel
import com.shourov.apps.pacedream.signin.screens.createAccount.components.Password
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.shourov.apps.pacedream.core.data.AccountCreationProcess.HOBBIES_N_INTERESTS_SETUP
import com.shourov.apps.pacedream.core.data.AccountCreationProcess.PASSWORD_SETUP
import com.shourov.apps.pacedream.core.data.AccountCreationProcess.PERSONAL_DETAILS_SETUP
import com.shourov.apps.pacedream.core.data.AccountCreationProcess.PROFILE_PICTURE_SETUP
import com.shourov.apps.pacedream.core.data.AccountCreationProcess.USER_PROFILE_SETUP

private const val CONTENT_ANIMATION_DURATION = 300

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit = {},
) {

    val setupViewModel: AccountSetupViewModel = hiltViewModel()
    val accountSetupScreenData = setupViewModel.accountSetupScreenData

    PaceDreamTheme {
        SetupContent(
            accountSetupScreenData = accountSetupScreenData,
            shouldShowPreviousButton = accountSetupScreenData.processIndex > 0,
            shouldShowDoneButton = accountSetupScreenData.shouldShowDoneButton,
            onPreviousPressed = setupViewModel::onPreviousClicked,
            isContinueEnabled = true,
            onContinueClick = setupViewModel::onContinueClicked,
            onHelpClick = {},
            onDoneClick = {
                setupViewModel.onDoneClicked(
                    onSuccess = { onSetupComplete() },
                    onError = { /* Error handled via setupError state */ }
                )
            },
            content = {
                AnimatedContent(
                    targetState = accountSetupScreenData,
                    label = "accountSetupScreenDataAnimation",
                    transitionSpec = {
                        val animationSpec: TweenSpec<IntOffset> =
                            tween(CONTENT_ANIMATION_DURATION)
                        val direction = getTransitionDirection(
                            initialIndex = initialState.processIndex,
                            targetIndex = targetState.processIndex,
                        )
                        slideIntoContainer(
                            towards = direction,
                            animationSpec = animationSpec,
                        ) togetherWith slideOutOfContainer(
                            towards = direction,
                            animationSpec = animationSpec,
                        )
                    },
                ) { targetState ->
                    when (targetState.accountCreationProcess) {
                        PASSWORD_SETUP -> {
                            Password(
                                onPasswordResponse = setupViewModel::onPasswordResponse,
                            )
                        }

                        PROFILE_PICTURE_SETUP -> {
                            ProfileSetup()
                        }

                        USER_PROFILE_SETUP -> {
                            UsernameDetails(
                                onUserProfileResponse = setupViewModel::onUserProfileResponse,
                            )
                        }

                        PERSONAL_DETAILS_SETUP -> {
                            UserDob(
                                onDateSelected = setupViewModel::onDateOfBirthResponse,
                                dateOfBirth = setupViewModel.accountSetupData.dateOfBirthMillis,
                            )
                        }

                        HOBBIES_N_INTERESTS_SETUP -> {
                            HobbiesNInterests()
                        }
                    }
                }
            },
        )
    }
}

@Composable
fun SetupContent(
    modifier: Modifier = Modifier,
    accountSetupScreenData: AccountSetupScreenData,
    isContinueEnabled: Boolean,
    onContinueClick: () -> Unit,
    onDoneClick: () -> Unit,
    content: @Composable () -> Unit,
    onHelpClick: () -> Unit,
    shouldShowPreviousButton: Boolean,
    onPreviousPressed: () -> Unit,
    shouldShowDoneButton: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        val title = when (accountSetupScreenData.accountCreationProcess) {
            AccountCreationProcess.PASSWORD_SETUP -> stringResource(id = R.string.core_ui_password_title) to stringResource(
                id = R.string.core_ui_password_setup_subtitle,
            )

            AccountCreationProcess.HOBBIES_N_INTERESTS_SETUP -> stringResource(id = R.string.core_ui_hobbies_n_interests) to stringResource(
                id = R.string.core_ui_hobbies_n_interests_subtitle,
            )

            else -> stringResource(id = R.string.core_ui_setup_basic) to stringResource(id = R.string.core_ui_setup_basic_subtitle)
        }

        AccountCreationHeader(
            title = title.first,
            onNavigateToCreateAccount = {},
            onNavigateToAccountSignIn = {},
            subtitle = title.second,
            onHeaderActionClick = onHelpClick,
            actionText = if (accountSetupScreenData.accountCreationProcess == AccountCreationProcess.PASSWORD_SETUP) stringResource(
                id = R.string.core_ui_help_text,
            )
            else "",
        )

        content()

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = WindowInsets.systemBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                    ),
            ) {
                if (shouldShowPreviousButton) {
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f),
                        onClick = onPreviousPressed,
                    ) {
                        Text(text = stringResource(id = R.string.feature_signin_ui_previous))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                val buttonText =
                    if (shouldShowDoneButton) stringResource(id =  com.pacedream.common.R.string.core_designsystem_done)
                    else stringResource(id =  com.pacedream.common.R.string.core_designsystem_process_button_default_text)
                ProcessButton(
                    text = buttonText,
                    onClick = if (shouldShowDoneButton) onDoneClick else onContinueClick,
                    isEnabled = isContinueEnabled,
                    modifier = Modifier
                        .weight(1f),
                )
            }
        }
    }
}

private fun getTransitionDirection(
    initialIndex: Int,
    targetIndex: Int,
): AnimatedContentTransitionScope.SlideDirection {
    return if (targetIndex > initialIndex) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
}