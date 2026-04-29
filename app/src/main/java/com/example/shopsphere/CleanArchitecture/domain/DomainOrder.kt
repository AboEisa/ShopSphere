package com.example.shopsphere.CleanArchitecture.domain

// Product item in an order
data class DomainOrderProduct(
    val productName: String,
    val quantity: Int,
    val price: Double,
    val productImage: String? = null
)

data class DomainOrder(
    val orderId: Int,
    val totalAmount: Double,
    val date: String,
    val paymentStatus: String,
    val orderStatus: String,
    val shippingAddress: String = "",
    val products: List<DomainOrderProduct> = emptyList(),
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
