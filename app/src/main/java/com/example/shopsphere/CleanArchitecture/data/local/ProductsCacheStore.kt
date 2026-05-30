package com.example.shopsphere.CleanArchitecture.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-disk cache for the product catalog so the home / search / details
 * screens can render immediately on launch instead of waiting on the
 * (sometimes slow) ngrok backend. Refreshed in the background via the
 * repository's observeProducts() flow.
 *
 * Backed by a Gson-encoded blob in SharedPreferences — fine because the
 * catalog is small (a few hundred items at most). For something larger
 * we'd want Room.
 */
@Singleton
class ProductsCacheStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    fun load(): List<DomainProductResult> {
        val json = prefs.getString(KEY_PAYLOAD, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<DomainProductResult>>() {}.type
            gson.fromJson<List<DomainProductResult>>(json, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun save(products: List<DomainProductResult>) {
        if (products.isEmpty()) return
        prefs.edit()
            .putString(KEY_PAYLOAD, gson.toJson(products))
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    /** Last successful refresh time in epoch ms, or 0 if never. */
    fun lastSavedAt(): Long = prefs.getLong(KEY_SAVED_AT, 0L)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREF_NAME = "PRODUCTS_CACHE_PREF"
        const val KEY_PAYLOAD = "products_payload"
        const val KEY_SAVED_AT = "products_saved_at"
    }
}
