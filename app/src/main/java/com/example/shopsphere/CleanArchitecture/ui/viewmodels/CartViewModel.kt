package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.domain.GetCardProductsUseCase
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationRating
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
    private val repository: IRepository,
    private val sharedPreference: SharedPreference
) : ViewModel() {
    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount

    init {
        loadCartProducts()
    }

    private fun updateCartItemCount(products: List<PresentationProductResult>) {
        _cartItemCount.value = products.sumOf { it.quantity }
    }

    fun refreshCartCount() {
        viewModelScope.launch {
            try {
                val count = repository.getCartItemCount()
                _cartItemCount.value = count
            } catch (e: Exception) {
                Log.e("CartViewModel", "refreshCartCount error", e)
            }
        }
    }

    private val _cartProducts = MutableLiveData<List<PresentationProductResult>>()
    val cartProducts: LiveData<List<PresentationProductResult>> = _cartProducts

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _emptyState = MutableLiveData(false)
    val emptyState: LiveData<Boolean> = _emptyState

    private val _totalPrice = MutableLiveData<Double>()
    val totalPrice: LiveData<Double> = _totalPrice

    fun loadCartProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val remoteItems = repository.getCartItems().getOrNull().orEmpty()
                if (remoteItems.isEmpty()) {
                    _cartProducts.postValue(emptyList())
                    _emptyState.postValue(true)
                    _totalPrice.postValue(0.0)
                    _cartItemCount.value = 0
                } else {
                    val cartProductList = remoteItems.map { item ->
                        PresentationProductResult(
                            category = "",
                            description = "",
                            id = item.cartId,
                            image = item.image,
                            price = item.price,
                            rating = PresentationRating(count = 0, rate = 0.0),
                            title = item.productName,
                            stock = 999,
                            quantity = item.quantity,
                            selectedSize = "",
                            cartLineId = item.cartId.toString()
                        )
                    }
                    _cartProducts.postValue(cartProductList)
                    _emptyState.postValue(cartProductList.isEmpty())
                    updateTotalPrice(cartProductList)
                    updateCartItemCount(cartProductList)
                    syncLocalCart(cartProductList)
                }
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun syncLocalCart(products: List<PresentationProductResult>) {
        sharedPreference.clearCartProducts()
        products.forEach { product ->
            repeat(product.quantity) {
                sharedPreference.addCartProduct(product.id, product.selectedSize)
            }
        }
    }

    private fun updateTotalPrice(cartProducts: List<PresentationProductResult>) {
        val total = cartProducts.sumOf { product ->
            product.price * product.quantity
        }
        _totalPrice.postValue(total)
    }

    suspend fun addProductToCart(productId: Int, size: String, availableStock: Int): Boolean {
        return try {
            val currentQuantity = cartProducts.value
                .orEmpty()
                .firstOrNull { it.id == productId }
                ?.quantity ?: 0
            val stock = availableStock.coerceAtLeast(0)

            if (stock <= 0 || currentQuantity >= stock) {
                return false
            }

            val remoteResult = repository.addToCart(productId, 1)
            if (remoteResult.isFailure) {
                Log.e("CartViewModel", "addProductToCart: API call failed", remoteResult.exceptionOrNull())
                return false
            }
            loadCartProducts()
            true
        } catch (e: Exception) {
            Log.e("CartViewModel", "addProductToCart error", e)
            false
        }
    }

    fun removeFromCart(productId: Int, size: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cartItem = cartProducts.value
                    .orEmpty()
                    .firstOrNull { it.id == productId }
                val cartId = cartItem?.cartLineId?.toIntOrNull()
                if (cartId != null) {
                    repository.removeCartItem(cartId)
                }
                loadCartProducts()
            } catch (e: Exception) {
                Log.e("CartViewModel", "removeFromCart error", e)
            }
        }
    }

    fun removeCartLine(lineId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cartId = lineId.toIntOrNull()
                if (cartId != null) {
                    repository.removeCartItem(cartId)
                }
                loadCartProducts()
            } catch (e: Exception) {
                Log.e("CartViewModel", "removeCartLine error", e)
            }
        }
    }

    fun updateQuantity(lineId: String, newQuantity: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cartId = lineId.toIntOrNull()
                if (cartId != null) {
                    repository.updateCartItemQuantity(cartId, newQuantity)
                }
                loadCartProducts()
            } catch (e: Exception) {
                Log.e("CartViewModel", "updateQuantity error", e)
            }
        }
    }

    fun isInCart(productId: Int, size: String): Boolean {
        val inRemoteCart = cartProducts.value.orEmpty().any { it.id == productId }
        Log.d("CartViewModel", "isInCart: productId=$productId size=$size → $inRemoteCart")
        return inRemoteCart
    }

    fun clearRemoteCart() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearCart()
                _cartProducts.postValue(emptyList())
                _emptyState.postValue(true)
                _totalPrice.postValue(0.0)
                _cartItemCount.value = 0
            } catch (e: Exception) {
                Log.e("CartViewModel", "clearRemoteCart error", e)
            }
        }
    }
}
