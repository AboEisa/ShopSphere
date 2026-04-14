package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

data class AuthRequestDto(
    @SerializedName("FullName") val fullName: String? = null,
    @SerializedName("Email") val email: String,
    @SerializedName("Password") val password: String,
    @SerializedName("Phone") val phone: String? = null,
    @SerializedName("Address") val address: String? = null
)

data class AuthResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("userId") val userId: Int? = null,
    @SerializedName("customerId") val customerId: Int? = null,
    @SerializedName("uid") val uid: String? = null,
    @SerializedName("token") val token: String? = null
) {
    fun resolvedUserId(): String? {
        userId?.let { return it.toString() }
        customerId?.let { return it.toString() }
        return uid
    }
}
