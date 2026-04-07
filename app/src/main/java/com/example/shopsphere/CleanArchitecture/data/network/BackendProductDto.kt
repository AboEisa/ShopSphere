package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

data class BackendProductDto(
    @SerializedName("iD_Product")
    val id: Int,
    @SerializedName("name")
    val name: String,
    val price: Double,
    val image: String? = null,
    val size: Int? = null
)
