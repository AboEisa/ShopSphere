package com.example.shopsphere.CleanArchitecture.data

import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.models.mapToDomain
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.domain.DomainCartItem
import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import javax.inject.Inject

class Repository @Inject constructor(
    private val remoteDataSource: IRemoteDataSource,
    private val sharedPreferencesHelper: SharedPreference
) : IRepository {


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

//    override suspend fun addToCart(cart: DomainAddToCartRequest): Result<List<DomainCartProduct>> {
//        return try {
//            val remoteData = remoteDataSource.addToCart(cart.mapToData())
//            Result.success(remoteData.getOrNull()?.map { it.mapToDomain() } ?: emptyList())
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    override suspend fun getCartProducts(ids: List<Int>): Result<List<DomainProductResult>> {
        return try {
            val allProducts = remoteDataSource.getProducts().getOrThrow()
            val cartProducts = allProducts.filter { ids.contains(it.id) }
            Result.success(cartProducts.map { it.mapToDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCartItems(): Result<List<DomainCartItem>> {
        val customerId = currentUserId()?.toIntOrNull()
            ?: return Result.failure(Exception("Missing customer id"))
        return remoteDataSource.getCartItems(customerId).map { response ->
            response.cartItems.map {
                DomainCartItem(
                    cartId = it.cartId,
                    productId = it.productId,
                    quantity = it.quantity
                )
            }
        }
    }

    override suspend fun addToCart(productId: Int, quantity: Int): Result<Unit> {
        val customerId = currentUserId()?.toIntOrNull()
            ?: return Result.failure(Exception("Missing customer id"))
        return remoteDataSource.addToCart(customerId, productId, quantity).map { Unit }
    }

    override suspend fun updateCartItemQuantity(cartId: Int, newQuantity: Int): Result<Unit> {
        return remoteDataSource.updateQuantity(cartId, newQuantity).map { Unit }
    }

    override suspend fun removeCartItem(cartId: Int): Result<Unit> {
        return remoteDataSource.removeItem(cartId).map { Unit }
    }

    override suspend fun clearCart(): Result<Unit> {
        val customerId = currentUserId()?.toIntOrNull()
            ?: return Result.failure(Exception("Missing customer id"))
        return remoteDataSource.clearCart(customerId).map { Unit }
    }




    override suspend fun login(email: String, password: String): Result<Unit> =
        remoteDataSource.login(email, password).fold(
            onSuccess = { response ->
                val userId = response.resolvedUserId()
                if (userId.isNullOrBlank()) {
                    Result.failure(Exception(response.message ?: "Login succeeded but missing user ID"))
                } else {
                    sharedPreferencesHelper.saveUid(userId)
                    Result.success(Unit)
                }
            },
            onFailure = { Result.failure(it) }
        )

    override suspend fun loginWithGoogle(idToken: String): Result<Unit> =
        Result.failure(Exception("Google login is not supported by backend API yet"))

    override suspend fun loginWithFacebook(accessToken: String): Result<Unit> =
        Result.failure(Exception("Facebook login is not supported by backend API yet"))



    override suspend fun registerEmail(
        name: String,
        email: String,
        password: String
    ): Result<Boolean> {
        return remoteDataSource.register(name, email, password).fold(
            onSuccess = { response ->
                val userId = response.resolvedUserId()
                if (userId.isNullOrBlank()) {
                    Result.failure(Exception(response.message ?: "Registration succeeded but missing user ID"))
                } else {
                    sharedPreferencesHelper.saveUid(userId)
                    Result.success(true)
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun getCartItemCount(): Int {
        return getCartItems().getOrNull()?.sumOf { it.quantity } ?: sharedPreferencesHelper.getCartItemCount()
    }

    override fun logout() {
        sharedPreferencesHelper.clearUid()
    }

    override fun currentUserId(): String? = sharedPreferencesHelper.getUid().ifBlank { null }

}
