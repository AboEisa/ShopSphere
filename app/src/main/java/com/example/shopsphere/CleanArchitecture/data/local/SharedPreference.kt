package com.example.shopsphere.CleanArchitecture.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class CartLineStorage(
        val lineId: String,
        val productId: Int,
        val size: String,
        val quantity: Int
    )

    private val sharedPref: SharedPreferences by lazy {
        context.getSharedPreferences("FAVORITE_PRODUCTS_PREF", Context.MODE_PRIVATE)
    }

    private val gson = Gson()
    private val _changes = MutableSharedFlow<Unit>(replay = 1)
    val changes = _changes.asSharedFlow()

    // In-memory caches — avoid re-parsing JSON on every call. These are the hot path
    // for RecyclerView rebinds (isFavorite / isInCart run once per item per scroll).
    @Volatile
    private var cachedFavoriteIds: List<Int>? = null

    @Volatile
    private var cachedCartLines: List<CartLineStorage>? = null

    // ----------------------------------------------------------
    // AUTH / UID MANAGEMENT
    // ----------------------------------------------------------
    fun saveUid(uid: String) {
        sharedPref.edit().putString("uid", uid).apply()
    }

    fun getUid(): String {
        return sharedPref.getString("uid", "") ?: ""
    }

    fun clearUid() {
        sharedPref.edit().remove("uid").remove("customer_id").remove("auth_token").apply()
        cachedFavoriteIds = null
        cachedCartLines = null
    }

    fun saveToken(token: String) {
        sharedPref.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String {
        return sharedPref.getString("auth_token", "") ?: ""
    }

    fun saveCustomerId(customerId: String) {
        sharedPref.edit().putString("customer_id", customerId).apply()
    }

    fun getCustomerId(): String {
        return sharedPref.getString("customer_id", "") ?: ""
    }

    fun saveProfile(name: String, email: String, phone: String) {
        sharedPref.edit()
            .putString("profile_name", name)
            .putString("profile_email", email)
            .putString("profile_phone", phone)
            .apply()
    }

    fun saveProfileExtras(birthDate: String, gender: String) {
        sharedPref.edit()
            .putString("profile_birth_date", birthDate)
            .putString("profile_gender", gender)
            .apply()
    }

    fun getProfileName(): String = sharedPref.getString("profile_name", "") ?: ""
    fun getProfileEmail(): String = sharedPref.getString("profile_email", "") ?: ""
    fun getProfilePhone(): String = sharedPref.getString("profile_phone", "") ?: ""
    fun getProfileBirthDate(): String = sharedPref.getString("profile_birth_date", "12/07/1990") ?: "12/07/1990"
    fun getProfileGender(): String = sharedPref.getString("profile_gender", "Male") ?: "Male"

    fun saveNotificationPreference(key: String, enabled: Boolean) {
        sharedPref.edit().putBoolean("notification_pref_$key", enabled).apply()
    }

    fun getNotificationPreference(key: String, defaultValue: Boolean): Boolean {
        return sharedPref.getBoolean("notification_pref_$key", defaultValue)
    }

    // ----------------------------------------------------------
    // LOGIN STATE (CRITICAL FIX)
    // ----------------------------------------------------------
    fun saveIsLoggedIn(isLoggedIn: Boolean) {
        sharedPref.edit().putBoolean("is_logged_in", isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPref.getBoolean("is_logged_in", false)
    }

    // ----------------------------------------------------------
    // CLEAR ALL DATA (for logout)
    // ----------------------------------------------------------
    fun clear() {
        sharedPref.edit().clear().apply()
        cachedFavoriteIds = null
        cachedCartLines = null
        _changes.tryEmit(Unit)
    }

    // ----------------------------------------------------------
    // FAVORITE PRODUCTS
    // ----------------------------------------------------------
    fun saveFavoriteProducts(products: List<Int>) {
        val snapshot = products.toList()
        cachedFavoriteIds = snapshot
        val json = gson.toJson(snapshot)
        sharedPref.edit().putString("favorite_products", json).apply()
        _changes.tryEmit(Unit)
    }

    fun getFavoriteProducts(): List<Int> {
        cachedFavoriteIds?.let { return it }
        val json = sharedPref.getString("favorite_products", null)
        val parsed = if (json != null) {
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson<List<Int>>(json, type) ?: emptyList()
        } else {
            emptyList()
        }
        cachedFavoriteIds = parsed
        return parsed
    }

    fun addFavoriteProduct(productId: Int) {
        val currentFavorites = getFavoriteProducts().toMutableList()
        if (!currentFavorites.contains(productId)) {
            currentFavorites.add(productId)
            saveFavoriteProducts(currentFavorites)
        }
    }

    fun removeFavoriteProduct(productId: Int) {
        val currentFavorites = getFavoriteProducts().toMutableList()
        if (currentFavorites.contains(productId)) {
            currentFavorites.remove(productId)
            saveFavoriteProducts(currentFavorites)
        }
    }

    fun isFavorite(productId: Int): Boolean {
        return getFavoriteProducts().contains(productId)
    }

    // ----------------------------------------------------------
    // CART PRODUCTS
    // ----------------------------------------------------------
    fun saveCartLines(lines: List<CartLineStorage>) {
        val normalized = lines
            .filter { it.productId > 0 && it.quantity > 0 }
            .map { line ->
                val size = normalizeSize(line.size)
                line.copy(
                    lineId = line.lineId.ifBlank { buildCartLineId(line.productId, size) },
                    size = size,
                    quantity = line.quantity.coerceAtLeast(1)
                )
            }

        cachedCartLines = normalized
        val json = gson.toJson(normalized)
        sharedPref.edit()
            .putString(KEY_CART_LINES, json)
            .remove(KEY_LEGACY_CART_PRODUCTS)
            .apply()
        _changes.tryEmit(Unit)
    }

    fun getCartLines(): List<CartLineStorage> {
        cachedCartLines?.let { return it }
        val json = sharedPref.getString(KEY_CART_LINES, null)
        if (!json.isNullOrBlank()) {
            val parsed = try {
                val type = object : TypeToken<List<CartLineStorage>>() {}.type
                gson.fromJson<List<CartLineStorage>>(json, type).orEmpty()
                    .filter { it.productId > 0 && it.quantity > 0 }
                    .map { line ->
                        val size = normalizeSize(line.size)
                        line.copy(
                            lineId = line.lineId.ifBlank { buildCartLineId(line.productId, size) },
                            size = size,
                            quantity = line.quantity.coerceAtLeast(1)
                        )
                    }
            } catch (_: Exception) {
                emptyList()
            }
            cachedCartLines = parsed
            return parsed
        }

        val legacy = getLegacyCartProducts().map { (productId, quantity) ->
            CartLineStorage(
                lineId = buildCartLineId(productId, ""),
                productId = productId,
                size = "",
                quantity = quantity.coerceAtLeast(1)
            )
        }
        cachedCartLines = legacy
        return legacy
    }

    fun saveCartProducts(products: Map<Int, Int>) {
        saveCartLines(
            products.map { (productId, quantity) ->
                CartLineStorage(
                    lineId = buildCartLineId(productId, ""),
                    productId = productId,
                    size = "",
                    quantity = quantity.coerceAtLeast(1)
                )
            }
        )
    }

    fun getCartProducts(): Map<Int, Int> {
        return getCartLines()
            .groupBy { it.productId }
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
    }

    fun addCartProduct(productId: Int, size: String) {
        val normalizedSize = normalizeSize(size)
        val updated = getCartLines().toMutableList()
        val index = if (normalizedSize.isBlank()) {
            updated.indexOfFirst { it.productId == productId }
        } else {
            val lineId = buildCartLineId(productId, normalizedSize)
            updated.indexOfFirst { it.lineId == lineId }
        }
        if (index >= 0) {
            val current = updated[index]
            updated[index] = current.copy(quantity = current.quantity + 1)
        } else {
            updated.add(
                CartLineStorage(
                    lineId = buildCartLineId(productId, normalizedSize),
                    productId = productId,
                    size = normalizedSize,
                    quantity = 1
                )
            )
        }
        saveCartLines(updated)
    }

    fun addCartProduct(productId: Int) = addCartProduct(productId, "")

    fun isInCart(productId: Int, size: String): Boolean {
        val normalizedSize = normalizeSize(size)
        return if (normalizedSize.isBlank()) {
            getCartLines().any { it.productId == productId }
        } else {
            val lineId = buildCartLineId(productId, normalizedSize)
            getCartLines().any { it.lineId == lineId }
        }
    }

    fun removeCartProduct(productId: Int, size: String) {
        val normalizedSize = normalizeSize(size)
        if (normalizedSize.isBlank()) {
            saveCartLines(getCartLines().filterNot { it.productId == productId })
        } else {
            removeCartProductByLineId(buildCartLineId(productId, normalizedSize))
        }
    }

    fun removeCartProduct(productId: Int) {
        saveCartLines(getCartLines().filterNot { it.productId == productId })
    }

    fun removeCartProductByLineId(lineId: String) {
        saveCartLines(getCartLines().filterNot { it.lineId == lineId })
    }

    fun updateQuantity(productId: Int, size: String, newQuantity: Int) {
        val normalizedSize = normalizeSize(size)
        if (normalizedSize.isBlank()) {
            val lineId = getCartLines().firstOrNull { it.productId == productId }?.lineId ?: return
            updateQuantity(lineId, newQuantity)
        } else {
            updateQuantity(buildCartLineId(productId, normalizedSize), newQuantity)
        }
    }

    fun updateQuantity(lineId: String, newQuantity: Int) {
        val updated = getCartLines().mapNotNull { line ->
            if (line.lineId != lineId) {
                line
            } else if (newQuantity <= 0) {
                null
            } else {
                line.copy(quantity = newQuantity.coerceAtLeast(1))
            }
        }
        saveCartLines(updated)
    }

    fun clearCartProducts() {
        sharedPref.edit()
            .remove(KEY_CART_LINES)
            .remove(KEY_LEGACY_CART_PRODUCTS)
            .apply()
        cachedCartLines = emptyList()
        _changes.tryEmit(Unit)
    }

    fun getCartItemCount(): Int {
        return getCartLines().sumOf { it.quantity }
    }

    private fun getLegacyCartProducts(): Map<Int, Int> {
        val json = sharedPref.getString(KEY_LEGACY_CART_PRODUCTS, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val raw = gson.fromJson<Map<String, Int>>(json, type) ?: emptyMap()
            raw.mapKeys { (k, _) -> k.toIntOrNull() ?: -1 }.filterKeys { it >= 0 }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun normalizeSize(size: String): String {
        return size.trim().uppercase()
    }

    private fun buildCartLineId(productId: Int, size: String): String {
        val normalizedSize = normalizeSize(size)
        return "$productId:${normalizedSize.ifBlank { NO_SIZE_TOKEN }}"
    }

    private companion object {
        private const val KEY_CART_LINES = "cart_lines"
        private const val KEY_LEGACY_CART_PRODUCTS = "cart_products"
        private const val NO_SIZE_TOKEN = "NO_SIZE"
    }
}
