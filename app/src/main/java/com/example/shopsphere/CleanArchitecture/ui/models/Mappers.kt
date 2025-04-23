package com.example.shopsphere.CleanArchitecture.ui.models

import com.example.shopsphere.CleanArchitecture.domain.DomainAddToCartRequest
import com.example.shopsphere.CleanArchitecture.domain.DomainCartProduct
import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.DomainProductsModel
import com.example.shopsphere.CleanArchitecture.domain.DomainRating

// Domain to Presentation mappings
fun DomainProductsModel.mapToPresentation(): PresentationProductsModel {
    return PresentationProductsModel(
        products = products.map { it.mapToPresentation() } // تحويل كل المنتجات
    )
}

fun DomainProductResult.mapToPresentation(): PresentationProductResult {
    return PresentationProductResult(
        category = category,
        description = description,
        id = id,
        image = image,
        price = price,
        rating = rating.mapToPresentation(), // تحويل التقييم
        title = title,
        quantity =  1
    )
}

fun DomainRating.mapToPresentation(): PresentationRating {
    return PresentationRating(
        count = count,
        rate = rate
    )
}

// تأكد من تحويل الـ List بالطريقة الصحيحة
fun List<DomainProductResult>.mapToPresentation(): List<PresentationProductResult> {
    return this.map { it.mapToPresentation() } // تحويل كل عنصر
}

fun DomainCartProduct.mapToPresentation(): PresentationCartProduct {
    return PresentationCartProduct(
        productId = productId,
        quantity = quantity
    )
}

// Presentation to Domain mappings
fun PresentationAddToCartRequest.mapToDomain(): DomainAddToCartRequest {
    return DomainAddToCartRequest(
        userId = userId,
        date = date,
        products = products.map { it.mapToDomain() }
    )
}

fun PresentationCartProduct.mapToDomain(): DomainCartProduct {
    return DomainCartProduct(
        productId = productId,
        quantity = quantity
    )
}
