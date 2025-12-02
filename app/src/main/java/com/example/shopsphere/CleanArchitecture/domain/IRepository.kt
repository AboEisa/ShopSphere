package com.example.shopsphere.CleanArchitecture.domain

import com.example.shopsphere.CleanArchitecture.data.models.AddToCartRequest
import com.google.firebase.auth.FirebaseUser

interface IRepository {
    suspend fun getProducts(): Result<List<DomainProductResult>>
    suspend fun getProductsByCategory(category: String): Result<List<DomainProductResult>>
    suspend fun getFavoriteProducts(ids: List<Int>): Result<List<DomainProductResult>>
    suspend fun toggleFavorite(productId: Int)
    suspend fun isFavorite(productId: Int): Boolean
    suspend fun getFavoriteIds(): List<Int>
//    suspend fun addToCart(cart: DomainAddToCartRequest): Result<List<DomainCartProduct>>
    suspend fun getCartProducts(ids: List<Int>): Result<List<DomainProductResult>>
    suspend fun getCartItemCount(): Int

    suspend fun registerEmail(name: String, email: String, password:
    String): Result<Boolean>
    suspend fun loginEmail(email: String, pass: String): FirebaseUser

    suspend fun googleSignIn(idToken: String): FirebaseUser

    suspend fun facebookSignIn(accessToken: String): Result<FirebaseUser>
    fun logout()
    fun currentUserId(): String?



}