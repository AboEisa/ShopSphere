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

// POST /CreateInvoice — requires full invoice details
// Request body with payment method, cart total, customer info, etc.
data class CreateInvoiceRequest(
    @SerializedName("payment_method_id") val paymentMethodId: Int = 0,
    @SerializedName("cartTotal") val cartTotal: String,
    @SerializedName("currency") val currency: String = "EGP",
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("customer") val customer: CustomerInfo,
    @SerializedName("redirectionUrls") val redirectionUrls: RedirectionUrls,
    @SerializedName("cartItems") val cartItems: List<InvoiceCartItem>
)

data class CustomerInfo(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("address") val address: String
)

data class RedirectionUrls(
    @SerializedName("successUrl") val successUrl: String = "https://shopsphere.app/payment/success",
    @SerializedName("failUrl") val failUrl: String = "https://shopsphere.app/payment/fail",
    @SerializedName("pendingUrl") val pendingUrl: String = "https://shopsphere.app/payment/pending"
)

data class InvoiceCartItem(
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: String,
    @SerializedName("quantity") val quantity: String
)

// Response from /CreateInvoice
data class InvoiceResponseDto(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("invoiceId") val invoiceId: String? = null,
    @SerializedName("orderId") val orderId: Int? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("currency") val currency: String? = null,
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
// Element example:
// { "orderId": 1004, "totalAmount": 210.00, "date": "2026-04-21",
//   "paymentStatus": "Pending", "orderStatus": "Processing",
//   "shippingAddress": ",elshorouk,"
//   "products": [{ "productName": "Ped Lipstick", "quantity": 1, "price": 70.00 }] }
// NOTE: shippingAddress can be String OR Object {} depending on backend
data class MyOrderDto(
    @SerializedName("orderId") val orderId: Int = 0,
    @SerializedName("totalAmount") val totalAmount: Double = 0.0,
    @SerializedName("date") val date: String? = null,
    @SerializedName("paymentStatus") val paymentStatus: String? = null,
    @SerializedName("orderStatus") val orderStatus: String? = null,
    @SerializedName("shippingAddress") val shippingAddressRaw: com.google.gson.JsonElement? = null,
    @SerializedName("products") val products: List<OrderProductDto> = emptyList(),
    // Real-time courier location — null until the order is dispatched
    @SerializedName("currentLat") val currentLat: Double? = null,
    @SerializedName("currentLng") val currentLng: Double? = null,
    // Driver/courier name assigned by the backend
    @SerializedName("driverName") val driverName: String? = null
) {
    // Helper to get shippingAddress as String, handling both String and Object cases
    val shippingAddress: String?
        get() = shippingAddressRaw?.let {
            if (it.isJsonPrimitive) {
                it.asString
            } else if (it.isJsonObject) {
                // Try to extract meaningful data from the object
                it.asJsonObject.toString().takeIf { str -> str != "{}" }
            } else {
                it.toString().takeIf { str -> str.isNotBlank() }
            }
        }
}
