package com.example.shopsphere.CleanArchitecture.ui.models


data class PresentationProductsModel(
    val products: List<PresentationProductResult>
)

data class PresentationProductResult(
    val category: String,
    val description: String,
    val id: Int,
    val image: String,
    val price: Double,
    val rating: PresentationRating,
    val title: String
)

data class PresentationRating(
    val count: Int,
    val rate: Double
)