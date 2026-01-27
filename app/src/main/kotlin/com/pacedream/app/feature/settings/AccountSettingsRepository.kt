package com.pacedream.app.feature.settings

import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountSettingsRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {

    // region Personal Info

    @Serializable
    data class AccountProfile(
        @SerialName("firstName") val firstName: String? = null,
        @SerialName("lastName") val lastName: String? = null,
        @SerialName("email") val email: String? = null,
        @SerialName("phoneNumber") val phoneNumber: String? = null,
        @SerialName("profileImage") val profileImage: String? = null
    )

    @Serializable
    data class Envelope<T>(
        val status: Boolean? = null,
        val success: Boolean? = null,
        val code: Int? = null,
        val data: T? = null,
        val message: String? = null,
        val error: String? = null
    ) {
        val isOk: Boolean
            get() = (status == true) || (success == true)
    }

    suspend fun getAccount(): ApiResult<AccountProfile> {
        val url = appConfig.buildFrontendUrl("api", "proxy", "account", "me")
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val envelope = json.decodeFromString(Envelope.serializer(AccountProfile.serializer()), result.data)
                    if (envelope.isOk && envelope.data != null) {
                        ApiResult.Success(envelope.data)
                    } else {
                        ApiResult.Failure(
                            ApiError.ServerError(
                                envelope.code ?: 500,
                                envelope.error ?: envelope.message ?: "Unable to load account."
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse /account/me response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun updateProfile(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String?
    ): ApiResult<AccountProfile> {
        val url = appConfig.buildFrontendUrl("api", "proxy", "account", "profile")
        val body = json.encodeToString(
            AccountProfile.serializer(),
            AccountProfile(firstName, lastName, email, phoneNumber)
        )
        return when (val result = apiClient.put(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val envelope = json.decodeFromString(Envelope.serializer(AccountProfile.serializer()), result.data)
                    if (envelope.isOk && envelope.data != null) {
                        ApiResult.Success(envelope.data)
                    } else {
                        ApiResult.Failure(
                            ApiError.ServerError(
                                envelope.code ?: 500,
                                envelope.error ?: envelope.message ?: "Unable to update profile."
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse /account/profile response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    // endregion

    // region Login & Security

    @Serializable
    data class ChangePasswordRequest(
        val currentPassword: String,
        val newPassword: String
    )

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): ApiResult<Unit> {
        val url = appConfig.buildFrontendUrl("api", "proxy", "account", "password")
        val body = json.encodeToString(ChangePasswordRequest.serializer(), ChangePasswordRequest(currentPassword, newPassword))
        return when (val result = apiClient.put(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                // envelope may be generic; just check status/success if present
                try {
                    val envelope = json.decodeFromString(Envelope.serializer(UnitSerializer), result.data)
                    if (envelope.isOk) {
                        ApiResult.Success(Unit)
                    } else {
                        ApiResult.Failure(
                            ApiError.ServerError(
                                envelope.code ?: 400,
                                envelope.error ?: envelope.message ?: "Unable to update password."
                            )
                        )
                    }
                } catch (_: Exception) {
                    // Fallback if body is not envelope; treat 2xx as success
                    ApiResult.Success(Unit)
                }
            }
            is ApiResult.Failure -> result
        }
    }

    object UnitSerializer : kotlinx.serialization.KSerializer<Unit> {
        override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("Unit") {}
        override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder) = Unit
        override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Unit) = Unit
    }

    suspend fun deactivateAccount(): ApiResult<Unit> {
        val url = appConfig.buildFrontendUrl("api", "proxy", "account", "deactivate")
        val body = "{}"
        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }
    }

    // endregion

    // region Notifications

    @Serializable
    data class NotificationSettings(
        val emailGeneral: Boolean = false,
        val pushGeneral: Boolean = false,
        val messageNotifications: Boolean = false,
        val bookingUpdates: Boolean = false,
        val bookingAlerts: Boolean = false,
        val marketingPromotions: Boolean = false
    )

    suspend fun getNotificationSettings(): ApiResult<NotificationSettings> {
        val url = appConfig.buildFrontendUrl("api", "proxy", "account", "notifications")
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val envelope = json.decodeFromString(Envelope.serializer(NotificationSettings.serializer()), result.data)
                    if (envelope.isOk && envelope.data != null) {
                        ApiResult.Success(envelope.data)
                    } else {
                        ApiResult.Failure(
                            ApiError.ServerError(
                                envelope.code ?: 500,
                                envelope.error ?: envelope.message ?: "Unable to load notification settings."
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse notification settings response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun updateNotificationSettings(settings: NotificationSettings): ApiResult<NotificationSettings> {
        val url = appConfig.buildFrontendUrl("api", "proxy", "account", "notifications")
        val body = json.encodeToString(NotificationSettings.serializer(), settings)
        return when (val result = apiClient.put(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val envelope = json.decodeFromString(Envelope.serializer(NotificationSettings.serializer()), result.data)
                    if (envelope.isOk && envelope.data != null) {
                        ApiResult.Success(envelope.data)
                    } else {
                        ApiResult.Failure(
                            ApiError.ServerError(
                                envelope.code ?: 500,
                                envelope.error ?: envelope.message ?: "Unable to update notification settings."
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse notification settings update response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    // endregion

    // region Preferences

    @Serializable
    data class Preferences(
        val language: String? = null,
        val currency: String? = null,
        val timezone: String? = null
    )

    suspend fun getPreferences(): ApiResult<Preferences> {
        val url = appConfig.buildFrontendUrl("api", "proxy", "account", "preferences")
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val envelope = json.decodeFromString(Envelope.serializer(Preferences.serializer()), result.data)
                    if (envelope.isOk && envelope.data != null) {
                        ApiResult.Success(envelope.data)
                    } else {
                        ApiResult.Failure(
                            ApiError.ServerError(
                                envelope.code ?: 500,
                                envelope.error ?: envelope.message ?: "Unable to load preferences."
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse preferences response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun updatePreferences(preferences: Preferences): ApiResult<Preferences> {
        val url = appConfig.buildFrontendUrl("api", "proxy", "account", "preferences")
        val body = json.encodeToString(Preferences.serializer(), preferences)
        return when (val result = apiClient.put(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val envelope = json.decodeFromString(Envelope.serializer(Preferences.serializer()), result.data)
                    if (envelope.isOk && envelope.data != null) {
                        ApiResult.Success(envelope.data)
                    } else {
                        ApiResult.Failure(
                            ApiError.ServerError(
                                envelope.code ?: 500,
                                envelope.error ?: envelope.message ?: "Unable to update preferences."
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse preferences update response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    // endregion
}

