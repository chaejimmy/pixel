package com.shourov.apps.pacedream.feature.host.data

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StripeConnectRepository @Inject constructor(
    private val hostApiService: HostApiService
) {

    /**
     * iOS PR #206 parity: extract user-facing error message from error response body.
     * Shows server error messages for ALL status codes (not just 4xx).
     */
    private fun extractErrorMessage(response: retrofit2.Response<*>, fallback: String): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                // Try to extract message from JSON error body
                val json = org.json.JSONObject(errorBody)
                json.optString("message", null)
                    ?: json.optString("error", null)
                    ?: json.optJSONObject("error")?.optString("message", null)
                    ?: fallback
            } else {
                fallback
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not parse error body, using fallback")
            fallback
        }
    }

    // Connect Account
    suspend fun getConnectAccountStatus(): Result<ConnectAccount> {
        return try {
            val response = hostApiService.getConnectAccountStatus()
            if (response.isSuccessful) {
                Result.success(response.body() ?: ConnectAccount())
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Failed to fetch connect account")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createConnectAccount(email: String, country: String = "US"): Result<ConnectAccount> {
        return try {
            val response = hostApiService.createConnectAccount(
                CreateConnectAccountRequest(email, country)
            )
            if (response.isSuccessful) {
                Result.success(response.body() ?: ConnectAccount())
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Failed to create connect account")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Onboarding & Dashboard Links
    suspend fun createOnboardingLink(platform: String = "android"): Result<PayoutLinkResponse> {
        return try {
            val response = hostApiService.createOnboardingLink(platform)
            if (response.isSuccessful) {
                Result.success(response.body() ?: throw Exception("Empty response"))
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Couldn't start Stripe setup")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createLoginLink(): Result<PayoutLinkResponse> {
        return try {
            val response = hostApiService.createLoginLink()
            if (response.isSuccessful) {
                Result.success(response.body() ?: throw Exception("Empty response"))
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Couldn't open Stripe dashboard")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Balance
    suspend fun getBalance(): Result<ConnectBalance> {
        return try {
            val response = hostApiService.getStripeBalance()
            if (response.isSuccessful) {
                Result.success(response.body() ?: ConnectBalance())
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Failed to fetch balance")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Transfers
    suspend fun getTransfers(limit: Int = 20): Result<List<Transfer>> {
        return try {
            val response = hostApiService.getStripeTransfers(limit)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Failed to fetch transfers")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Payouts
    suspend fun getPayouts(limit: Int = 20): Result<List<Payout>> {
        return try {
            val response = hostApiService.getStripePayouts(limit)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Failed to fetch payouts")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPayout(amount: Int, currency: String = "usd"): Result<Payout> {
        return try {
            val response = hostApiService.createPayout(CreatePayoutRequest(amount, currency))
            if (response.isSuccessful) {
                Result.success(response.body() ?: throw Exception("Empty response"))
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Failed to request payout")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Payout Methods
    suspend fun getPayoutMethods(): Result<List<PayoutMethod>> {
        return try {
            val response = hostApiService.getPayoutMethods()
            if (response.isSuccessful) {
                val methods = response.body()?.resolvedMethods?.map { m ->
                    PayoutMethod(
                        id = m.resolvedId,
                        type = m.resolvedType,
                        label = m.resolvedLabel,
                        isPrimary = m.resolvedIsPrimary
                    )
                } ?: emptyList()
                Result.success(methods)
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Failed to fetch payout methods")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Payout Status
    suspend fun getPayoutStatus(): Result<PayoutStatusResponse> {
        return try {
            val response = hostApiService.getPayoutStatus()
            if (response.isSuccessful) {
                Result.success(response.body() ?: PayoutStatusResponse())
            } else {
                Result.failure(Exception(extractErrorMessage(response, "Failed to fetch payout status")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
