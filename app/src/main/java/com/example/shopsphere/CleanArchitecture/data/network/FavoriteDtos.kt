package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

data class FavoriteRequestDto(
    @SerializedName("productId") val productId: Int
)

data class FavoriteMutationResponseDto(
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("message") val message: String? = null
)

data class FavoriteItemDto(
    @SerializedName("iD_Product") val productId: Int = 0,
    @SerializedName("name") val name: String? = null,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("image") val image: String? = null,
    @SerializedName("size") val size: Int? = null
)
