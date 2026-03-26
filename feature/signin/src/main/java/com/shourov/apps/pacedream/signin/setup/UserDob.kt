package com.shourov.apps.pacedream.signin.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shourov.apps.pacedream.core.ui.ShowDatePicker
import com.shourov.apps.pacedream.feature.signin.R
import com.pacedream.common.composables.buttons.CustomDateTimePickerButton
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

const val simpleDateFormatPattern = "EEE, MMM d yyyy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDob(
    modifier: Modifier = Modifier,
    onDateSelected: (Long) -> Unit,
    dateOfBirth: Long? = null,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat(simpleDateFormatPattern, Locale.getDefault())
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val dateString = dateOfBirth?.let { dateFormat.format(it) }
        ?: stringResource(id = R.string.feature_signin_ui_dob_label)
    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Text(
            text = "Date of Birth",
        )
        CustomDateTimePickerButton(
            text = dateString,
            onClick = {
                showDatePicker = true
            },
            modifier = Modifier
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        // todo gender picker
    }

    AnimatedVisibility(
        visible = showDatePicker,
        label = "dob_picker_dialog_visibility",
    ) {
        ShowDatePicker(
            onDateSelected = {
                onDateSelected(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Preview
@Composable
fun DobPrev(){
    //UserDob(onDateSelected = {})
    CustomDateTimePickerButton(text = "", onClick = { /*TODO*/ })
}