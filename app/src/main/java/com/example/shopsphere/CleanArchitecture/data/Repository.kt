package com.example.shopsphere.CleanArchitecture.data

import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import javax.inject.Inject

class Repository @Inject constructor(private val remoteDataSource: IRemoteDataSource, private val sharedPreferencesHelper: SharedPreference) : IRepository {



    override suspend fun getProducts(): Result<List<DomainProductResult>> {
        return try {
            val remoteData = remoteDataSource.getProducts()
            Result.success(remoteData.getOrNull()?.map { it.mapToDomain() } ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductsByCategory(category: String): Result<List<DomainProductResult>> {

        return try {
            val remoteData = remoteDataSource.getProductsByCategory(category)
            Result.success(remoteData.getOrNull()?.map { it.mapToDomain() } ?: emptyList())
            } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavoriteProducts(ids: List<Int>): Result<List<DomainProductResult>> {
        return try {
            val favoriteIds = sharedPreferencesHelper.getFavoriteProducts()
            val allProducts = remoteDataSource.getProducts().getOrThrow()
            val favorites = allProducts.filter { favoriteIds.contains(it.id) }
            Result.success(favorites.map { it.mapToDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun toggleFavorite(productId: Int) {
        if (sharedPreferencesHelper.isFavorite(productId)) {
            sharedPreferencesHelper.removeFavoriteProduct(productId)
        } else {
            sharedPreferencesHelper.addFavoriteProduct(productId)
        }
    }

    override suspend fun isFavorite(productId: Int): Boolean {
        return sharedPreferencesHelper.isFavorite(productId)
    }

    override suspend fun getFavoriteIds(): List<Int> {
        return sharedPreferencesHelper.getFavoriteProducts()
    }


}