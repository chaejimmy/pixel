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

package com.shourov.apps.pacedream.core.data.util

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build.VERSION_CODES
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays

/**
 * DO NOT USE THIS IN PRODUCTION
 *
 * This class is only for development purpose. For production app, use keytool to generate the hash:
 * https://developers.google.com/identity/sms-retriever/verify#computing_your_apps_hash_string
 *
 * This is a helper class to generate your message hash to be included in your SMS message.
 * Once the hash has been generated this class can be deleted.
 *
 * Without the correct hash, your app won't receive the message callback. This only needs to be
 * generated once per app and stored. Then you can remove this helper class from your code.
 */
class AppSignatureHelper(context: Context?) : ContextWrapper(context) {
    val appSignatures: ArrayList<String>
        /**
         * Get all the app signatures for the current package
         * @return
         */
        @RequiresApi(VERSION_CODES.P)
        get() {
            val appCodes = ArrayList<String>()
            try {
                // Get all package signatures for the current package
                val packageName = packageName
                val packageManager = packageManager
                val packageInfo: PackageInfo =
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES,
                    )

                val signingInfo = packageInfo.signingInfo
                val signatures: Array<out Signature> = if (signingInfo != null) {
                    // New method (API level 28 and above)
                    if (signingInfo.hasMultipleSigners()) {
                        // Handle multiple signers if necessary
                        signingInfo.apkContentsSigners
                    } else {
                        // Single signer
                        signingInfo.signingCertificateHistory
                    }
                } else {
                    // Old method (deprecated)
                    @Suppress("DEPRECATION")
                    packageInfo.signatures ?: emptyArray()
                }

                // For each signature create a compatible hash
                for (signature in signatures) {
                    val hash = hash(packageName, signature.toCharsString())
                    if (hash != null) {
                        appCodes.add(String.format("%s", hash))
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Unable to find package to obtain hash.", e)
            }
            return appCodes
        }

    companion object {
        val TAG = AppSignatureHelper::class.java.simpleName
        private const val HASH_TYPE = "SHA-256"
        private const val NUM_HASHED_BYTES = 9
        private const val NUM_BASE64_CHAR = 11
        private fun hash(packageName: String, signature: String): String? {
            val appInfo = "$packageName $signature"
            try {
                val messageDigest = MessageDigest.getInstance(HASH_TYPE)
                messageDigest.update(appInfo.toByteArray(StandardCharsets.UTF_8))
                var hashSignature = messageDigest.digest()

                // truncated into NUM_HASHED_BYTES
                hashSignature = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES)
                // encode into Base64
                var base64Hash: String =
                    Base64.encodeToString(hashSignature, Base64.NO_PADDING or Base64.NO_WRAP)
                base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR)
                if (com.shourov.apps.pacedream.core.data.BuildConfig.DEBUG) {
                    Log.d(TAG, String.format("pkg: %s -- hash: %s", packageName, base64Hash))
                }
                return base64Hash
            } catch (e: NoSuchAlgorithmException) {
                Log.e(TAG, "hash:NoSuchAlgorithm", e)
            }
            return null
        }
    }
}