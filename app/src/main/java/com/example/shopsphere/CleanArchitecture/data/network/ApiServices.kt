package com.example.shopsphere.CleanArchitecture.data.network

import retrofit2.http.GET
import retrofit2.http.Path

interface ApiServices {

    @GET("All_Products")
    suspend fun getAllProducts(): List<BackendProductDto>

    @GET("{endpoint}")
    suspend fun getProductsByEndpoint(
        @Path("endpoint") endpoint: String
    ): List<BackendProductDto>
}
