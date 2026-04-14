package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

data class AddToCartRequestDto(
    @SerializedName("productId") val productId: Int,
    @SerializedName("Quantity") val quantity: String
)

data class UpdateQuantityRequestDto(
    @SerializedName("cartId") val cartId: Int,
    @SerializedName("newQuantity") val newQuantity: Int
)

data class CartMutationResponseDto(
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("message") val message: String? = null
)

data class GetCartItemsResponseDto(
    @SerializedName("cartItems") val cartItems: List<CartItemDto> = emptyList(),
    @SerializedName("finalGrandTotal") val finalGrandTotal: Double? = null
)

data class CartItemDto(
    @SerializedName("cartID") val cartId: Int = 0,
    @SerializedName("productName") val productName: String? = null,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("quantity") val quantity: Int = 0,
    @SerializedName("image") val image: String? = null,
    @SerializedName("totalPrice") val totalPrice: Double = 0.0
)
