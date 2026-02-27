/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.signin.screens.createAccount

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.pacedream.common.composables.VerticalSpacer
import com.pacedream.common.composables.buttons.ProcessButton
import com.pacedream.common.composables.texts.TitleText
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.core.ui.R
import com.shourov.apps.pacedream.signin.screens.createAccount.components.CreateAccountParts

@Composable
fun CreateAccountScreen(
    navHostController: NavHostController
) {

    val createAccountViewModel = hiltViewModel<CreateAccountViewModel>()
    val context = LocalContext.current

    LaunchedEffect(key1 = createAccountViewModel.toastMessage.value) {
        if (createAccountViewModel.toastMessage.value.isNotBlank()){
            Toast.makeText(context, createAccountViewModel.toastMessage.value, Toast.LENGTH_SHORT).show()
            createAccountViewModel.toastMessage.value = ""
        }
    }

    Scaffold(
        containerColor = PaceDreamColors.Background,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            color = PaceDreamColors.Background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.Center
            ) {

                    TitleText(text = createAccountViewModel.accountCreationScreenState.tittle)
                    VerticalSpacer(height = 10)
                    CreateAccountParts(
                        state = createAccountViewModel.accountCreationScreenState.currentComponent,
                        accountDataStateChange = { createAccountData ->
                            createAccountViewModel.setCreateAccountData(createAccountData)
                        }
                    )

                VerticalSpacer(height = 10)

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
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
                        if (createAccountViewModel.accountCreationScreenState.showPreviousButton) {
                            OutlinedButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(PaceDreamButtonHeight.MD),
                                onClick = {
                                    createAccountViewModel.onPreviousClicked()
                                },
                                shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = PaceDreamColors.TextPrimary,
                                ),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.feature_signin_ui_previous),
                                    style = PaceDreamTypography.Button,
                                )
                            }
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                        }

                        ProcessButton(
                            modifier = Modifier.weight(1f),
                            text = if (createAccountViewModel.accountCreationScreenState.showDoneButton) stringResource(id = com.pacedream.common.R.string.core_designsystem_done) else stringResource(
                                id = R.string.core_ui_continue_button,
                            ),
                            onClick = {
                                if (createAccountViewModel.accountCreationScreenState.showDoneButton){
                                    createAccountViewModel.onDoneClicked()
                                }else{
                                    createAccountViewModel.onContinueClicked()
                                }
                            },

                            )
                    }
                }

            }
        }
    }
}

@Preview
@Composable
fun CreateAccountPrev(){
   // CreateAccountScreen()
}