package com.example.shopsphere.CleanArchitecture.data.network


import com.example.shopsphere.CleanArchitecture.data.models.AddToCartRequest
import com.example.shopsphere.CleanArchitecture.data.models.CartProduct
import com.example.shopsphere.CleanArchitecture.data.models.ProductResult
import com.example.shopsphere.CleanArchitecture.domain.DomainAddToCartRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiServices {

        @GET("products")
        suspend fun getProducts(): List<ProductResult>



        @GET("products/category/{category}")
        suspend fun getProductsByCategory(
                @Path("category") category: String
        ): List<ProductResult>





}