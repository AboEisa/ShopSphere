package com.example.shopsphere.CleanArchitecture.domain

import com.example.shopsphere.CleanArchitecture.data.models.AddToCartRequest
import kotlinx.coroutines.flow.Flow

interface IRepository {
    suspend fun getProducts(): Result<List<DomainProductResult>>

    /**
     * Stale-while-revalidate variant of [getProducts]. Emits the on-disk
     * cache immediately (if any) so the UI can render without waiting on
     * the slow backend, then makes the network call and emits the fresh
     * result. The cache is updated when the network call succeeds. On
     * network failure with a non-empty cache, no failure is emitted —
     * the user keeps seeing the stale list rather than a blank screen.
     */
    fun observeProducts(): Flow<Result<List<DomainProductResult>>>
    suspend fun getProductsByCategory(category: String): Result<List<DomainProductResult>>
    suspend fun searchProducts(query: String): Result<List<DomainProductResult>>
    suspend fun getFavoriteProducts(ids: List<Int>): Result<List<DomainProductResult>>
    suspend fun toggleFavorite(productId: Int)
    suspend fun isFavorite(productId: Int): Boolean
    suspend fun getFavoriteIds(): List<Int>
//    suspend fun addToCart(cart: DomainAddToCartRequest): Result<List<DomainCartProduct>>
    suspend fun getCartProducts(ids: List<Int>): Result<List<DomainProductResult>>
    suspend fun getCartItems(): Result<List<DomainCartItem>>
    suspend fun addToCart(productId: Int, quantity: Int = 1): Result<Unit>
    suspend fun updateCartItemQuantity(cartId: Int, newQuantity: Int): Result<Unit>
    suspend fun removeCartItem(cartId: Int): Result<Unit>
    suspend fun clearCart(): Result<Unit>
    suspend fun getCartItemCount(): Int

    suspend fun checkout(): Result<DomainCheckoutResult>
    suspend fun getMyOrders(): Result<List<DomainOrder>>

    /** Stale-while-revalidate variant of [getMyOrders] — see [observeProducts]. */
    fun observeMyOrders(): Flow<Result<List<DomainOrder>>>

    suspend fun registerEmail(firstName: String, lastName: String, email: String, password: String): Result<Boolean>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun loginWithGoogle(idToken: String): Result<Unit>
    suspend fun loginWithFacebook(accessToken: String): Result<Unit>

    fun logout()
    fun currentUserId(): String?



}
