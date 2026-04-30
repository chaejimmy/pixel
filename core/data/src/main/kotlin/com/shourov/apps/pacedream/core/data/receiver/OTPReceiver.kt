package com.shourov.apps.pacedream.core.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.phone.SmsRetrieverClient
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * BroadcastReceiver that listens for SMS_RETRIEVED broadcasts from Google
 * Play Services' SmsRetriever API.
 *
 * IMPORTANT: do NOT register this receiver statically in AndroidManifest.xml
 * with android:exported="true". Although the SmsRetriever API documents
 * com.google.android.gms.auth.api.phone.permission.SEND as the gating
 * permission, manifest-registered exported receivers add a long-lived,
 * always-on broadcast surface — register at runtime instead, only while
 * an OTP-entry UI is on screen, via:
 *
 *   ContextCompat.registerReceiver(
 *       context,
 *       OTPReceiver().also { it.init(listener) },
 *       IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
 *       SmsRetriever.SEND_PERMISSION,
 *       null,
 *       ContextCompat.RECEIVER_EXPORTED
 *   )
 *
 * and unregister it on screen exit / lifecycle stop.
 *
 * Note: SMS auto-retrieval is opportunistic. On unsupported devices users
 * must enter the OTP manually (handled by OtpVerificationScreen).
 */
class OTPReceiver : BroadcastReceiver() {
    private var otpReceiveListener: OTPReceiveListener? = null

    fun init(otpReceiveListener: OTPReceiveListener?) {
        this.otpReceiveListener = otpReceiveListener
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
            val extras: Bundle? = intent.extras
            val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

            when (status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    // Get SMS message contents
                    val msg = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE) ?: return

                    // extract the 6-digit code from the SMS
                    val smsCode = "[0-9]{6}".toRegex().find(msg)

                    smsCode?.value?.let { otpReceiveListener?.onOTPReceived(it) }
                }

                CommonStatusCodes.TIMEOUT -> {
                    otpReceiveListener?.onOTPTimeOut()
                }
            }
        }
    }

    interface OTPReceiveListener {
        fun onOTPReceived(otp: String?)
        fun onOTPTimeOut()
    }
}

fun startSMSRetrieverClient(context: Context) {
    val client: SmsRetrieverClient = SmsRetriever.getClient(context)
    val smsRetrieverTask = client.startSmsRetriever()
    smsRetrieverTask.addOnSuccessListener {
        // SMS retriever started successfully
    }
    smsRetrieverTask.addOnFailureListener {
        // Non-fatal: user can still enter OTP manually
    }
}