package com.example.shopsphere.CleanArchitecture.domain

interface IRepository {
    suspend fun getProducts(): Result<List<DomainProductResult>>
    suspend fun getProductsByCategory(category: String): Result<List<DomainProductResult>>
    suspend fun getFavoriteProducts(ids: List<Int>): Result<List<DomainProductResult>>
    suspend fun toggleFavorite(productId: Int)
    suspend fun isFavorite(productId: Int): Boolean
    suspend fun getFavoriteIds(): List<Int>


}