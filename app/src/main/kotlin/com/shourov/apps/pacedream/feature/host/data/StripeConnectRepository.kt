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

    // ── Earnings Dashboard (iOS parity: PayoutsService.fetchDashboard) ──

    /**
     * Fetch comprehensive earnings dashboard from /host/earnings/dashboard.
     * This matches the iOS PayoutsService.fetchDashboard() endpoint which returns
     * balances, transactions, stats, and payout rules in a single call.
     */
    suspend fun getEarningsDashboard(): Result<EarningsDashboardResponse> {
        return try {
            Timber.d("Fetching earnings dashboard from /host/earnings/dashboard")
            val response = hostApiService.getEarningsDashboard()
            if (response.isSuccessful) {
                val body = response.body() ?: EarningsDashboardResponse()
                Timber.d("Earnings dashboard loaded: ${body.transactions.size} transactions, ${body.payouts.size} payouts")
                Result.success(body)
            } else {
                val msg = extractErrorMessage(response, "Failed to fetch earnings dashboard")
                Timber.w("Earnings dashboard request failed [${response.code()}]: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Earnings dashboard fetch exception")
            Result.failure(e)
        }
    }

    // Connect Account
    suspend fun getConnectAccountStatus(): Result<ConnectAccount> {
        return try {
            Timber.d("Fetching connect account status from /host/stripe/connect/status")
            val response = hostApiService.getConnectAccountStatus()
            if (response.isSuccessful) {
                Result.success(response.body() ?: ConnectAccount())
            } else {
                val msg = extractErrorMessage(response, "Failed to fetch connect account")
                Timber.w("Connect account status failed [${response.code()}]: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Connect account status fetch exception")
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
            Timber.d("Fetching Stripe balance from /host/stripe/balance")
            val response = hostApiService.getStripeBalance()
            if (response.isSuccessful) {
                Result.success(response.body() ?: ConnectBalance())
            } else {
                val msg = extractErrorMessage(response, "Failed to fetch balance")
                Timber.w("Stripe balance failed [${response.code()}]: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Stripe balance fetch exception")
            Result.failure(e)
        }
    }

    // Transfers
    suspend fun getTransfers(limit: Int = 20): Result<List<Transfer>> {
        return try {
            Timber.d("Fetching Stripe transfers from /host/stripe/transfers")
            val response = hostApiService.getStripeTransfers(limit)
            if (response.isSuccessful) {
                val transfers = response.body() ?: emptyList()
                Timber.d("Loaded ${transfers.size} transfers")
                Result.success(transfers)
            } else {
                val msg = extractErrorMessage(response, "Failed to fetch transfers")
                Timber.w("Stripe transfers failed [${response.code()}]: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Stripe transfers fetch exception")
            Result.failure(e)
        }
    }

    // Payouts
    suspend fun getPayouts(limit: Int = 20): Result<List<Payout>> {
        return try {
            Timber.d("Fetching Stripe payouts from /host/stripe/payouts")
            val response = hostApiService.getStripePayouts(limit)
            if (response.isSuccessful) {
                val payouts = response.body() ?: emptyList()
                Timber.d("Loaded ${payouts.size} payouts")
                Result.success(payouts)
            } else {
                val msg = extractErrorMessage(response, "Failed to fetch payouts")
                Timber.w("Stripe payouts failed [${response.code()}]: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Stripe payouts fetch exception")
            Result.failure(e)
        }
    }

    suspend fun createPayout(amount: Int, currency: String = "usd"): Result<Payout> {
        return try {
            Timber.d("Requesting payout: ${amount}c $currency")
            val response = hostApiService.createPayout(CreatePayoutRequest(amount, currency))
            if (response.isSuccessful) {
                Timber.d("Payout request succeeded")
                Result.success(response.body() ?: throw Exception("Empty response"))
            } else {
                val msg = extractErrorMessage(response, "Failed to request payout")
                Timber.w("Payout request failed [${response.code()}]: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Payout request exception")
            Result.failure(e)
        }
    }

    // Payout Methods
    suspend fun getPayoutMethods(): Result<List<PayoutMethod>> {
        return try {
            Timber.d("Fetching payout methods from /host/payouts/methods")
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
                Timber.d("Loaded ${methods.size} payout methods")
                Result.success(methods)
            } else {
                val msg = extractErrorMessage(response, "Failed to fetch payout methods")
                Timber.w("Payout methods failed [${response.code()}]: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Payout methods fetch exception")
            Result.failure(e)
        }
    }

    // Payout Status
    suspend fun getPayoutStatus(): Result<PayoutStatusResponse> {
        return try {
            Timber.d("Fetching payout status from /host/payouts/status")
            val response = hostApiService.getPayoutStatus()
            if (response.isSuccessful) {
                Result.success(response.body() ?: PayoutStatusResponse())
            } else {
                val msg = extractErrorMessage(response, "Failed to fetch payout status")
                Timber.w("Payout status failed [${response.code()}]: $msg")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Payout status fetch exception")
            Result.failure(e)
        }
    }
}
