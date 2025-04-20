package com.example.shopsphere.CleanArchitecture.data

import com.example.shopsphere.CleanArchitecture.data.models.ProductResult
import com.example.shopsphere.CleanArchitecture.data.models.ProductsModel
import com.example.shopsphere.CleanArchitecture.data.models.Rating
import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.DomainProductsModel
import com.example.shopsphere.CleanArchitecture.domain.DomainRating


fun List<ProductResult>.mapToDomain(): DomainProductsModel {
    return DomainProductsModel(
        products = this.map { it.mapToDomain() }
    )
}


fun ProductsModel.mapToDomain(): DomainProductsModel {
    return DomainProductsModel(
        products = products.map { it.mapToDomain() }
    )
}

fun ProductResult.mapToDomain(): DomainProductResult {
    return DomainProductResult(
        category = category,
        description = description,
        id = id,
        image = image,
        price = price,
        rating = rating.mapToDomain(),
        title = title
    )
}

fun Rating.mapToDomain(): DomainRating {
    return DomainRating(
        count = count,
        rate = rate
    )
}