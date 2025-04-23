package com.example.shopsphere.CleanArchitecture.data.models


data class AddToCartRequest(
    val userId: Int,
    val date: String,
    val products: List<CartProduct>
)

data class CartProduct(
    val productId: Int,
    val quantity: Int
)


