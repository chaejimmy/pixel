package com.shourov.apps.pacedream.core.network.remote.interceptor

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import okhttp3.Interceptor
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * OkHttp interceptor that attaches lightweight telemetry headers to every request.
 * These help the backend correlate requests for abuse detection and monitoring.
 *
 * Headers sent:
 *  - X-App-Version: app versionName (e.g. "1.2.3")
 *  - X-App-Platform: "android"
 *  - X-Device-Model: manufacturer + model (e.g. "Google Pixel 7")
 *  - X-OS-Version: Android version (e.g. "14")
 *  - X-Client-Timestamp: ISO-8601 UTC timestamp of when the request was made
 *
 * No PII, device IDs, or advertising IDs are sent.
 */
class TelemetryInterceptor(context: Context) : Interceptor {

    private val appVersion: String
    private val deviceModel: String
    private val osVersion: String

    init {
        val pm = context.packageManager
        appVersion = try {
            pm.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        osVersion = Build.VERSION.RELEASE
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-App-Version", appVersion)
            .header("X-App-Platform", "android")
            .header("X-Device-Model", deviceModel)
            .header("X-OS-Version", osVersion)
            .header("X-Client-Timestamp", nowIso())
            .build()
        return chain.proceed(request)
    }

    private fun nowIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
