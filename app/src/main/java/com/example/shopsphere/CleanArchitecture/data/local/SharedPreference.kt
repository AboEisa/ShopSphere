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
    private val _changes = MutableSharedFlow<Unit>()
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
}
