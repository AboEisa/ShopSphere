package com.example.shopsphere.CleanArchitecture.ui.models



data class PresentationAddToCartRequest(
    val userId: Int,
    val date: String,
    val products: List<PresentationCartProduct>
)
data class PresentationCartProduct(
    val productId: Int,
    val quantity: Int
)

