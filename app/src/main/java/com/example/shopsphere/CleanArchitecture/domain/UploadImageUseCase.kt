package com.example.shopsphere.CleanArchitecture.domain

import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.data.network.UploadResponseDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class UploadImageUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    /**
     * Convenience overload — wraps a [File] into the multipart shape the
     * server expects (part name `file`, generic image MIME type if unknown).
     */
    suspend operator fun invoke(file: File): Result<UploadResponseDto> {
        val mime = guessMime(file.extension)
        val requestBody = file.asRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        return remote.uploadImage(part)
    }

    /** Pass-through for callers that already built the part themselves. */
    suspend operator fun invoke(part: MultipartBody.Part): Result<UploadResponseDto> =
        remote.uploadImage(part)

    private fun guessMime(extension: String): String = when (extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png"         -> "image/png"
        "webp"        -> "image/webp"
        "gif"         -> "image/gif"
        else          -> "application/octet-stream"
    }
}
