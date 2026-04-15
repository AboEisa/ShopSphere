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
    // Backend uses "productId" per docs; keep legacy aliases as fallbacks so we
    // still work if the server rolls back to the older shapes.
    @SerializedName(value = "productId", alternate = ["iD_Product", "productID", "ProductId"])
    val productId: Int = 0,
    @SerializedName("name") val name: String? = null,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("description") val description: String? = null,
    // Docs say "imagePath"; keep "image" as alias for backward compatibility.
    @SerializedName(value = "imagePath", alternate = ["image"])
    val image: String? = null,
    @SerializedName("size") val size: Int? = null
)
