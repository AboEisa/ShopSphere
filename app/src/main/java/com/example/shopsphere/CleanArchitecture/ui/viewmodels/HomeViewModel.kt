package com.example.yourpackage.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.domain.GetProductsUseCase
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.models.mapToPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _productsLiveData = MutableLiveData<List<PresentationProductResult>>()
    val productsLiveData: LiveData<List<PresentationProductResult>> = _productsLiveData

    private val _filteredProductsLiveData = MutableLiveData<List<PresentationProductResult>>()
    val filteredProductsLiveData: LiveData<List<PresentationProductResult>> = _filteredProductsLiveData

    private val _categoriesLiveData = MutableLiveData<List<String>>(listOf(ALL_CATEGORY))
    val categoriesLiveData: LiveData<List<String>> = _categoriesLiveData

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _hasLoadedOnce = MutableLiveData(false)
    val hasLoadedOnce: LiveData<Boolean> = _hasLoadedOnce

    private var allProductsCache: List<PresentationProductResult> = emptyList()
    private var selectedCategory: String = ALL_CATEGORY
    private var activePriceRange: Pair<Float, Float>? = null
    private var favoriteIds: Set<Int> = emptySet()
    private var recentIds: List<Int> = emptyList()

    init {
        fetchProducts()
    }

    fun fetchProducts() {
        selectedCategory = ALL_CATEGORY
        activePriceRange = null
        loadAllProducts()
    }

    fun fetchProductsByCategory(category: String) {
        selectedCategory = category.ifBlank { ALL_CATEGORY }
        if (allProductsCache.isEmpty()) {
            loadAllProducts()
        } else {
            publishVisibleProducts()
        }
    }

    fun filterProducts(minPrice: Float, maxPrice: Float) {
        activePriceRange = minPrice to maxPrice
        publishVisibleProducts()
    }

    fun clearPriceFilter() {
        activePriceRange = null
        publishVisibleProducts()
    }

    fun setInterestSignals(favoriteIds: Set<Int>, recentIds: List<Int>) {
        val favChanged = favoriteIds != this.favoriteIds
        val recentChanged = recentIds != this.recentIds
        if (!favChanged && !recentChanged) return
        this.favoriteIds = favoriteIds
        this.recentIds = recentIds
        if (allProductsCache.isNotEmpty()) publishVisibleProducts()
    }

    private fun loadAllProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                val result = getProductsUseCase.getProducts()
                if (result.isSuccess) {
                    allProductsCache = result.getOrNull()?.mapToPresentation().orEmpty()
                    publishCategories()
                    publishVisibleProducts()
                } else {
                    Log.e(
                        TAG,
                        "Error fetching products: ${result.exceptionOrNull()?.message}"
                    )
                    allProductsCache = emptyList()
                    _categoriesLiveData.postValue(listOf(ALL_CATEGORY))
                    _productsLiveData.postValue(emptyList())
                    _filteredProductsLiveData.postValue(emptyList())
                }
            } finally {
                _hasLoadedOnce.postValue(true)
                _isLoading.postValue(false)
            }
        }
    }

    private fun publishCategories() {
        val dynamicCategories = allProductsCache
            .map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

        _categoriesLiveData.postValue(listOf(ALL_CATEGORY) + dynamicCategories)
    }

    private fun publishVisibleProducts() {
        viewModelScope.launch(Dispatchers.Default) {
            val categoryFiltered = if (selectedCategory.equals(ALL_CATEGORY, ignoreCase = true)) {
                allProductsCache
            } else {
                allProductsCache.filter {
                    it.category.trim().equals(selectedCategory.trim(), ignoreCase = true)
                }
            }

            val priceFiltered = activePriceRange?.let { range ->
                categoryFiltered.filter { product ->
                    product.price.toFloat() in range.first..range.second
                }
            } ?: categoryFiltered

            val ranked = rankByInterests(priceFiltered)

            _productsLiveData.postValue(ranked)
            _filteredProductsLiveData.postValue(ranked)
            _hasLoadedOnce.postValue(true)
        }
    }

    /**
     * Reranks products so items matching the user's interest signals surface first.
     * Only applies when viewing the "All" category — category-filtered views keep
     * their natural order so the user sees everything in that category.
     */
    private fun rankByInterests(
        products: List<PresentationProductResult>
    ): List<PresentationProductResult> {
        if (!selectedCategory.equals(ALL_CATEGORY, ignoreCase = true)) return products
        if (favoriteIds.isEmpty() && recentIds.isEmpty()) return products

        val byId = allProductsCache.associateBy { it.id }
        val favCategories = favoriteIds
            .mapNotNull { byId[it]?.category?.trim()?.lowercase()?.takeIf { c -> c.isNotBlank() } }
            .toSet()
        val recentCategories = recentIds
            .mapNotNull { byId[it]?.category?.trim()?.lowercase()?.takeIf { c -> c.isNotBlank() } }
            .toSet()
        return products
            .withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<PresentationProductResult>> { (_, p) ->
                    var score = 0
                    val cat = p.category.trim().lowercase()
                    if (cat in favCategories) score += 2
                    if (cat in recentCategories) score += 1
                    if (p.id in favoriteIds) score += 3
                    score
                }.thenBy { it.index }
            )
            .map { it.value }
    }

    companion object {
        private const val TAG = "HomeViewModel"
        const val ALL_CATEGORY = "All"
    }
}
