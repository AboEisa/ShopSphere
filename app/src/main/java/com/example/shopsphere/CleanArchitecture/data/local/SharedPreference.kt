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
    private val sharedPref: SharedPreferences by lazy {
        context.getSharedPreferences("FAVORITE_PRODUCTS_PREF", Context.MODE_PRIVATE)
    }

    private val gson = Gson()
    private val _changes = MutableSharedFlow<Unit>(replay = 1)
    val changes = _changes.asSharedFlow()

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
        sharedPref.edit().remove("uid").apply()
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
        _changes.tryEmit(Unit)
    }

    // ----------------------------------------------------------
    // FAVORITE PRODUCTS
    // ----------------------------------------------------------
    fun saveFavoriteProducts(products: List<Int>) {
        val json = gson.toJson(products)
        sharedPref.edit().putString("favorite_products", json).apply()
        _changes.tryEmit(Unit)
    }

    fun getFavoriteProducts(): List<Int> {
        val json = sharedPref.getString("favorite_products", null)
        return if (json != null) {
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
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
    fun saveCartProducts(products: Map<Int, Int>) {
        val json = gson.toJson(products)
        sharedPref.edit().putString("cart_products", json).apply()
        _changes.tryEmit(Unit)
    }

    fun getCartProducts(): Map<Int, Int> {
        val json = sharedPref.getString("cart_products", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<Map<Int, Int>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    fun addCartProduct(productId: Int) {
        val currentCart = getCartProducts().toMutableMap()
        if (currentCart.containsKey(productId)) {
            currentCart[productId] = currentCart[productId]!! + 1
        } else {
            currentCart[productId] = 1
        }
        saveCartProducts(currentCart)
    }

    fun removeCartProduct(productId: Int) {
        val currentCart = getCartProducts().toMutableMap()
        if (currentCart.containsKey(productId)) {
            currentCart.remove(productId)
            saveCartProducts(currentCart)
        }
    }

    fun updateQuantity(productId: Int, newQuantity: Int) {
        val currentCart = getCartProducts().toMutableMap()
        if (currentCart.containsKey(productId)) {
            currentCart[productId] = newQuantity
            saveCartProducts(currentCart)
        }
    }
}