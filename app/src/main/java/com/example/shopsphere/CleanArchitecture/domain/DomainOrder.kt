package com.example.shopsphere.CleanArchitecture.domain

data class DomainOrder(
    val orderId: Int,
    val totalAmount: Double,
    val date: String,
    val paymentStatus: String,
    val orderStatus: String,
    // Real-time courier location from the backend (null until dispatched)
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    // Courier/driver name assigned by the backend
    val driverName: String? = null
)

data class DomainCheckoutResult(
    val success: Boolean,
    val orderId: Int?,
    val message: String?
)
