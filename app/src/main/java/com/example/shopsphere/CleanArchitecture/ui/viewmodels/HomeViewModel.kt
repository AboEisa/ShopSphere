package com.example.yourpackage.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.domain.GetProductsByCategoryUseCase
import com.example.shopsphere.CleanArchitecture.domain.GetProductsUseCase
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.models.mapToPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val getProductsByCategoryUseCase: GetProductsByCategoryUseCase
) : ViewModel() {

    private val _productsLiveData = MutableLiveData<List<PresentationProductResult>>()
    val productsLiveData: LiveData<List<PresentationProductResult>> = _productsLiveData



    init {
        fetchProducts()
    }

     fun fetchProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = getProductsUseCase.getProducts()
            if (result.isSuccess) {
                _productsLiveData.postValue(result.getOrNull()?.mapToPresentation())
            } else {
                Log.e("HomeViewModel", "Error fetching products: ${result.exceptionOrNull()?.message}")
                _productsLiveData.postValue(emptyList())
            }
        }
    }

    fun fetchProductsByCategory(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = getProductsByCategoryUseCase.getProductsByCategory(category)
            if (result.isSuccess) {
                _productsLiveData.postValue(result.getOrNull()?.mapToPresentation())
            } else {
                Log.e("HomeViewModel", "Error fetching products by category: ${result.exceptionOrNull()?.message}")
                _productsLiveData.postValue(emptyList())
            }
        }
    }




    private val _filteredProductsLiveData = MutableLiveData<List<PresentationProductResult>>()
    val filteredProductsLiveData: LiveData<List<PresentationProductResult>> = _filteredProductsLiveData

    fun filterProducts(minPrice: Float, maxPrice: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = getProductsUseCase.getProducts()
            if (result.isSuccess) {
                val filteredProducts = result.getOrNull()?.filter { product ->
                    product.price in minPrice..maxPrice
                }
                _filteredProductsLiveData.postValue(filteredProducts?.mapToPresentation())
            }
        }
    }


//    fun searchProducts(query: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val result = getProductsUseCase.getProducts()
//            if (result.isSuccess) {
//                val filteredProducts = result.getOrNull()?.filter { product ->
//                    product.title.contains(query, ignoreCase = true)
//                }
//                _filteredProductsLiveData.postValue(filteredProducts?.mapToPresentation())
//            }
//        }
//    }

//    fun sortProductsByPrice(ascending: Boolean) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val result = getProductsUseCase.getProducts()
//            if (result.isSuccess) {
//                val sortedProducts = result.getOrNull()?.sortedBy { product ->
//                    if (ascending) product.price else -product.price
//                }
//                _filteredProductsLiveData.postValue(sortedProducts?.mapToPresentation())
//            }
//        }
//    }

//    fun sortProductsByRating(ascending: Boolean) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val result = getProductsUseCase.getProducts()
//            if (result.isSuccess) {
//                val sortedProducts = result.getOrNull()?.sortedBy { product ->
//                    if (ascending) product.rating.rate else -product.rating.rate
//                }
//                _filteredProductsLiveData.postValue(sortedProducts?.mapToPresentation())
//            }
//        }
//    }


}
