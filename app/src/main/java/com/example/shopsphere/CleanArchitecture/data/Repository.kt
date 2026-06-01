package com.example.shopsphere.CleanArchitecture.data

import com.example.shopsphere.CleanArchitecture.data.local.OrdersCacheStore
import com.example.shopsphere.CleanArchitecture.data.local.ProductsCacheStore
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.models.*
import com.example.shopsphere.CleanArchitecture.data.network.AuthResponseDto
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.utils.Constant
import com.example.shopsphere.CleanArchitecture.domain.DomainCartItem
import com.example.shopsphere.CleanArchitecture.domain.DomainCheckoutResult
import com.example.shopsphere.CleanArchitecture.domain.DomainOrder
import com.example.shopsphere.CleanArchitecture.domain.DomainOrderProduct
import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class Repository @Inject constructor(
    private val remoteDataSource: IRemoteDataSource,
    private val sharedPreferencesHelper: SharedPreference,
    private val productsCache: ProductsCacheStore,
    private val ordersCache: OrdersCacheStore
) : IRepository {

    @Volatile
    private var cachedFavoriteIds: Set<Int>? = null
    private var favoritesCacheAt: Long = 0L

    private suspend fun favoriteIds(forceRefresh: Boolean = false): Set<Int> {
        val now = System.currentTimeMillis()
        val cached = cachedFavoriteIds
        if (!forceRefresh && cached != null && now - favoritesCacheAt < FAVORITES_TTL_MILLIS) {
            return cached
        }
        val ids = remoteDataSource.getAllFavorites().getOrNull()
            .orEmpty()
            .map { it.productId }
            .toSet()
        cachedFavoriteIds = ids
        favoritesCacheAt = now
        return ids
    }

    private fun invalidateFavoritesCache() {
        cachedFavoriteIds = null
    }


    override suspend fun getProducts(): Result<List<DomainProductResult>> {
        return try {
            val remoteData = remoteDataSource.getProducts()
            val products = remoteData.getOrNull()?.map { it.mapToDomain() } ?: emptyList()
            // Always refresh the cache on a successful network call so future
            // observeProducts() collectors get the latest snapshot first.
            if (products.isNotEmpty()) productsCache.save(products)
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeProducts(): Flow<Result<List<DomainProductResult>>> = flow {
        // 1) Emit cache straight away when present so the UI paints fast.
        val cached = productsCache.load()
        val cacheEmitted = cached.isNotEmpty()
        if (cacheEmitted) emit(Result.success(cached))

        // 2) Hit the network and emit the fresh result. On failure we only
        //    bubble up the error if the user has nothing on screen yet —
        //    otherwise we keep them on the stale list.
        val fresh = runCatching {
            remoteDataSource.getProducts().getOrNull()?.map { it.mapToDomain() } ?: emptyList()
        }
        fresh
            .onSuccess { products ->
                if (products.isNotEmpty()) productsCache.save(products)
                emit(Result.success(products))
            }
            .onFailure { e ->
                if (!cacheEmitted) emit(Result.failure(e))
            }
    }

    override suspend fun searchProducts(query: String): Result<List<DomainProductResult>> {
        return try {
            val results = remoteDataSource.searchProducts(query)
            Result.success(results.getOrNull()?.map { it.mapToDomain() } ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavoriteProducts(ids: List<Int>): Result<List<DomainProductResult>> {
        return try {
            val favIds = favoriteIds(forceRefresh = true)
            if (favIds.isEmpty()) return Result.success(emptyList())

            val allProducts = remoteDataSource.getProducts().getOrNull().orEmpty()
            val favorites = allProducts.filter { it.id in favIds }
            Result.success(favorites.map { it.mapToDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(productId: Int) {
        val currentIds = favoriteIds()
        val isFav = productId in currentIds
        android.util.Log.d("Repository", "Toggle favorite: productId=$productId, isFav=$isFav")
        
        // Optimistic cache update — UI reads reflect the change immediately.
        cachedFavoriteIds = if (isFav) currentIds - productId else currentIds + productId
        
        val result = if (isFav) {
            android.util.Log.d("Repository", "Removing from favorites")
            remoteDataSource.removeFromFavorite(productId)
        } else {
            android.util.Log.d("Repository", "Adding to favorites")
            remoteDataSource.addToFavorite(productId)
        }
        
        if (result.isSuccess) {
            android.util.Log.d("Repository", "✅ Favorite toggle succeeded")
        } else {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            android.util.Log.e("Repository", "❌ Favorite toggle failed: $error")
            // Roll back on failure.
            cachedFavoriteIds = currentIds
        }
    }

    override suspend fun isFavorite(productId: Int): Boolean {
        return productId in favoriteIds()
    }

    override suspend fun getFavoriteIds(): List<Int> {
        return favoriteIds().toList()
    }

    override suspend fun getCartItems(): Result<List<DomainCartItem>> {
        return remoteDataSource.getCartItems().map { response ->
            // The backend returns cartID values that aren't valid identifiers for
            // RemoveItem / UpdateQuantity (they return 404 / 400). Those endpoints
            // actually want the real productId. Resolve it by matching productName
            // against the products catalog.
            val allProducts = remoteDataSource.getProducts().getOrNull().orEmpty()
            val productIdByName = allProducts.associate { it.title.trim() to it.id }

            response.cartItems.map { item ->
                val imageUrl = if (item.image.isNullOrBlank()) "" else {
                    if (item.image.startsWith("http")) item.image
                    else "${Constant.BASE_URL}GetImage/${item.image}"
                }
                val resolvedProductId = if (item.productId != 0) {
                    item.productId
                } else {
                    productIdByName[item.productName?.trim().orEmpty()] ?: 0
                }
                DomainCartItem(
                    cartId = item.cartId,
                    productId = resolvedProductId,
                    productName = item.productName.orEmpty(),
                    price = item.price,
                    quantity = item.quantity,
                    image = imageUrl
                )
            }
        }
    }

    override suspend fun addToCart(productId: Int, quantity: Int): Result<Unit> {
        return remoteDataSource.addToCart(productId, quantity).map { Unit }
    }

    // Domain-layer parameter is still named `cartId` for backward compatibility,
    // but per Swagger the backend actually wants productId here. Callers must
    // pass the real productId (CartViewModel does via resolveServerItemId).
    override suspend fun updateCartItemQuantity(cartId: Int, newQuantity: Int): Result<Unit> {
        return remoteDataSource.updateQuantity(productId = cartId, newQuantity = newQuantity)
            .map { Unit }
    }

    override suspend fun removeCartItem(cartId: Int): Result<Unit> {
        return remoteDataSource.removeFromCart(productId = cartId).map { Unit }
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

    override suspend fun registerEmail(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Result<Boolean> {
        return remoteDataSource.register(firstName, lastName, email, password).fold(
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
        invalidateFavoritesCache()
    }

    override fun currentUserId(): String? = sharedPreferencesHelper.getUid().ifBlank { null }

    override suspend fun checkout(): Result<DomainCheckoutResult> {
        return remoteDataSource.checkout().mapCatching { response ->
            DomainCheckoutResult(
                success = response.success == true,
                orderId = response.orderId,
                message = response.message
            )
        }
    }

    override suspend fun getMyOrders(): Result<List<DomainOrder>> {
        return remoteDataSource.getMyOrders().mapCatching { orders ->
            orders.map { dto ->
                DomainOrder(
                    orderId = dto.orderId,
                    totalAmount = dto.totalAmount,
                    date = dto.date.orEmpty(),
                    paymentStatus = dto.paymentStatus.orEmpty(),
                    orderStatus = dto.orderStatus.orEmpty(),
                    shippingAddress = dto.shippingAddress.orEmpty(),
                    products = dto.products.map { p ->
                        DomainOrderProduct(
                            productName = p.productName,
                            quantity = p.quantity,
                            price = p.price,
                            productImage = p.productImage
                        )
                    },
                    currentLat = dto.currentLat,
                    currentLng = dto.currentLng,
                    driverName = dto.driverName?.trim()?.takeIf { it.isNotBlank() }
                )
            }
        }.also { result ->
            // Persist a fresh snapshot for the next launch's instant render.
            result.getOrNull()?.let { ordersCache.save(it) }
        }
    }

    override fun observeMyOrders(): Flow<Result<List<DomainOrder>>> = flow {
        val cached = ordersCache.load()
        val cacheEmitted = cached.isNotEmpty()
        if (cacheEmitted) emit(Result.success(cached))

        val fresh = runCatching { getMyOrders() }
            .getOrElse { Result.failure(it) }

        fresh
            .onSuccess { emit(Result.success(it)) }
            .onFailure { e -> if (!cacheEmitted) emit(Result.failure(e)) }
    }

    companion object {
        private const val FAVORITES_TTL_MILLIS = 30_000L
    }
}
