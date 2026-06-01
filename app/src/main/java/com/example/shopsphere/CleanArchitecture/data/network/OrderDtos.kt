package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

// POST /ChecKout — no request body (server reads current cart via Bearer token).
// Response example:
// { "success": true, "orderId": 1004, "message": "Order created successfully from your cart!" }
data class CheckoutResponseDto(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("orderId") val orderId: Int? = null,
    @SerializedName("message") val message: String? = null
)

// POST /PayNow — requires orderId in request body
data class PayNowRequest(
    @SerializedName("orderId") val orderId: Int
)

// Response from /PayNow
data class PayNowResponseDto(
    @SerializedName("url") val url: String? = null,
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("paymentUrl") val paymentUrl: String? = null,
    @SerializedName("paymentToken") val paymentToken: String? = null,
    @SerializedName("orderId") val orderId: Int? = null,
    @SerializedName("message") val message: String? = null
)

// POST /Callbackt — payment-provider webhook acknowledged by the server.
// Request body to update payment status (invoiceStatus: "paid" or "failed")
data class PaymentCallbackRequest(
    @SerializedName("invoice_status") val invoiceStatus: String = "paid",
    @SerializedName("OrderId") val orderId: String
)

// Response from /Callbackt
data class PaymentCallbackDto(
    @SerializedName("message") val message: String? = null
)

// Product item in order response
data class OrderProductDto(
    @SerializedName("productName") val productName: String = "",
    @SerializedName("quantity") val quantity: Int = 0,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("productImage") val productImage: String? = null
)

// GET /MyOrders — returns an array of orders for the authenticated user.
data class MyOrderDto(
    @SerializedName("orderId") val orderId: Int = 0,
    @SerializedName("totalAmount") val totalAmount: Double = 0.0,
    @SerializedName("date") val date: String? = null,
    @SerializedName("paymentStatus") val paymentStatus: String? = null,
    @SerializedName("orderStatus") val orderStatus: String? = null,
    @SerializedName("shippingAddress") val shippingAddressRaw: com.google.gson.JsonElement? = null,
    @SerializedName("products") val products: List<OrderProductDto> = emptyList(),
    @SerializedName("currentLat") val currentLat: Double? = null,
    @SerializedName("currentLng") val currentLng: Double? = null,
    @SerializedName("driverName") val driverName: String? = null
) {
    val shippingAddress: String?
        get() = shippingAddressRaw?.let {
            if (it.isJsonPrimitive) {
                it.asString
            } else if (it.isJsonObject) {
                it.asJsonObject.toString().takeIf { str -> str != "{}" }
            } else {
                it.toString().takeIf { str -> str.isNotBlank() }
            }
        }
}
