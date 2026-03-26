package com.shourov.apps.pacedream.core.network.model

import com.google.gson.Gson
import com.shourov.apps.pacedream.model.response.ErrorResponse
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

suspend fun <T> wrapIntoApiResult(
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    apiCall: suspend () -> T,
): ApiResult<T> {
    return withContext(dispatcher) {
        try {
            ApiResult.Success(apiCall.invoke())
        } catch (throwable: Throwable) {
            timber.log.Timber.e(throwable, "API call failed")
            when (throwable) {
                is IOException -> ApiResult.NetworkError
                else -> {
                    if (throwable is retrofit2.HttpException) {
                        val t = HttpException(throwable)
                        Throwable("Something went wrong. Please try again after sometime.")
                        return@withContext ApiResult.GenericError(t)
                    }
                    if (throwable is JsonDataException) {
                        val error =
                            Throwable("Something went wrong. Please try again after sometime.")

                        return@withContext ApiResult.GenericError(error)
                    }

                    ApiResult.GenericError(throwable)
                }
            }
        }
    }
}

class HttpException(throwable: retrofit2.HttpException) :
    RuntimeException(buildMessage(throwable)) {

    companion object {
        private fun buildMessage(throwable: retrofit2.HttpException): String {
            val body = convertErrorBodyToString(throwable)
            return try {
                val errorResponse = Gson().fromJson(body, ErrorResponse::class.java)
                errorResponse.message.error
            } catch (e: Exception) {
                throwable.message()
            }
        }

        private fun convertErrorBodyToString(throwable: retrofit2.HttpException): String? {
            return try {
                throwable.response()
                    ?.errorBody()
                    ?.string()
            } catch (_: Throwable) {
                null
            }
        }
    }
}
