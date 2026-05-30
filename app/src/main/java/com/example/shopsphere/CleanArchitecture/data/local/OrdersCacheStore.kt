package com.example.shopsphere.CleanArchitecture.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.shopsphere.CleanArchitecture.domain.DomainOrder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-disk cache for the user's order history. Lets the orders / track
 * screens render the last-known list instantly, while the repository
 * refreshes from the backend in the background.
 *
 * Driver location and order status do go stale, so anything time-sensitive
 * (live tracking) should still wait for the fresh response — caching here
 * is purely to avoid the blank-screen feel on launch.
 */
@Singleton
class OrdersCacheStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    fun load(): List<DomainOrder> {
        val json = prefs.getString(KEY_PAYLOAD, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<DomainOrder>>() {}.type
            gson.fromJson<List<DomainOrder>>(json, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun save(orders: List<DomainOrder>) {
        prefs.edit()
            .putString(KEY_PAYLOAD, gson.toJson(orders))
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    fun lastSavedAt(): Long = prefs.getLong(KEY_SAVED_AT, 0L)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREF_NAME = "ORDERS_CACHE_PREF"
        const val KEY_PAYLOAD = "orders_payload"
        const val KEY_SAVED_AT = "orders_saved_at"
    }
}
