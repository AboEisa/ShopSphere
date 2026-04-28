package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

// POST /Upload (multipart/form-data, part name "file") returns the stored
// filename. Build the full URL with `${BASE_URL}GetImage/${fileName}`.
data class UploadResponseDto(
    @SerializedName("fileName") val fileName: String? = null,
    @SerializedName("file_name") val fileNameSnake: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null
) {
    /** Returns the canonical filename regardless of which key the server used. */
    fun resolvedFileName(): String? = fileName ?: fileNameSnake
}
