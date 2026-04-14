package com.example.shopsphere.CleanArchitecture.domain

data class DomainCartItem(
    val cartId: Int,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val image: String
)
