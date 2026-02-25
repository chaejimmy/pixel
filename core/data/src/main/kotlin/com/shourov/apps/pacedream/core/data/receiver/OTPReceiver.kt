package com.shourov.apps.pacedream.core.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.phone.SmsRetrieverClient
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

private const val OTP_RECEIVER_TAG = "OTPReceiver"

/**
 * BroadcastReceiver to wait for SMS messages. This can be registered either
 * in the AndroidManifest or at runtime. Should filter Intents on
 * SmsRetriever.SMS_RETRIEVED_ACTION.
 *
 * TODO: Working in Android 13 (API 33) and 14 (API 34), but not working in Android 12 (API 31)
 *
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
    smsRetrieverTask.addOnFailureListener { e ->
        Log.e(OTP_RECEIVER_TAG, "startSMSRetrieverClient failed", e)
    }
}