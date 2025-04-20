package com.example.shopsphere.CleanArchitecture.ui.models

import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.DomainProductsModel
import com.example.shopsphere.CleanArchitecture.domain.DomainRating


fun DomainProductsModel.mapToPresentation(): PresentationProductsModel {
    return PresentationProductsModel(
        products = products.map { it.mapToPresentation() }
    )
}

fun DomainProductResult.mapToPresentation(): PresentationProductResult {
    return PresentationProductResult(
        category = category,
        description = description,
        id = id,
        image = image,
        price = price,
        rating = rating.mapToPresentation(),
        title = title
    )
}

fun DomainRating.mapToPresentation(): PresentationRating {
    return PresentationRating(
        count = count,
        rate = rate
    )
}

fun List<DomainProductResult>.mapToPresentation(): List<PresentationProductResult> {
    return this.map { it.mapToPresentation() }
}