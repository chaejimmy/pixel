package com.shourov.apps.pacedream.create_account.navigation

import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shourov.apps.pacedream.core.common.result.Result.Loading
import com.shourov.apps.pacedream.core.ui.otp.OtpScreen

@RequiresApi(VERSION_CODES.TIRAMISU)
@Composable
fun OTPVerification() {
    OtpScreen(
        otpValue = "",
        onOtpModified = { otp, isComplete ->
            // todo
        },
        userPhoneNumber = "",
        onHaveNotReceivedOtp = {
            // todo
        },
        onConfirmOtp = {
            // todo
        },
        otpVerificationState = Loading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 16.dp,
            ),
    )
}

@Preview(showBackground = true)
@RequiresApi(VERSION_CODES.TIRAMISU)
@Composable
fun PreviewOTPVerification() {
   // PaceDreamTheme {
        OTPVerification()
   // }
}