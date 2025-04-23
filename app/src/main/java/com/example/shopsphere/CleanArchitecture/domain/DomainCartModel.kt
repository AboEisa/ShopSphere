package com.example.shopsphere.CleanArchitecture.domain



data class DomainAddToCartRequest(
    val userId: Int,
    val date: String,
    val products: List<DomainCartProduct>
)
data class DomainCartProduct(
    val productId: Int,
    val quantity: Int
)

