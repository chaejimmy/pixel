import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rejowan.ccpc.Country
import com.rejowan.ccpc.Country.UnitedStates
import com.rejowan.ccpc.CountryCodePicker
import com.rejowan.ccpc.PickerCustomization
import com.rejowan.ccpc.ViewCustomization
import com.shourov.apps.pacedream.core.data.UserAuthPath
import com.shourov.apps.pacedream.core.ui.AccountCreationHeader
import com.shourov.apps.pacedream.core.ui.textFieldStates.PhoneNumberValidationState
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.pacedream.common.composables.theme.stronglyDeemphasizedAlpha
import com.shourov.apps.pacedream.core.data.UserAuthPath.NEW
import com.shourov.apps.pacedream.core.ui.R as CoreUiR

@Composable
fun PhoneEntryScreen(
    modifier: Modifier = Modifier,
    onProceedToOtpVerification: () -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToAccountSignIn: () -> Unit,
    onNavigateToAccountSetup: () -> Unit,
    userAuthPath: UserAuthPath,
) {
    var phoneNumberState by remember {
        mutableStateOf(
            PhoneNumberValidationState(
                countryCode = "US",
            ),
        )
    }
    var countryCode by remember {
        mutableStateOf("US")
    }

    LaunchedEffect(countryCode) {
        phoneNumberState = PhoneNumberValidationState(
            countryCode = countryCode,
        )
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier,
    ) {
        AccountCreationHeader(
            title = stringResource(id = CoreUiR.string.core_ui_auth_header_title, "phone"),
            onNavigateToCreateAccount = onNavigateToCreateAccount,
            onNavigateToAccountSignIn = onNavigateToAccountSignIn,
            userAuthPath = userAuthPath,
        )

        PhoneEntryTextField(
            onPhoneNumberChanged = {
                phoneNumberState.text = it
            },
            phoneNumber = phoneNumberState.text,
            onClear = {
                phoneNumberState.text = ""
            },
            onDone = { phoneNumberState.enableShowErrors() },
            showsErrors = phoneNumberState.showErrors(),
            onCountrySelected = {
                countryCode = it.countryCode
            },
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = {
                phoneNumberState.enableShowErrors()
//                if (phoneNumberState.isValid) {
//                    onProceedToOtpVerification()
//                }
                onNavigateToAccountSetup()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
            ,
        ) {
            Text(text = stringResource(id = CoreUiR.string.core_ui_continue_button))
        }
    }
}

@Composable
fun PhoneNumberInput(
    modifier: Modifier = Modifier,
    onPhoneNumberChanged: (String) -> Unit,
    phoneNumber: String,
    onClear: () -> Unit = {},
    onDone: () -> Unit = {},
    focusManager: FocusManager = LocalFocusManager.current,
    hasError: Boolean = false,
) {
    var hasFocus by remember {
        mutableStateOf(false)
    }
    val color by animateColorAsState(
        targetValue = if (hasError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "phone_number_text_color",
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChanged,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onDone()
                },
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = color,
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        hasFocus = true
                    }
                },
            decorationBox = { innerTextField ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (phoneNumber.isEmpty() && !hasFocus) {
                        true -> Text(
                            text = stringResource(id = CoreUiR.string.core_ui_phone_number_placehoolder),
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = stronglyDeemphasizedAlpha,
                            ),
                        )

                        false -> innerTextField()
                    }
                }
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        AnimatedVisibility(
            visible = phoneNumber.isNotEmpty(),
            enter = scaleIn(),
            exit = scaleOut(),
        ) {
            IconButton(
                onClick = onClear,
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Cancel,
                    contentDescription = "clear text",
                    modifier = Modifier.size(24.dp),
                    tint = color,
                )
            }
        }
    }
}

@Composable
fun PhoneEntryTextField(
    modifier: Modifier = Modifier,
    onPhoneNumberChanged: (String) -> Unit,
    phoneNumber: String,
    onClear: () -> Unit = {},
    onDone: () -> Unit = {},
    showsErrors: Boolean = false,
    focusManager: FocusManager = LocalFocusManager.current,
    onCountrySelected: (Country) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            var country by remember {
                mutableStateOf(UnitedStates)
            }
            val background by animateColorAsState(
                targetValue = if (showsErrors) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(
                        stronglyDeemphasizedAlpha,
                    )
                },
                label = "phone_number_border_color",
            )

            CountryCodePicker(
                modifier = Modifier
                    .height(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            stronglyDeemphasizedAlpha,
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ),
                selectedCountry = country,
                onCountrySelected = {
                    country = it
                    onCountrySelected(it)
                },
                viewCustomization = ViewCustomization(
                    showFlag = true,
                    showCountryIso = false,
                    showCountryName = false,
                    showCountryCode = true,
                    clipToFull = false,
                ),
                pickerCustomization = PickerCustomization(
                    showFlag = false,
                ),
                showSheet = false,
            )

            PhoneNumberInput(
                onPhoneNumberChanged = onPhoneNumberChanged,
                phoneNumber = phoneNumber,
                onClear = onClear,
                onDone = {
                    focusManager.clearFocus()
                    onDone()
                },
                hasError = showsErrors,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(
                        color = background,
                        shape = MaterialTheme.shapes.medium,
                    ),
                focusManager = focusManager,
            )
        }
        AnimatedVisibility(
            visible = showsErrors,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.padding(end = 8.dp),
        ) {
            Text(
                text = stringResource(id = CoreUiR.string.core_ui_phone_number_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
fun PhoneEntryTextFieldPreview() {
    PaceDreamTheme {
        PhoneEntryTextField(
            onPhoneNumberChanged = {},
            phoneNumber = "3468349",
            onClear = {},
            onDone = {},
            showsErrors = false,
            onCountrySelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PhoneEntryScreenPreview() {
    PaceDreamTheme {
        PhoneEntryScreen(
            onProceedToOtpVerification = {},
            onNavigateToCreateAccount = {},
            onNavigateToAccountSignIn = {},
            onNavigateToAccountSetup = {},
            userAuthPath = NEW,
        )
    }
}