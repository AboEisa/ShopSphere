package com.example.shopsphere.CleanArchitecture.ui.viewmodels

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
class DetailsViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _productLiveData = MutableLiveData<PresentationProductResult?>()
    val productLiveData: LiveData<PresentationProductResult?> = _productLiveData

    fun fetchProductById(productId: Int) {
        // Skip work if we already have this product loaded.
        if (_productLiveData.value?.id == productId) return

        viewModelScope.launch(Dispatchers.IO) {
            val result = getProductsUseCase.getProducts()
            if (result.isSuccess) {
                val product = result.getOrNull()
                    ?.firstOrNull { it.id == productId }
                    ?.let { listOf(it).mapToPresentation().firstOrNull() }
                _productLiveData.postValue(product)
            } else {
                Log.e("DetailsViewModel", "Error fetching product: ${result.exceptionOrNull()?.message}")
                _productLiveData.postValue(null)
            }
        }
    }
}