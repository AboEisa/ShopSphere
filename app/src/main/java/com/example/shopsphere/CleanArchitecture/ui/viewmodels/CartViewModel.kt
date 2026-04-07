package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.domain.GetCardProductsUseCase
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.models.mapToPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
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
                val cartLines = sharedPreference.getCartLines()
                if (cartLines.isEmpty()) {
                    _cartProducts.postValue(emptyList())
                    _emptyState.postValue(true)
                    _totalPrice.postValue(0.0)
                } else {
                    val result = getCardProductsUseCase(cartLines.map { it.productId }.distinct())
                    if (result.isSuccess) {
                        val productsById = result.getOrNull().orEmpty().associateBy { it.id }
                        val cartProductList = cartLines.mapNotNull { line ->
                            productsById[line.productId]?.mapToPresentation()?.let { product ->
                                product.copy(
                                    quantity = line.quantity,
                                    stock = product.stock,
                                    selectedSize = if (supportsSizeSelection(product.category)) {
                                        line.size
                                    } else {
                                        ""
                                    },
                                    cartLineId = line.lineId
                                )
                            }
                        }
                        _cartProducts.postValue(cartProductList)
                        _emptyState.postValue(cartProductList.isEmpty())
                        updateTotalPrice(cartProductList)
                    } else {
                        _emptyState.postValue(true)
                        _totalPrice.postValue(0.0)
                    }
                }
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun updateTotalPrice(cartProducts: List<PresentationProductResult>) {
        val total = cartProducts.sumOf { product ->
            product.price * product.quantity
        }
        _totalPrice.postValue(total)
    }

    fun addProductToCart(productId: Int, size: String, availableStock: Int): Boolean {
        return try {
            val normalizedSize = size.trim().uppercase()
            val currentQuantity = sharedPreference.getCartLines()
                .firstOrNull {
                    if (normalizedSize.isBlank()) {
                        it.productId == productId
                    } else {
                        it.productId == productId && it.size == normalizedSize
                    }
                }
                ?.quantity ?: 0
            val stock = availableStock.coerceAtLeast(0)

            if (stock <= 0 || currentQuantity >= stock) {
                return false
            }

            sharedPreference.addCartProduct(productId, normalizedSize)
            loadCartItemCount()
            true
        } catch (e: Exception) {
            Log.e("CartViewModel", "addProductToCart error", e)
            false
        }
    }

    fun removeFromCart(productId: Int, size: String) {
        try {
            sharedPreference.removeCartProduct(productId, size)
            loadCartItemCount()
        } catch (e: Exception) {
            Log.e("CartViewModel", "removeFromCart error", e)
        }
    }

    fun removeCartLine(lineId: String) {
        try {
            sharedPreference.removeCartProductByLineId(lineId)
            loadCartItemCount()
        } catch (e: Exception) {
            Log.e("CartViewModel", "removeCartLine error", e)
        }
    }

    fun updateQuantity(lineId: String, newQuantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreference.updateQuantity(lineId, newQuantity)
            loadCartProducts()
        }
    }

    fun isInCart(productId: Int, size: String): Boolean {
        return try {
            sharedPreference.isInCart(productId, size).also {
                Log.d("CartViewModel", "isInCart: $productId, size=$size, result: $it")
            }
        } catch (e: Exception) {
            Log.e("CartViewModel", "isInCart error", e)
            false
        }
    }

    private fun supportsSizeSelection(category: String): Boolean {
        val normalized = category.trim().lowercase()
        val clothingKeywords = listOf(
            "clothing", "shirt", "dress", "top", "jacket",
            "jeans", "trouser", "pants", "hoodie", "sweater", "coat"
        )
        return clothingKeywords.any { keyword -> normalized.contains(keyword) }
    }
}
