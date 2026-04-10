package com.example.shopsphere.CleanArchitecture.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiServices {

    @GET("All_Products")
    suspend fun getAllProducts(): List<BackendProductDto>

    @GET("{endpoint}")
    suspend fun getProductsByEndpoint(
        @Path("endpoint") endpoint: String
    ): List<BackendProductDto>

    @POST("SignUp")
    suspend fun signUp(
        @Body request: AuthRequestDto
    ): AuthResponseDto

    @POST("Login")
    suspend fun login(
        @Body request: AuthRequestDto
    ): AuthResponseDto

    @POST("AddToCart")
    suspend fun addToCart(
        @Body request: AddToCartRequestDto
    ): CartMutationResponseDto

    @GET("GetCartItems/{customerId}")
    suspend fun getCartItems(
        @Path("customerId") customerId: Int
    ): GetCartItemsResponseDto

    @PUT("UpdateQuantity")
    suspend fun updateQuantity(
        @Body request: UpdateQuantityRequestDto
    ): CartMutationResponseDto

    @DELETE("RemoveItem/{cartId}")
    suspend fun removeItem(
        @Path("cartId") cartId: Int
    ): CartMutationResponseDto

    @DELETE("ClearCart/{customerId}")
    suspend fun clearCart(
        @Path("customerId") customerId: Int
    ): CartMutationResponseDto
}
