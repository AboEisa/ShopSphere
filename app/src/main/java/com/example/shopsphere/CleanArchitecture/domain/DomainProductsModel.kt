package com.example.shopsphere.CleanArchitecture.domain


data class DomainProductsModel(
    val products: List<DomainProductResult>
)

data class DomainProductResult(
    val category: String,
    val description: String,
    val id: Int,
    val image: String,
    val price: Double,
    val rating: DomainRating,
    val title: String,
    val stock: Int = 0
)

data class DomainRating(
    val count: Int,
    val rate: Double
)
