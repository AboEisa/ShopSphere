package com.example.shopsphere.CleanArchitecture.data

import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.models.mapToDomain
import com.example.shopsphere.CleanArchitecture.data.network.AuthResponseDto
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.utils.Constant
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
            val favoriteItems = remoteDataSource.getAllFavorites().getOrThrow()
            val allProducts = remoteDataSource.getProducts().getOrThrow()
            val favoriteIds = favoriteItems.map { it.productId }.toSet()
            val favorites = allProducts.filter { it.id in favoriteIds }
            Result.success(favorites.map { it.mapToDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(productId: Int) {
        val currentFavorites = remoteDataSource.getAllFavorites().getOrNull().orEmpty()
        val isFav = currentFavorites.any { it.productId == productId }
        if (isFav) {
            remoteDataSource.removeFromFavorite(productId)
        } else {
            remoteDataSource.addToFavorite(productId)
        }
    }

    override suspend fun isFavorite(productId: Int): Boolean {
        val favorites = remoteDataSource.getAllFavorites().getOrNull().orEmpty()
        return favorites.any { it.productId == productId }
    }

    override suspend fun getFavoriteIds(): List<Int> {
        return remoteDataSource.getAllFavorites().getOrNull().orEmpty().map { it.productId }
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
        return remoteDataSource.getCartItems().map { response ->
            response.cartItems.map {
                val imageUrl = if (it.image.isNullOrBlank()) "" else {
                    if (it.image.startsWith("http")) it.image
                    else "${Constant.BASE_URL}GetImage/${it.image}"
                }
                DomainCartItem(
                    cartId = it.cartId,
                    productName = it.productName.orEmpty(),
                    price = it.price,
                    quantity = it.quantity,
                    image = imageUrl
                )
            }
        }
    }

    override suspend fun addToCart(productId: Int, quantity: Int): Result<Unit> {
        return remoteDataSource.addToCart(productId, quantity).map { Unit }
    }

    override suspend fun updateCartItemQuantity(cartId: Int, newQuantity: Int): Result<Unit> {
        return remoteDataSource.updateQuantity(cartId, newQuantity).map { Unit }
    }

    override suspend fun removeCartItem(cartId: Int): Result<Unit> {
        return remoteDataSource.removeItem(cartId).map { Unit }
    }

    override suspend fun clearCart(): Result<Unit> {
        return remoteDataSource.clearCart().map { Unit }
    }




    private fun saveAuthIds(response: AuthResponseDto) {
        val userId = response.resolvedUserId()
        if (!userId.isNullOrBlank()) {
            sharedPreferencesHelper.saveUid(userId)
        }
        // Save customerId separately for cart operations
        val custId = response.customerId
        if (custId != null) {
            sharedPreferencesHelper.saveCustomerId(custId.toString())
        }
        // Save JWT token for Bearer auth
        val token = response.token
        if (!token.isNullOrBlank()) {
            sharedPreferencesHelper.saveToken(token)
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> =
        remoteDataSource.login(email, password).fold(
            onSuccess = { response ->
                val userId = response.resolvedUserId()
                if (userId.isNullOrBlank()) {
                    Result.failure(Exception(response.message ?: "Login succeeded but missing user ID"))
                } else {
                    saveAuthIds(response)
                    Result.success(Unit)
                }
            },
            onFailure = { Result.failure(it) }
        )

    override suspend fun loginWithGoogle(idToken: String): Result<Unit> =
        try {
            val response = remoteDataSource.loginWithGoogle(idToken)
            response.fold(
                onSuccess = { authResponse ->
                    val userId = authResponse.resolvedUserId()
                    if (userId.isNullOrBlank()) {
                        Result.failure(Exception(authResponse.message ?: "Google login succeeded but missing user ID"))
                    } else {
                        saveAuthIds(authResponse)
                        Result.success(Unit)
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun loginWithFacebook(accessToken: String): Result<Unit> =
        try {
            val response = remoteDataSource.loginWithFacebook(accessToken)
            response.fold(
                onSuccess = { authResponse ->
                    val userId = authResponse.resolvedUserId()
                    if (userId.isNullOrBlank()) {
                        Result.failure(Exception(authResponse.message ?: "Facebook login succeeded but missing user ID"))
                    } else {
                        saveAuthIds(authResponse)
                        Result.success(Unit)
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }



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
                    saveAuthIds(response)
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
