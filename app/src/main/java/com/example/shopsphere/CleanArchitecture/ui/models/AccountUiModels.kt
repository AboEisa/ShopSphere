package com.example.shopsphere.CleanArchitecture.ui.models

data class AddressBookItem(
    val id: String,
    val title: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDefault: Boolean = false,
    val isSelected: Boolean = false
)

data class PaymentMethodItem(
    val id: String,
    val brand: String,
    val holderName: String,
    val lastFour: String,
    val isDefault: Boolean = false,
    val isSelected: Boolean = false
)

data class OrderHistoryItem(
    val orderId: String,
    val date: String,
    val status: String,
    val total: String,
    val itemTitle: String = "",
    val itemSize: String = "",
    val itemImageUrl: String = "",
    val itemPrice: String = "",
    val reviewRating: Double = 0.0,
    val reviewComment: String = "",
    val address: String = "",
    val customerName: String = "",
    val phone: String = "",
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val statusStep: Int = 0
)
