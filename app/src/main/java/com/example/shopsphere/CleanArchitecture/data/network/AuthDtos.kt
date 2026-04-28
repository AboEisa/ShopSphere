package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

// SignUp body uses PascalCase keys (per /SignUp Postman).
data class AuthRequestDto(
    @SerializedName("FullName") val fullName: String? = null,
    @SerializedName("Email") val email: String,
    @SerializedName("Password") val password: String,
    @SerializedName("Phone") val phone: String? = null,
    @SerializedName("Address") val address: String? = null
)

// Login body uses lowercase keys (per /Login Postman). Kept as a separate DTO
// so we can't accidentally send the wrong casing.
data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
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
