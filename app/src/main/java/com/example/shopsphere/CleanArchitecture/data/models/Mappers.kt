package com.example.shopsphere.CleanArchitecture.data.models

import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.DomainRating

fun ProductResult.mapToDomain(): DomainProductResult {
    return DomainProductResult(
        category = category,
        description = description,
        id = id,
        image = image,
        price = price,
        rating = rating.mapToDomain(),
        title = title,
        stock = stock
    )
}

fun Rating.mapToDomain(): DomainRating {
    return DomainRating(
        count = count,
        rate = rate
    )
}
