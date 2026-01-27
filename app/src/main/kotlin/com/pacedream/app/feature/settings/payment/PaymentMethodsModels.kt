package com.pacedream.app.feature.settings.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethod(
    val id: String,
    val brand: String,
    val last4: String,
    @SerialName("expMonth")
    val expMonth: Int? = null,
    @SerialName("expYear")
    val expYear: Int? = null,
    @SerialName("isDefault")
    val isDefault: Boolean = false
)

@Serializable
data class PaymentMethodsData(
    val paymentMethods: List<PaymentMethod> = emptyList()
)

@Serializable
data class PaymentMethodsResponse(
    val status: Boolean? = null,
    val success: Boolean? = null,
    val code: Int? = null,
    val data: PaymentMethodsData? = null,
    val message: String? = null,
    val error: String? = null
) {
    val isOk: Boolean
        get() = (status == true) || (success == true)
}

@Serializable
data class SetupIntentData(
    val clientSecret: String
)

@Serializable
data class SetupIntentResponse(
    val status: Boolean? = null,
    val success: Boolean? = null,
    val code: Int? = null,
    val data: SetupIntentData? = null,
    val message: String? = null,
    val error: String? = null
) {
    val isOk: Boolean
        get() = (status == true) || (success == true)
}

