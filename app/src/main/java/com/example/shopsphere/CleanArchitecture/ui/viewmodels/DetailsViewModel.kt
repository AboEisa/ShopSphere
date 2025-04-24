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
        viewModelScope.launch(Dispatchers.IO) {
            val result = getProductsUseCase.getProducts()
            if (result.isSuccess) {
                val products = result.getOrNull()?.mapToPresentation()
                val product = products?.find { it.id == productId }
                _productLiveData.postValue(product)
            } else {
                Log.e("DetailsViewModel", "Error fetching product: ${result.exceptionOrNull()?.message}")
                _productLiveData.postValue(null)
            }
        }
    }
}