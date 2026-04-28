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

// POST /CreateInvoice — body-less; server builds an invoice from the latest
// order/cart of the authenticated user.
data class InvoiceResponseDto(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("invoiceId") val invoiceId: String? = null,
    @SerializedName("orderId") val orderId: Int? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("message") val message: String? = null
)

// POST /PayNow — body-less; server returns the URL/token the client opens to
// complete payment (e.g. a Paymob/Stripe iframe URL).
data class PayNowResponseDto(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("paymentUrl") val paymentUrl: String? = null,
    @SerializedName("paymentToken") val paymentToken: String? = null,
    @SerializedName("orderId") val orderId: Int? = null,
    @SerializedName("message") val message: String? = null
)

// POST /Callbackt — payment-provider webhook acknowledged by the server.
// Exposed from the app mainly for testing / manual reconciliation.
data class PaymentCallbackDto(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("orderId") val orderId: Int? = null,
    @SerializedName("paymentStatus") val paymentStatus: String? = null,
    @SerializedName("message") val message: String? = null
)

// GET /MyOrders — returns an array of orders for the authenticated user.
// Element example:
// { "orderId": 1004, "totalAmount": 210.00, "date": "2026-04-21",
//   "paymentStatus": "Pending", "orderStatus": "Processing",
//   "currentLat": 30.06, "currentLng": 31.22, "driverName": "Ahmed Hassan" }
data class MyOrderDto(
    @SerializedName("orderId") val orderId: Int = 0,
    @SerializedName("totalAmount") val totalAmount: Double = 0.0,
    @SerializedName("date") val date: String? = null,
    @SerializedName("paymentStatus") val paymentStatus: String? = null,
    @SerializedName("orderStatus") val orderStatus: String? = null,
    // Real-time courier location — null until the order is dispatched
    @SerializedName("currentLat") val currentLat: Double? = null,
    @SerializedName("currentLng") val currentLng: Double? = null,
    // Driver/courier name assigned by the backend
    @SerializedName("driverName") val driverName: String? = null
)
