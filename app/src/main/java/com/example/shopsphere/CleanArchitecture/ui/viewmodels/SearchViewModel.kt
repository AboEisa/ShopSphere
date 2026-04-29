package com.example.shopsphere.CleanArchitecture.ui.viewmodels

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
class SearchViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<PresentationProductResult>>(emptyList())
    val searchResults: LiveData<List<PresentationProductResult>> = _searchResults

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /** Full catalogue cached after the first load — no extra network call per keystroke. */
    private var allProducts: List<PresentationProductResult> = emptyList()

    fun clearResults() {
        _searchResults.value = emptyList()
    }

    fun searchProducts(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        // If we already have the catalogue, filter synchronously — instant, no spinner.
        if (allProducts.isNotEmpty()) {
            _searchResults.value = filter(allProducts, trimmed)
            return
        }

        // First call: fetch catalogue once, then filter.
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val products = getProductsUseCase.getProducts()
                .getOrNull()
                .orEmpty()
                .mapToPresentation()
            allProducts = products
            _searchResults.postValue(filter(products, trimmed))
            _isLoading.postValue(false)
        }
    }

    private fun filter(
        products: List<PresentationProductResult>,
        query: String
    ): List<PresentationProductResult> {
        val q = query.lowercase()
        return products.filter { it.title.lowercase().contains(q) }
    }
}