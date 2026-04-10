package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

data class AddToCartRequestDto(
    @SerializedName("userId") val userId: Int,
    @SerializedName("productId") val productId: Int,
    @SerializedName("quantity") val quantity: Int
)

data class UpdateQuantityRequestDto(
    @SerializedName("cartId") val cartId: Int,
    @SerializedName("newQuantity") val newQuantity: Int
)

data class CartMutationResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null
)

data class GetCartItemsResponseDto(
    @SerializedName("cartItems") val cartItems: List<CartItemDto> = emptyList(),
    @SerializedName("finalGrandTotal") val finalGrandTotal: Double? = null
)

data class CartItemDto(
    @SerializedName("cartId") val cartId: Int,
    @SerializedName("productId") val productId: Int,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("addedDate") val addedDate: String? = null
)
