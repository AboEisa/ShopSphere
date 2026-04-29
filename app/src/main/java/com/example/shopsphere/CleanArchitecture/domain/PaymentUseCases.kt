package com.example.shopsphere.CleanArchitecture.domain

import com.example.shopsphere.CleanArchitecture.data.network.CreateInvoiceRequest
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.data.network.InvoiceResponseDto
import com.example.shopsphere.CleanArchitecture.data.network.PayNowRequest
import com.example.shopsphere.CleanArchitecture.data.network.PayNowResponseDto
import com.example.shopsphere.CleanArchitecture.data.network.PaymentCallbackDto
import javax.inject.Inject

class CreateInvoiceUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(request: CreateInvoiceRequest): Result<InvoiceResponseDto> = remote.createInvoice(request)
}

class PayNowUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(request: PayNowRequest): Result<PayNowResponseDto> = remote.payNow(request)
}

class PaymentCallbackUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(orderId: Int): Result<PaymentCallbackDto> = remote.paymentCallback(orderId)
}

class MarkPaymentAsFailedUseCase @Inject constructor(
    private val remote: IRemoteDataSource
) {
    suspend operator fun invoke(orderId: Int): Result<PaymentCallbackDto> = remote.markPaymentAsFailed(orderId)
}
