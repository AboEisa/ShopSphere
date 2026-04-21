package com.example.shopsphere.CleanArchitecture.domain

data class DomainOrder(
    val orderId: Int,
    val totalAmount: Double,
    val date: String,
    val paymentStatus: String,
    val orderStatus: String
)

data class DomainCheckoutResult(
    val success: Boolean,
    val orderId: Int?,
    val message: String?
)
