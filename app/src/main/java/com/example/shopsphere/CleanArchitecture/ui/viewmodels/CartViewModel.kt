package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coroutines.CleanArchitecture.domain.GetFavoriteProductsUseCase
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.domain.AddToCartUseCase
import com.example.shopsphere.CleanArchitecture.domain.GetCardProductsUseCase
import com.example.shopsphere.CleanArchitecture.domain.IRepository

import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.models.mapToPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repository: IRepository,
    private val getCardProductsUseCase: GetCardProductsUseCase,
    private val sharedPreference: SharedPreference
) : ViewModel() {
    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount

    init {
        loadCartItemCount()
    }

    private fun loadCartItemCount() {
        viewModelScope.launch {
            try {
                val count = sharedPreference.getCartItemCount()
                _cartItemCount.value = count
            } catch (e: Exception) {
                Log.e("CartViewModel", "loadCartItemCount error", e)
                _cartItemCount.value = 0
            }
        }
    }

    fun refreshCartCount() {
        loadCartItemCount()
    }

    private val _cartProducts = MutableLiveData<List<PresentationProductResult>>()
    val cartProducts: LiveData<List<PresentationProductResult>> = _cartProducts

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _emptyState = MutableLiveData(false)
    val emptyState: LiveData<Boolean> = _emptyState

    private val _totalPrice = MutableLiveData<Double>()
    val totalPrice: LiveData<Double> = _totalPrice

    init {
        loadCartProducts()
        observePreferenceChanges()
    }

    private fun observePreferenceChanges() {
        viewModelScope.launch {
            sharedPreference.changes.collect {
                loadCartProducts()
            }
        }
    }

    fun loadCartProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val cartIds = sharedPreference.getCartProducts()
                if (cartIds.isEmpty()) {
                    _cartProducts.postValue(emptyList())
                    _emptyState.postValue(true)
                    _totalPrice.postValue(0.0) // Clear total price if the cart is empty
                } else {
                    val result = getCardProductsUseCase(cartIds.keys.toList())
                    if (result.isSuccess) {
                        val cartProductList = result.getOrNull()?.map { domainProduct ->
                            val presentationProduct = domainProduct.mapToPresentation()
                            presentationProduct.copy(quantity = cartIds[domainProduct.id] ?: 1)
                        }
                        _cartProducts.postValue(cartProductList.orEmpty())
                        _emptyState.postValue(cartProductList.isNullOrEmpty())
                        updateTotalPrice(cartProductList.orEmpty()) // Update total price
                    } else {
                        _emptyState.postValue(true)
                        _totalPrice.postValue(0.0) // Reset total price in case of error
                    }
                }
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun updateTotalPrice(cartProducts: List<PresentationProductResult>) {
        val total = cartProducts.sumOf { product ->
            (product.price * (product.quantity ?: 1))
        }
        _totalPrice.postValue(total)
    }

    fun addProductToCart(productId: Int, availableStock: Int): Boolean {
        return try {
            val cartMap = sharedPreference.getCartProducts()
            val currentQuantity = cartMap[productId] ?: 0
            val stock = availableStock.coerceAtLeast(0)

            if (stock <= 0 || currentQuantity >= stock) {
                return false
            }

            sharedPreference.addCartProduct(productId)
            loadCartItemCount()
            true
        } catch (e: Exception) {
            Log.e("CartViewModel", "addProductToCart error", e)
            false
        }
    }

    fun removeFromCart(productId: Int) {
        try {
            sharedPreference.removeCartProduct(productId)
            loadCartItemCount()
        } catch (e: Exception) {
            Log.e("CartViewModel", "removeFromCart error", e)
        }
    }

    fun updateQuantity(productId: Int, newQuantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreference.updateQuantity(productId, newQuantity)
            loadCartProducts() // Reload the products to update total price
        }
    }

    fun isInCart(productId: Int): Boolean {
        return try {
            val cart = sharedPreference.getCartProducts()
            cart.containsKey(productId).also {
                Log.d("CartViewModel", "isInCart: $productId, result: $it")
            }
        } catch (e: Exception) {
            Log.e("CartViewModel", "isInCart error", e)
            false
        }
    }
}
