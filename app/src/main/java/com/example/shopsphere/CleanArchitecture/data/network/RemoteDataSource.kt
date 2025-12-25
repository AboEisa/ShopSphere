package com.example.shopsphere.CleanArchitecture.data.network



import com.example.shopsphere.CleanArchitecture.data.models.ProductResult
import com.example.shopsphere.CleanArchitecture.data.models.Rating
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlin.math.absoluteValue
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dummyApiService: DummyApiServices
) : IRemoteDataSource {

    override suspend fun getProducts(): Result<List<ProductResult>> {
        return runCatching {
            val collection = firestore.collection(PRODUCTS_COLLECTION)
            val snapshot = runCatching { collection.get(Source.SERVER).await() }
                .getOrElse { collection.get().await() }

            if (snapshot.isEmpty) {
                val seededProducts = dummyApiService.getProducts().products
                val batch = firestore.batch()
                seededProducts.forEach { product ->
                    val document = collection.document(product.id.toString())
                    batch.set(document, product.toFirestoreMap())
                }
                batch.commit().await()
                seededProducts.map { it.toProductResult() }
            } else {
                snapshot.documents
                    .mapNotNull { document -> document.toProductResult() }
                    .sortedBy { it.id }
            }
        }

    }

    override suspend fun getProductsByCategory(category: String): Result<List<ProductResult>> {
        return runCatching {
            getProducts()
                .getOrThrow()
                .filter { normalizeCategory(it.category) == normalizeCategory(category) }
        }
    }

//    override suspend fun addToCart(cart: AddToCartRequest): Result<List<CartProduct>> {
//        return try {
//            val data = apiService.addToCart(cart)
//            Result.success(data)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }


    private fun DummyProductItem.toProductResult(): ProductResult {
        val mappedCategory = mapCategory(category)
        return ProductResult(
            category = mappedCategory,
            description = description,
            id = id,
            image = thumbnail ?: images?.firstOrNull().orEmpty(),
            price = price,
            rating = Rating(
                count = stock ?: 0,
                rate = rating ?: 0.0
            ),
            title = title
        )
    }

    private fun DummyProductItem.toFirestoreMap(): Map<String, Any> {
        val mappedCategory = mapCategory(category)
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "category" to mappedCategory,
            "price" to price,
            "thumbnail" to (thumbnail ?: images?.firstOrNull().orEmpty()),
            "image" to (thumbnail ?: images?.firstOrNull().orEmpty()),
            "images" to (images ?: emptyList<String>()),
            "ratingRate" to (rating ?: 0.0),
            "ratingCount" to (stock ?: 0),
            "stock" to (stock ?: 0),
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "seededFromDummyJsonAt" to Timestamp.now()
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toProductResult(): ProductResult? {
        val isActive = getBoolean("isActive") ?: true
        if (!isActive) return null

        val title = getString("title") ?: getString("name") ?: return null
        val description = getString("description").orEmpty()
        val category = mapCategory(getString("category").orEmpty().ifBlank { "General" })
        val price = getDouble("price")
            ?: getLong("price")?.toDouble()
            ?: 0.0

        val image = getString("image")
            ?: getString("thumbnail")
            ?: (get("images") as? List<*>)?.firstOrNull()?.toString()
            ?: ""

        val ratingRate = getDouble("ratingRate")
            ?: getDouble("rating")
            ?: 0.0
        val stockCount = getLong("stock")?.toInt()
            ?: getLong("ratingCount")?.toInt()
            ?: getLong("inventory")?.toInt()
            ?: 0

        val productId = getLong("id")?.toInt() ?: id.hashCode().absoluteValue

        return ProductResult(
            category = category,
            description = description,
            id = productId,
            image = image,
            price = price,
            rating = Rating(
                count = stockCount.coerceAtLeast(0),
                rate = ratingRate
            ),
            title = title
        )
    }

    private fun mapCategory(rawCategory: String): String {
        val cleaned = rawCategory.trim()
        if (cleaned.isBlank()) return "General"

        return cleaned
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                }
            }
    }

    private fun normalizeCategory(category: String): String = category.trim().lowercase()

    companion object {
        private const val PRODUCTS_COLLECTION = "products"
    }

}
