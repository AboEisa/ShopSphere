package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

// GET /MyDetails — returns the authenticated user's profile.
// Field names mirror the SignUp body the server stores (PascalCase).
data class MyDetailsDto(
    @SerializedName("FullName") val fullName: String? = null,
    @SerializedName("Email") val email: String? = null,
    @SerializedName("Phone") val phone: String? = null,
    @SerializedName("Address") val address: String? = null,
    @SerializedName("userId") val userId: Int? = null,
    @SerializedName("customerId") val customerId: Int? = null
)

// PUT /UpdateMyDetails  body: { "FullName": "...", "Email": "..." }
data class UpdateMyDetailsRequest(
    @SerializedName("FullName") val fullName: String,
    @SerializedName("Email") val email: String
)

// PUT /UpdateMyAddress&Phone  body: { "Address": "...", "Phone": "..." }
// (the literal `&` in the path is preserved by routing the call through @Url)
data class UpdateAddressPhoneRequest(
    @SerializedName("Address") val address: String,
    @SerializedName("Phone") val phone: String
)

// Generic { status, message } envelope returned by Logout, Update*, Callbackt etc.
data class GenericResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("success") val success: Boolean? = null
)
