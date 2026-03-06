package com.shourov.apps.pacedream.feature.host.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StripeConnectRepository @Inject constructor(
    private val hostApiService: HostApiService
) {

    // Connect Account
    suspend fun getConnectAccountStatus(): Result<ConnectAccount> {
        return try {
            val response = hostApiService.getConnectAccountStatus()
            if (response.isSuccessful) {
                Result.success(response.body() ?: ConnectAccount())
            } else {
                Result.failure(Exception("Failed to fetch connect account: ${response.code()}"))
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
                Result.failure(Exception("Failed to create connect account: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Onboarding & Dashboard Links
    suspend fun createOnboardingLink(): Result<PayoutLinkResponse> {
        return try {
            val response = hostApiService.createOnboardingLink()
            if (response.isSuccessful) {
                Result.success(response.body() ?: throw Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to create onboarding link: ${response.code()}"))
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
                Result.failure(Exception("Failed to create login link: ${response.code()}"))
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
                Result.failure(Exception("Failed to fetch balance: ${response.code()}"))
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
                Result.failure(Exception("Failed to fetch transfers: ${response.code()}"))
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
                Result.failure(Exception("Failed to fetch payouts: ${response.code()}"))
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
                Result.failure(Exception("Failed to create payout: ${response.code()}"))
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
                Result.failure(Exception("Failed to fetch payout methods: ${response.code()}"))
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
                Result.failure(Exception("Failed to fetch payout status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
