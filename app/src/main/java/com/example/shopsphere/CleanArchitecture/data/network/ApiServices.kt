package com.example.shopsphere.CleanArchitecture.data.network


import com.example.shopsphere.CleanArchitecture.data.models.ProductResult
import com.example.shopsphere.CleanArchitecture.data.models.ProductsModel
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiServices {

        @GET("products")
        suspend fun getProducts(): List<ProductResult>



        @GET("products/category/{category}")
        suspend fun getProductsByCategory(
                @Path("category") category: String
        ): List<ProductResult>


}