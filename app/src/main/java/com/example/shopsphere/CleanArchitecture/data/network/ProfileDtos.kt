package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

// GET /MyDetails — returns the authenticated user's profile.
data class MyDetailsDto(
    @SerializedName("firstName")  val firstName: String?  = null,
    @SerializedName("lastName")   val lastName: String?   = null,
    @SerializedName("email")      val email: String?      = null,
    @SerializedName("phone")      val phone: String?      = null,
    @SerializedName("address")    val address: String?    = null,
    @SerializedName("userId")     val userId: Int?        = null,
    @SerializedName("customerId") val customerId: Int?    = null
) {
    val fullName: String get() = listOfNotNull(
        firstName?.trim()?.ifBlank { null },
        lastName?.trim()?.ifBlank { null }
    ).joinToString(" ")
}

// PUT /UpdateMyDetails  body: { firstName, lastName, email, phone, address }
data class UpdateMyDetailsRequest(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName")  val lastName: String,
    @SerializedName("email")     val email: String,
    @SerializedName("phone")     val phone: String,
    @SerializedName("address")   val address: String
)

// Generic { status, message } envelope returned by Logout, Update*, Callback etc.
data class GenericResponseDto(
    @SerializedName("status")  val status: String?  = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("success") val success: Boolean? = null
)
