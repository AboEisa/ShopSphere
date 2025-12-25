package com.example.shopsphere.CleanArchitecture.data.network

import com.example.shopsphere.CleanArchitecture.data.models.ProductResult


interface IRemoteDataSource {

    suspend fun getProducts(): Result<List<ProductResult>>
    suspend fun getProductsByCategory(category: String): Result<List<ProductResult>>
//    suspend fun addToCart(cart: AddToCartRequest): Result<List<CartProduct>>


}
