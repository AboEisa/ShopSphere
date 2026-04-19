package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.GetProductsUseCase
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.models.mapToPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<PresentationProductResult>>(emptyList())
    val searchResults: LiveData<List<PresentationProductResult>> = _searchResults

    @Volatile
    private var cachedProducts: List<DomainProductResult>? = null
    private val cacheMutex = Mutex()

    fun searchProducts(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val products = loadProducts() ?: run {
                _searchResults.postValue(emptyList())
                return@launch
            }
            val filtered = products
                .filter { it.title.contains(query, ignoreCase = true) }
                .mapToPresentation()
            _searchResults.postValue(filtered)
        }
    }

    private suspend fun loadProducts(): List<DomainProductResult>? {
        cachedProducts?.let { return it }
        return cacheMutex.withLock {
            cachedProducts ?: getProductsUseCase.getProducts().getOrNull()?.also {
                cachedProducts = it
            }
        }
    }
}
