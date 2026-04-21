package com.example.shopsphere.CleanArchitecture.data.network

import com.example.shopsphere.CleanArchitecture.data.models.ProductResult


interface IRemoteDataSource {

    suspend fun getProducts(): Result<List<ProductResult>>
    suspend fun getProductsByCategory(category: String): Result<List<ProductResult>>
    suspend fun register(name: String, email: String, password: String): Result<AuthResponseDto>
    suspend fun login(email: String, password: String): Result<AuthResponseDto>
    suspend fun loginWithGoogle(idToken: String): Result<AuthResponseDto>
    suspend fun loginWithFacebook(accessToken: String): Result<AuthResponseDto>
    suspend fun addToCart(productId: Int, quantity: Int): Result<CartMutationResponseDto>
    suspend fun getCartItems(): Result<GetCartItemsResponseDto>
    suspend fun updateQuantity(productId: Int, newQuantity: Int): Result<CartMutationResponseDto>
    suspend fun removeFromCart(productId: Int): Result<CartMutationResponseDto>
    suspend fun clearCart(): Result<CartMutationResponseDto>
    suspend fun addToFavorite(productId: Int): Result<FavoriteMutationResponseDto>
    suspend fun removeFromFavorite(productId: Int): Result<FavoriteMutationResponseDto>
    suspend fun getAllFavorites(): Result<List<FavoriteItemDto>>
    suspend fun checkout(): Result<CheckoutResponseDto>
    suspend fun getMyOrders(): Result<List<MyOrderDto>>
}
