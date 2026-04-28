package com.example.shopsphere.CleanArchitecture.domain

import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.data.network.InvoiceResponseDto
import com.example.shopsphere.CleanArchitecture.data.network.PayNowResponseDto
import com.example.shopsphere.CleanArchitecture.data.network.PaymentCallbackDto
import javax.inject.Inject

class CreateInvoiceUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(): Result<InvoiceResponseDto> = remote.createInvoice()
}

class PayNowUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(): Result<PayNowResponseDto> = remote.payNow()
}

class PaymentCallbackUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(): Result<PaymentCallbackDto> = remote.paymentCallback()
}
