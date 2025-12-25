package com.example.shopsphere.CleanArchitecture.data.network

data class DummyProductsResponse(
    val products: List<DummyProductItem>
)

data class DummyProductItem(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val price: Double,
    val thumbnail: String? = null,
    val images: List<String>? = null,
    val rating: Double? = null,
    val stock: Int? = null
)

