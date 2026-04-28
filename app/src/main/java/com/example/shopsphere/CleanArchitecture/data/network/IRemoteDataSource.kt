package com.example.shopsphere.CleanArchitecture.data.network

import com.example.shopsphere.CleanArchitecture.data.models.ProductResult
import okhttp3.MultipartBody


interface IRemoteDataSource {

    // Products
    suspend fun getProducts(): Result<List<ProductResult>>
    suspend fun getProductsByCategory(category: String): Result<List<ProductResult>>
    suspend fun searchProducts(query: String): Result<List<ProductResult>>

    // Auth
    suspend fun register(name: String, email: String, password: String): Result<AuthResponseDto>
    suspend fun login(email: String, password: String): Result<AuthResponseDto>
    suspend fun loginWithGoogle(idToken: String): Result<AuthResponseDto>
    suspend fun loginWithFacebook(accessToken: String): Result<AuthResponseDto>
    suspend fun logout(): Result<GenericResponseDto>

    // Cart
    suspend fun addToCart(productId: Int, quantity: Int): Result<CartMutationResponseDto>
    suspend fun getCartItems(): Result<GetCartItemsResponseDto>
    suspend fun updateQuantity(productId: Int, newQuantity: Int): Result<CartMutationResponseDto>
    suspend fun removeFromCart(productId: Int): Result<CartMutationResponseDto>
    suspend fun clearCart(): Result<CartMutationResponseDto>

    // Favorites
    suspend fun addToFavorite(productId: Int): Result<FavoriteMutationResponseDto>
    suspend fun removeFromFavorite(productId: Int): Result<FavoriteMutationResponseDto>
    suspend fun getAllFavorites(): Result<List<FavoriteItemDto>>

    // Profile
    suspend fun getMyDetails(): Result<MyDetailsDto>
    suspend fun updateMyDetails(fullName: String, email: String): Result<GenericResponseDto>
    suspend fun updateMyAddressAndPhone(address: String, phone: String): Result<GenericResponseDto>

    // Images
    suspend fun uploadImage(part: MultipartBody.Part): Result<UploadResponseDto>

    // Orders / Payment
    suspend fun checkout(): Result<CheckoutResponseDto>
    suspend fun getMyOrders(): Result<List<MyOrderDto>>
    suspend fun createInvoice(): Result<InvoiceResponseDto>
    suspend fun payNow(): Result<PayNowResponseDto>
    suspend fun paymentCallback(): Result<PaymentCallbackDto>
}
