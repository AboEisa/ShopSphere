package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

data class AuthRequestDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AuthResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("customerId") val customerId: String? = null,
    @SerializedName("uid") val uid: String? = null,
    @SerializedName("token") val token: String? = null
) {
    fun resolvedUserId(): String? = userId ?: customerId ?: uid
}
