package com.example.shopsphere.CleanArchitecture.data.network


import com.example.shopsphere.CleanArchitecture.data.models.ProductResult
import com.example.shopsphere.CleanArchitecture.data.models.ProductsModel


interface IRemoteDataSource {

    suspend fun getProducts(): Result<List<ProductResult>>
    suspend fun getProductsByCategory(category: String): Result<List<ProductResult>>


}