package com.example.shopsphere.CleanArchitecture.data.network

import com.example.shopsphere.CleanArchitecture.data.models.ProductResult


interface IRemoteDataSource {

    suspend fun getProducts(): Result<List<ProductResult>>
    suspend fun getProductsByCategory(category: String): Result<List<ProductResult>>
    suspend fun register(name: String, email: String, password: String): Result<AuthResponseDto>
    suspend fun login(email: String, password: String): Result<AuthResponseDto>
    suspend fun addToCart(userId: Int, productId: Int, quantity: Int): Result<CartMutationResponseDto>
    suspend fun getCartItems(customerId: Int): Result<GetCartItemsResponseDto>
    suspend fun updateQuantity(cartId: Int, newQuantity: Int): Result<CartMutationResponseDto>
    suspend fun removeItem(cartId: Int): Result<CartMutationResponseDto>
    suspend fun clearCart(customerId: Int): Result<CartMutationResponseDto>
//    suspend fun addToCart(cart: AddToCartRequest): Result<List<CartProduct>>


}
