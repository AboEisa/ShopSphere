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

    fun searchProducts(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = getProductsUseCase.getProducts()
            if (result.isSuccess) {
                val filteredProducts = result.getOrNull()?.filter {
                    it.title.contains(query, ignoreCase = true)
                }?.mapToPresentation()
                _searchResults.postValue(filteredProducts.orEmpty())
            } else {
                _searchResults.postValue(emptyList())
            }
        }
    }
}
