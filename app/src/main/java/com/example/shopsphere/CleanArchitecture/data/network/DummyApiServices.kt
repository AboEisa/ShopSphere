package com.example.shopsphere.CleanArchitecture.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface DummyApiServices {
    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int = 100
    ): DummyProductsResponse
}

