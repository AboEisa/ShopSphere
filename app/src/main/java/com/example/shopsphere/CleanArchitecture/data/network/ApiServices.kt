package com.example.shopsphere.CleanArchitecture.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.HTTP

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

    // Per Swagger: POST /AddToCart body { "productID": int, "quantity": int }
    @POST("AddToCart")
    suspend fun addToCart(
        @Body request: AddToCartRequestDto
    ): CartMutationResponseDto

    @GET("GetCartItems")
    suspend fun getCartItems(): GetCartItemsResponseDto

    // Per Swagger: PUT /UpdateQuantity?productId=<int>&newQuantity=<int>
    // (query parameters — no JSON body).
    @PUT("UpdateQuantity")
    suspend fun updateQuantity(
        @Query("productId") productId: Int,
        @Query("newQuantity") newQuantity: Int
    ): CartMutationResponseDto

    // Per Swagger: DELETE /RemoveFromCart?productId=<int>
    @DELETE("RemoveFromCart")
    suspend fun removeFromCart(
        @Query("productId") productId: Int
    ): CartMutationResponseDto

    @DELETE("ClearCart")
    suspend fun clearCart(): CartMutationResponseDto

    @POST("AddToFavorite")
    suspend fun addToFavorite(
        @Body request: FavoriteRequestDto
    ): FavoriteMutationResponseDto

    // Per API docs, RemoveFromFavorite is DELETE with a JSON body.
    @HTTP(method = "DELETE", path = "RemoveFromFavorite", hasBody = true)
    suspend fun removeFromFavorite(
        @Body request: FavoriteRequestDto
    ): FavoriteMutationResponseDto

    @GET("GetAllFavorites")
    suspend fun getAllFavorites(): List<FavoriteItemDto>
}
