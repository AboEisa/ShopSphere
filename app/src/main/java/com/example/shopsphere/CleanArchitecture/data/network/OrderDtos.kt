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

// GET /MyOrders — returns an array of orders for the authenticated user.
// Element example:
// { "orderId": 1004, "totalAmount": 210.00, "date": "2026-04-21",
//   "paymentStatus": "Pending", "orderStatus": "Processing" }
data class MyOrderDto(
    @SerializedName("orderId") val orderId: Int = 0,
    @SerializedName("totalAmount") val totalAmount: Double = 0.0,
    @SerializedName("date") val date: String? = null,
    @SerializedName("paymentStatus") val paymentStatus: String? = null,
    @SerializedName("orderStatus") val orderStatus: String? = null
)
