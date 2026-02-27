package com.shourov.apps.pacedream.core.network.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * Interceptor that detects 401 responses on the Retrofit code path.
 *
 * The primary ApiClient (OkHttp-based) already handles 401→refresh→retry
 * in its own executeRequest(). This interceptor is only relevant for the
 * legacy Retrofit service layer. It logs the event so that token refresh
 * failures surface in Crashlytics / Timber logs rather than silently failing.
 */
class TokenExpiredInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            Timber.w(
                "TokenExpiredInterceptor: 401 on %s – token may need refresh",
                chain.request().url.encodedPath
            )
        }
        return response
    }
}
