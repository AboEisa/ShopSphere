package com.example.shopsphere.CleanArchitecture.data.network

import com.example.shopsphere.CleanArchitecture.data.models.ProductResult
import com.example.shopsphere.CleanArchitecture.data.models.Rating
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val apiService: ApiServices
) : IRemoteDataSource {

    private val cacheMutex = Mutex()
    private var cachedProducts: List<ProductResult>? = null
    private var lastCacheAtMillis: Long = 0L

    override suspend fun getProducts(): Result<List<ProductResult>> = runCatching {
        cacheMutex.withLock {
            val now = System.currentTimeMillis()
            cachedProducts
                ?.takeIf { now - lastCacheAtMillis < CACHE_WINDOW_MILLIS }
                ?.let { return@withLock it }

            val freshProducts = fetchProductsFromBackend()
            cachedProducts = freshProducts
            lastCacheAtMillis = now
            freshProducts
        }
    }

    override suspend fun getProductsByCategory(category: String): Result<List<ProductResult>> {
        val normalizedCategory = normalizeCategory(category)
        if (normalizedCategory == normalizeCategory(ALL_CATEGORY)) {
            return getProducts()
        }

        return runCatching {
            getProducts()
                .getOrThrow()
                .filter { normalizeCategory(it.category) == normalizedCategory }
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): Result<AuthResponseDto> = runCatching {
        apiService.signUp(
            AuthRequestDto(
                fullName = name,
                email = email,
                password = password,
                phone = "N/A",
                address = "N/A"
            )
        )
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<AuthResponseDto> = runCatching {
        apiService.login(
            AuthRequestDto(
                email = email,
                password = password
            )
        )
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthResponseDto> =
        socialLogin(idToken, "Google")

    override suspend fun loginWithFacebook(accessToken: String): Result<AuthResponseDto> =
        socialLogin(accessToken, "Facebook")

    private suspend fun socialLogin(token: String, provider: String): Result<AuthResponseDto> =
        runCatching {
            val email = "${provider.lowercase()}_${token.takeLast(10)}@shopsphere.app"
            val password = "${provider}_$token"

            // Try login first; if it fails, register then login
            try {
                apiService.login(AuthRequestDto(email = email, password = password))
            } catch (_: Exception) {
                apiService.signUp(
                    AuthRequestDto(
                        fullName = "$provider User",
                        email = email,
                        password = password
                    )
                )
                apiService.login(AuthRequestDto(email = email, password = password))
            }
        }

    override suspend fun addToCart(
        productId: Int,
        quantity: Int
    ): Result<CartMutationResponseDto> = runCatching {
        apiService.addToCart(
            AddToCartRequestDto(
                productId = productId,
                quantity = quantity
            )
        )
    }

    override suspend fun getCartItems(): Result<GetCartItemsResponseDto> = runCatching {
        apiService.getCartItems()
    }

    override suspend fun updateQuantity(
        productId: Int,
        newQuantity: Int
    ): Result<CartMutationResponseDto> = runCatching {
        apiService.updateQuantity(productId = productId, newQuantity = newQuantity)
    }

    override suspend fun removeFromCart(productId: Int): Result<CartMutationResponseDto> =
        runCatching { apiService.removeFromCart(productId) }

    override suspend fun clearCart(): Result<CartMutationResponseDto> = runCatching {
        apiService.clearCart()
    }

    override suspend fun addToFavorite(productId: Int): Result<FavoriteMutationResponseDto> = runCatching {
        apiService.addToFavorite(FavoriteRequestDto(productId = productId))
    }

    override suspend fun removeFromFavorite(productId: Int): Result<FavoriteMutationResponseDto> = runCatching {
        apiService.removeFromFavorite(FavoriteRequestDto(productId = productId))
    }

    override suspend fun getAllFavorites(): Result<List<FavoriteItemDto>> = runCatching {
        apiService.getAllFavorites()
    }

    private suspend fun fetchProductsFromBackend(): List<ProductResult> = coroutineScope {
        val categorizedResults = CATEGORY_ENDPOINTS.map { endpoint ->
            async {
                endpoint.displayName to runCatching {
                    apiService.getProductsByEndpoint(endpoint.endpoint)
                }.getOrElse { emptyList() }
            }
        }.awaitAll()

        val categoryByProductId = mutableMapOf<Int, String>()
        categorizedResults.forEach { (categoryName, products) ->
            products.forEach { dto ->
                categoryByProductId[dto.id] = overrideCategoryFromTitle(dto.name, categoryName)
            }
        }

        apiService.getAllProducts()
            .map { dto ->
                dto.toProductResult(
                    category = categoryByProductId[dto.id]
                        ?: overrideCategoryFromTitle(dto.name, GENERAL_CATEGORY)
                )
            }
            .sortedBy { it.id }
    }

    private fun BackendProductDto.toProductResult(category: String): ProductResult {
        val cleanTitle = name.trim().ifBlank { "Product #$id" }
        val sanitizedImage = image
            .orEmpty()
            .trim()
            .trim('"')

        return ProductResult(
            category = category,
            description = buildDescription(cleanTitle, category),
            id = id,
            image = sanitizedImage,
            price = price,
            rating = buildRating(id, category),
            title = cleanTitle,
            stock = (size ?: 0).coerceAtLeast(0)
        )
    }

    private fun buildDescription(title: String, category: String): String {
        return when (normalizeCategory(category)) {
            "beauty" -> "$title is part of our beauty picks for an easy everyday routine."
            "fragrances" -> "$title delivers a polished fragrance choice for daily and special moments."
            "furniture" -> "$title brings a refined furniture touch that fits modern home spaces."
            "groceries" -> "$title is available for fast online ordering and doorstep delivery."
            "home decoration" -> "$title adds a warm decorative accent to your home setup."
            "kitchen" -> "$title is a practical kitchen essential for daily use."
            "electronics" -> "$title is ready for work, study, and daily performance needs."
            "men's clothing", "womens clothing", "women's clothing" ->
                "$title is styled for a comfortable fit and easy online shopping."
            else -> "Shop $title online with smooth checkout and reliable delivery."
        }
    }

    private fun buildRating(productId: Int, category: String): Rating {
        val normalizedCategory = normalizeCategory(category)
        val baseRate = when (normalizedCategory) {
            "beauty" -> 4.7
            "fragrances" -> 4.8
            "furniture" -> 4.5
            "groceries" -> 4.4
            "electronics" -> 4.6
            else -> 4.5
        }
        val adjustedRate = (baseRate + ((productId % 3) * 0.1)).coerceAtMost(4.9)
        val reviewCount = 24 + ((productId * 9) % 140)
        return Rating(
            count = reviewCount,
            rate = adjustedRate
        )
    }

    private fun overrideCategoryFromTitle(title: String, fallback: String): String {
        val normalizedTitle = title.trim().lowercase()
        return when {
            normalizedTitle.contains("laptop") ||
                normalizedTitle.contains("macbook") ||
                normalizedTitle.contains("zenbook") -> "Electronics"

            normalizedTitle.contains("spatula") ||
                normalizedTitle.contains("whisk") ||
                normalizedTitle.contains("cup") -> "Kitchen"

            normalizedTitle.contains("lipstick") ||
                normalizedTitle.contains("palette") ||
                normalizedTitle.contains("nail") ||
                normalizedTitle.contains("eye") -> "Beauty"

            normalizedTitle.contains("dior") ||
                normalizedTitle.contains("chanel") ||
                normalizedTitle.contains("ck one") ||
                normalizedTitle.contains("eau") -> "Fragrances"

            normalizedTitle.contains("shirt") ||
                normalizedTitle.contains("tshirt") ||
                normalizedTitle.contains("t-shirt") ||
                normalizedTitle.contains("plaid") -> {
                if (normalizeCategory(fallback).contains("women")) "Women's Clothing" else "Men's Clothing"
            }

            else -> fallback
        }
    }

    private fun normalizeCategory(value: String): String {
        return value.trim().lowercase()
    }

    private data class CategoryEndpoint(
        val endpoint: String,
        val displayName: String
    )

    companion object {
        private const val GENERAL_CATEGORY = "General"
        private const val ALL_CATEGORY = "All"
        private val CACHE_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(5)

        private val CATEGORY_ENDPOINTS = listOf(
            CategoryEndpoint("Beauty_Products", "Beauty"),
            CategoryEndpoint("Fragrances_Products", "Fragrances"),
            CategoryEndpoint("Furniture_Products", "Furniture"),
            CategoryEndpoint("Groeries_Products", "Groceries"),
            CategoryEndpoint("HomeDecoration_Products", "Home Decoration"),
            CategoryEndpoint("Jewelery_Products", "Jewelery"),
            CategoryEndpoint("MenIsClothing_Products", "Men's Clothing"),
            CategoryEndpoint("SwomenIsClothing_Products", "Women's Clothing")
        )
    }
}
