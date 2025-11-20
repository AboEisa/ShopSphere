package com.example.shopsphere.CleanArchitecture.data.models

import com.example.shopsphere.CleanArchitecture.domain.DomainAddToCartRequest
import com.example.shopsphere.CleanArchitecture.domain.DomainCartProduct
import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.DomainProductsModel
import com.example.shopsphere.CleanArchitecture.domain.DomainRating

// Data to Domain mappings
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

fun CartProduct.mapToDomain(): DomainCartProduct {
    return DomainCartProduct(
        productId = productId,
        quantity = quantity
    )
}

// Domain to Data mappings
fun DomainAddToCartRequest.mapToData(): AddToCartRequest {
    return AddToCartRequest(
        userId = userId,
        date = date,
        products = products.map { it.mapToData() }
    )
}

fun DomainCartProduct.mapToData(): CartProduct {
    return CartProduct(
        productId = productId,
        quantity = quantity
    )
}