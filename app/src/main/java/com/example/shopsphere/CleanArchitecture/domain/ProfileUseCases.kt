package com.example.shopsphere.CleanArchitecture.domain

import com.example.shopsphere.CleanArchitecture.data.network.GenericResponseDto
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.data.network.MyDetailsDto
import javax.inject.Inject

/**
 * Fetches the authenticated user's profile from the server.
 *
 * Returns [Result<MyDetailsDto>] so callers can inspect success/failure without
 * catching exceptions themselves. Null fields in [MyDetailsDto] mean the server
 * returned the key as null or omitted it entirely — callers must handle this.
 */
class GetMyDetailsUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(): Result<MyDetailsDto> = remote.getMyDetails()
}

/**
 * Updates the user's profile via PUT /UpdateMyDetails.
 * All five fields (firstName, lastName, email, phone, address) are sent together.
 */
class UpdateMyDetailsUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        address: String
    ): Result<GenericResponseDto> {
        require(firstName.isNotBlank()) { "firstName must not be blank" }
        require(email.isNotBlank()) { "email must not be blank" }
        return remote.updateMyDetails(
            firstName = firstName,
            lastName  = lastName,
            email     = email,
            phone     = phone,
            address   = address
        )
    }
}
