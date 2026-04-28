package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.domain.SearchProductsUseCase
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.models.mapToPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchProductsUseCase: SearchProductsUseCase
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<PresentationProductResult>>(emptyList())
    val searchResults: LiveData<List<PresentationProductResult>> = _searchResults

    /**
     * Server-side search via /Search?query=...
     *
     * The previous implementation cached the entire catalogue in-memory and
     * filtered by title client-side. That worked but ignored backend ranking
     * (synonyms, category boosts, brand matches). Switching to the dedicated
     * endpoint lets the server own relevance and pagination later on.
     */
    fun searchProducts(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchResults.postValue(emptyList())
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val results = searchProductsUseCase(trimmed)
                .getOrNull()
                .orEmpty()
                .mapToPresentation()
            _searchResults.postValue(results)
        }
    }
}
