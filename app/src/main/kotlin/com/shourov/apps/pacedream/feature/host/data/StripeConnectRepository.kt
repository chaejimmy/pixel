package com.shourov.apps.pacedream.feature.host.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
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

    // ── Earnings Dashboard (iOS parity: single all-in-one endpoint) ──

    /**
     * Fetch the all-in-one earnings dashboard from /host/earnings/dashboard.
     * This is the same endpoint iOS uses (PayoutsService.fetchDashboard).
     *
     * Parses the raw JSON manually (like iOS) to handle:
     * - Backend responses that may or may not have a `data` envelope
     * - snake_case vs camelCase field names
     * - Null/missing arrays (Gson bypasses Kotlin defaults via Unsafe)
     */
    suspend fun getEarningsDashboard(): Result<EarningsDashboardResponse> {
        Timber.d("[Earnings] Fetching dashboard from /host/earnings/dashboard")
        return try {
            val response = hostApiService.getEarningsDashboard()
            val code = response.code()
            Timber.d("[Earnings] Dashboard response: status=$code")
            if (response.isSuccessful) {
                val rawJson = response.body()
                if (rawJson == null || !rawJson.isJsonObject) {
                    Timber.w("[Earnings] Dashboard response body is null or not an object, raw=$rawJson")
                    return Result.success(EarningsDashboardResponse())
                }

                val root = rawJson.asJsonObject
                Timber.d("[Earnings] Raw response keys: ${root.keySet()}")

                // Unwrap data envelope if present (some backends wrap like { data: { ... } })
                val json: JsonObject = if (root.has("stripe") || root.has("balances")) {
                    root
                } else if (root.has("data") && root.get("data").isJsonObject) {
                    Timber.d("[Earnings] Unwrapping data envelope")
                    root.getAsJsonObject("data")
                } else {
                    Timber.d("[Earnings] No stripe/balances/data keys found, using root")
                    root
                }

                val dashboard = parseDashboard(json)
                Timber.d(
                    "[Earnings] Dashboard loaded: connected=${dashboard.stripe?.connected}, " +
                        "payoutsEnabled=${dashboard.stripe?.payoutsEnabled}, " +
                        "available=${dashboard.balances?.available}, " +
                        "pending=${dashboard.balances?.pending}, " +
                        "settling=${dashboard.balances?.settling}, " +
                        "lifetime=${dashboard.balances?.lifetime}, " +
                        "payouts=${dashboard.payouts.size}, " +
                        "transactions=${dashboard.transactions.size}"
                )
                Result.success(dashboard)
            } else {
                val errorMsg = if (code == 401) {
                    "401 Unauthorized"
                } else {
                    val serverMsg = extractErrorMessage(response, "")
                    val detail = if (serverMsg.isNotBlank()) serverMsg else "HTTP $code"
                    Timber.e("[Earnings] Dashboard failed: $code - $detail")
                    detail
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.e(e, "[Earnings] Dashboard request exception: ${e.javaClass.simpleName}: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Manual JSON parsing (iOS PayoutsService.fetchDashboard parity) ──

    private fun parseDashboard(json: JsonObject): EarningsDashboardResponse {
        val stripeObj = json.obj("stripe")
        val balObj = json.obj("balances")
        val payoutsArr = json.arr("payouts")
        val txArr = json.arr("transactions")
        val statsObj = json.obj("stats")
        val rulesObj = json.obj("payoutRules") ?: json.obj("payout_rules")

        return EarningsDashboardResponse(
            success = json.bool("success"),
            stripe = stripeObj?.let { parseStripeStatus(it) },
            balances = balObj?.let { parseBalances(it) },
            payouts = payoutsArr?.map { parsePayout(it.asJsonObject) } ?: emptyList(),
            transactions = txArr?.map { parseTransaction(it.asJsonObject) } ?: emptyList(),
            stats = statsObj?.let { parseStats(it) },
            payoutRules = rulesObj?.let { parsePayoutRules(it) }
        )
    }

    private fun parseStripeStatus(j: JsonObject) = DashboardStripeStatus(
        connected = j.bool("connected"),
        accountId = j.str("accountId") ?: j.str("account_id"),
        chargesEnabled = j.bool("chargesEnabled") || j.bool("charges_enabled"),
        payoutsEnabled = j.bool("payoutsEnabled") || j.bool("payouts_enabled"),
        detailsSubmitted = j.bool("detailsSubmitted") || j.bool("details_submitted"),
        onboardingComplete = j.bool("onboardingComplete") || j.bool("onboarding_complete"),
        disabledReason = j.str("disabledReason") ?: j.str("disabled_reason"),
        requirements = j.strList("requirements")
    )

    private fun parseBalances(j: JsonObject) = DashboardBalances(
        available = j.dbl("available"),
        pending = j.dbl("pending"),
        settling = j.dbl("settling"),
        readyForTransfer = j.dbl("readyForTransfer").let { if (it > 0) it else j.dbl("ready_for_transfer") },
        lifetime = j.dbl("lifetime"),
        currency = j.str("currency") ?: "usd",
        fundsSettling = j.bool("fundsSettling") || j.bool("funds_settling"),
        settlingNote = j.str("settlingNote") ?: j.str("settling_note")
    )

    private fun parsePayout(j: JsonObject) = DashboardPayout(
        id = j.str("id") ?: "",
        amount = j.dbl("amount"),
        currency = j.str("currency") ?: "usd",
        status = j.str("status") ?: "unknown",
        method = j.str("method"),
        arrivalDate = j.str("arrivalDate") ?: j.str("arrival_date"),
        createdAt = j.str("createdAt") ?: j.str("created_at"),
        description = j.str("description"),
        destination = j.obj("destination")?.let { d ->
            DashboardPayoutDestination(
                last4 = d.str("last4") ?: d.str("last_4"),
                bankName = d.str("bankName") ?: d.str("bank_name")
            )
        }
    )

    private fun parseTransaction(j: JsonObject) = DashboardTransaction(
        id = j.str("id") ?: "",
        bookingId = j.str("bookingId") ?: j.str("booking_id"),
        bookingType = j.str("bookingType") ?: j.str("booking_type"),
        amount = j.dbl("amount"),
        grossAmount = j.dbl("grossAmount").let { if (it > 0) it else j.dbl("gross_amount") },
        stripeProcessingFee = j.dbl("stripeProcessingFee").let { if (it > 0) it else j.dbl("stripe_processing_fee") },
        netAmount = j.dbl("netAmount").let { if (it > 0) it else j.dbl("net_amount") },
        currency = j.str("currency") ?: "usd",
        status = j.str("status"),
        payoutStatus = j.str("payoutStatus") ?: j.str("payout_status"),
        platformFee = j.dbl("platformFee").let { if (it > 0) it else j.dbl("platform_fee") },
        releaseRule = j.str("releaseRule") ?: j.str("release_rule"),
        payoutReleaseAt = j.str("payoutReleaseAt") ?: j.str("payout_release_at"),
        stripeTransferId = j.str("stripeTransferId") ?: j.str("stripe_transfer_id"),
        blockedReason = j.str("blockedReason") ?: j.str("blocked_reason"),
        createdAt = j.str("createdAt") ?: j.str("created_at"),
        description = j.str("description")
    )

    private fun parseStats(j: JsonObject) = DashboardStats(
        totalTransactions = j.int("totalTransactions").let { if (it > 0) it else j.int("total_transactions") },
        completedPayouts = j.int("completedPayouts").let { if (it > 0) it else j.int("completed_payouts") },
        completedAmount = j.dbl("completedAmount").let { if (it > 0) it else j.dbl("completed_amount") },
        heldPayouts = j.int("heldPayouts").let { if (it > 0) it else j.int("held_payouts") },
        heldAmount = j.dbl("heldAmount").let { if (it > 0) it else j.dbl("held_amount") },
        settlingPayouts = j.int("settlingPayouts").let { if (it > 0) it else j.int("settling_payouts") },
        settlingAmount = j.dbl("settlingAmount").let { if (it > 0) it else j.dbl("settling_amount") },
        readyPayouts = j.int("readyPayouts").let { if (it > 0) it else j.int("ready_payouts") },
        readyAmount = j.dbl("readyAmount").let { if (it > 0) it else j.dbl("ready_amount") },
        blockedPayouts = j.int("blockedPayouts").let { if (it > 0) it else j.int("blocked_payouts") },
        blockedAmount = j.dbl("blockedAmount").let { if (it > 0) it else j.dbl("blocked_amount") }
    )

    private fun parsePayoutRules(j: JsonObject) = DashboardPayoutRules(
        shortBookingThresholdHours = j.int("shortBookingThresholdHours").let {
            if (it > 0) it else j.int("short_booking_threshold_hours").let { v -> if (v > 0) v else 24 }
        },
        shortBookingRule = j.str("shortBookingRule") ?: j.str("short_booking_rule") ?: "",
        longBookingRule = j.str("longBookingRule") ?: j.str("long_booking_rule") ?: ""
    )

    // ── JsonObject extension helpers for safe field access ──

    private fun JsonObject.str(key: String): String? =
        if (has(key) && get(key).isJsonPrimitive) get(key).asJsonPrimitive.let {
            if (it.isString) it.asString else try { it.asString } catch (_: Exception) { null }
        } else null

    private fun JsonObject.bool(key: String): Boolean =
        try { if (has(key)) get(key).asBoolean else false } catch (_: Exception) { false }

    private fun JsonObject.dbl(key: String): Double =
        try { if (has(key)) get(key).asDouble else 0.0 } catch (_: Exception) { 0.0 }

    private fun JsonObject.int(key: String): Int =
        try { if (has(key)) get(key).asInt else 0 } catch (_: Exception) { 0 }

    private fun JsonObject.obj(key: String): JsonObject? =
        if (has(key) && get(key).isJsonObject) getAsJsonObject(key) else null

    private fun JsonObject.arr(key: String): List<JsonElement>? =
        if (has(key) && get(key).isJsonArray) getAsJsonArray(key).toList() else null

    private fun JsonObject.strList(key: String): List<String> =
        arr(key)?.mapNotNull { try { it.asString } catch (_: Exception) { null } } ?: emptyList()

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
            Timber.e(e, "Create connect account failed")
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
            Timber.e(e, "Create onboarding link failed")
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
            Timber.e(e, "Create login link failed")
            Result.failure(e)
        }
    }

    // Note: Balance, transfers, and payouts are now fetched via getEarningsDashboard()
    // which uses the single /host/earnings/dashboard endpoint (iOS parity).

    /**
     * Request a manual payout.
     *
     * [idempotencyKey] should be minted ONCE per user "Withdraw" tap and
     * reused if the call is retried (e.g. after a transient network
     * failure) so the backend / Stripe Connect dedup the request and we
     * never create two payouts for the same intent.  Callers that don't
     * pass one get a UUID per call — safe but loses retry dedup.
     */
    suspend fun createPayout(
        amount: Int,
        currency: String = "usd",
        idempotencyKey: String = java.util.UUID.randomUUID().toString(),
    ): Result<Payout> {
        return try {
            Timber.d("Requesting payout: ${amount}c $currency key=$idempotencyKey")
            val response = hostApiService.createPayout(
                request = CreatePayoutRequest(amount, currency),
                idempotencyKey = idempotencyKey,
            )
            if (response.isSuccessful) {
                Timber.d("Payout request succeeded")
                Result.success(response.body() ?: throw Exception("Empty response"))
            } else {
                val msg = extractErrorMessage(response, "Failed to request payout")
                Timber.w("Payout request failed [${response.code()}]: $msg key=$idempotencyKey")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Payout request exception key=$idempotencyKey")
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
