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

package com.shourov.apps.pacedream.signin.screens.signIn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pacedream.common.R
import com.pacedream.common.composables.VerticalSpacer
import com.pacedream.common.composables.buttons.PrimaryTextButton
import com.pacedream.common.composables.buttons.ProcessButton
import com.pacedream.common.composables.buttons.RoundIconButton
import com.pacedream.common.composables.inputfields.CustomInputTextField
import com.pacedream.common.composables.inputfields.CustomPasswordField
import com.pacedream.common.composables.texts.ClickableText
import com.pacedream.common.composables.texts.TitleText
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.signin.navigation.SignInRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignIn(
navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    RoundIconButton(
                        icon = PaceDreamIcons.ArrowBack,
                        onclick = {
                            navController.navigateUp()
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                ),
            )
        },
        containerColor = PaceDreamColors.Background,
    ){
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            color = PaceDreamColors.Background
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TitleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.welcome_to_Pacedream)
                )
                VerticalSpacer(height = 5)
                ClickableText(
                    modifier = Modifier.fillMaxWidth(),
                    nonClickPart = stringResource(id = R.string.core_ui_have_account_question,  "Don't"),
                    clickablePart = stringResource(id = R.string.core_ui_create_account),
                    onClick = {
                        navController.navigate(route = SignInRoutes.START_EMAIL_PHONE.name)
                    }
                )

                VerticalSpacer(height = 10)

                CustomInputTextField(
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email"
                        )
                    },
                    onValueChange = {
                    },
                )
                VerticalSpacer(height = 10)
                CustomPasswordField(
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Password,
                            contentDescription = "Password"
                        )
                    },
                    onValueChange = {

                    },
                )

                PrimaryTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.core_ui_forgot_password),
                    onClick = {/*TODO implement forgot password*/ },
                )

                ProcessButton(
                    onClick = { /*TODO*/ },
                    text = stringResource(id = R.string.core_ui_continue_button),
                    isEnabled = true,

                    )

            }
        }
    }
}

@Preview
@Composable
fun SignInPrev(){
    //SignIn()
}
