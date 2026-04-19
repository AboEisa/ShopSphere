package com.example.shopsphere.CleanArchitecture.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentlyViewedStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    private val _ids = MutableStateFlow(loadFromDisk())
    val ids: StateFlow<List<Int>> = _ids.asStateFlow()

    /** Move-to-front: bumps a just-viewed product to the head of the list. */
    fun record(productId: Int) {
        if (productId <= 0) return
        val updated = buildList {
            add(productId)
            _ids.value.forEach { if (it != productId) add(it) }
        }.take(MAX_SIZE)
        _ids.value = updated
        persist(updated)
    }

    fun clear() {
        _ids.value = emptyList()
        prefs.edit().remove(KEY).apply()
    }

    private fun loadFromDisk(): List<Int> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson<List<Int>>(json, type).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<Int>) {
        prefs.edit().putString(KEY, gson.toJson(list)).apply()
    }

    private companion object {
        const val PREF_NAME = "RECENTLY_VIEWED_PREF"
        const val KEY = "recently_viewed_ids"
        const val MAX_SIZE = 20
    }
}
