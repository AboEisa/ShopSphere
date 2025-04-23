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


    fun saveCartProducts(products: Map<Int, Int>) {
        val json = gson.toJson(products)
        sharedPref.edit().putString("cart_products", json).apply()// Use commit() for immediate write
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
            currentCart[productId] = currentCart[productId]!! + 1 // زيادة الكمية إذا كان المنتج موجود
        } else {
            currentCart[productId] = 1 // إذا المنتج غير موجود نضيفه بكمية 1
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
