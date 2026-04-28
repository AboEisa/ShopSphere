package com.example.shopsphere.CleanArchitecture.domain

import com.example.shopsphere.CleanArchitecture.data.network.GenericResponseDto
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.data.network.MyDetailsDto
import javax.inject.Inject

class GetMyDetailsUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(): Result<MyDetailsDto> = remote.getMyDetails()
}

class UpdateMyDetailsUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(fullName: String, email: String): Result<GenericResponseDto> =
        remote.updateMyDetails(fullName = fullName, email = email)
}

class UpdateMyAddressPhoneUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(address: String, phone: String): Result<GenericResponseDto> =
        remote.updateMyAddressAndPhone(address = address, phone = phone)
}
