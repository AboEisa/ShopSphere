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
    // ─── State holders ────────────────────────────────────────────────────────
    // IMPORTANT: All fields must be declared BEFORE the init block. The init
    // block calls loadCartProducts() which launches a coroutine on Dispatchers.IO;
    // if any of these fields are still uninitialized when that coroutine runs,
    // accessing them throws NullPointerException (this was the post-login /
    // post-rebuild crash).
    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount

    private val _cartProducts = MutableLiveData<List<PresentationProductResult>>(emptyList())
    val cartProducts: LiveData<List<PresentationProductResult>> = _cartProducts

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _emptyState = MutableLiveData(false)
    val emptyState: LiveData<Boolean> = _emptyState

    private val _totalPrice = MutableLiveData<Double>()
    val totalPrice: LiveData<Double> = _totalPrice

    init {
        loadCartProducts()
    }

    private fun updateCartItemCount(products: List<PresentationProductResult>) {
        // Badge shows the number of unique line items (products) in the cart,
        // NOT the sum of quantities. Multiple units of one product = one badge "1".
        _cartItemCount.value = products.size
    }

    fun refreshCartCount() {
        viewModelScope.launch {
            try {
                // Prefer the accurate list-based count so the badge always reflects
                // unique product lines. If the list hasn't loaded yet, fall back to
                // the repository count and clamp to 0 on any failure.
                val cached = _cartProducts.value
                if (cached != null) {
                    _cartItemCount.value = cached.size
                } else {
                    val count = repository.getCartItemCount()
                    _cartItemCount.value = count.coerceAtLeast(0)
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "refreshCartCount error", e)
                // Hide the badge on network failure rather than showing a stale count
                _cartItemCount.value = _cartProducts.value?.size ?: 0
            }
        }
    }

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
                            // Use productId when backend returns it; fall back to cartId
                            // so onItemClick navigation still has a non-zero value.
                            id = if (item.productId != 0) item.productId else item.cartId,
                            image = item.image,
                            price = item.price,
                            rating = PresentationRating(count = 0, rate = 0.0),
                            title = item.productName,
                            stock = 999,
                            quantity = item.quantity,
                            selectedSize = "",
                            // Swagger names the param "productId" but it actually
                            // expects the cartID from GetCartItems (confirmed via
                            // Postman: cartID=4019 → 200 OK).
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
            // Enforce stock only when we have a reliable positive stock value.
            // If availableStock is 0 or unknown, trust the backend to reject.
            if (availableStock > 0) {
                val currentQuantity = cartProducts.value
                    .orEmpty()
                    .firstOrNull { it.id == productId }
                    ?.quantity ?: 0
                if (currentQuantity >= availableStock) {
                    Log.w("CartViewModel", "addProductToCart: stock limit hit ($currentQuantity/$availableStock)")
                    return false
                }
            }

            Log.d("CartViewModel", "addProductToCart: productId=$productId")
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
                val serverItemId = resolveServerItemId(lineId)
                if (serverItemId == null) {
                    Log.e("CartViewModel", "removeCartLine: invalid lineId=$lineId")
                    return@launch
                }
                Log.d("CartViewModel", "removeCartLine: lineId=$lineId resolvedId=$serverItemId")
                var result = repository.removeCartItem(serverItemId)
                val lineIdAsInt = lineId.toIntOrNull()
                if (result.isFailure && lineIdAsInt != null && lineIdAsInt != serverItemId) {
                    Log.w(
                        "CartViewModel",
                        "removeCartLine: retry with cart line id=$lineIdAsInt after resolved id failed"
                    )
                    result = repository.removeCartItem(lineIdAsInt)
                }
                if (result.isFailure) {
                    Log.e("CartViewModel", "removeCartLine failed", result.exceptionOrNull())
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
                val serverItemId = resolveServerItemId(lineId)
                if (serverItemId == null) {
                    Log.e("CartViewModel", "updateQuantity: invalid lineId=$lineId")
                    return@launch
                }
                Log.d(
                    "CartViewModel",
                    "updateQuantity: lineId=$lineId resolvedId=$serverItemId newQuantity=$newQuantity"
                )
                var result = repository.updateCartItemQuantity(serverItemId, newQuantity)
                val lineIdAsInt = lineId.toIntOrNull()
                if (result.isFailure && lineIdAsInt != null && lineIdAsInt != serverItemId) {
                    Log.w(
                        "CartViewModel",
                        "updateQuantity: retry with cart line id=$lineIdAsInt after resolved id failed"
                    )
                    result = repository.updateCartItemQuantity(lineIdAsInt, newQuantity)
                }
                if (result.isFailure) {
                    Log.e("CartViewModel", "updateQuantity failed", result.exceptionOrNull())
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

    /**
     * Backend cart mutation endpoints are inconsistent: some accounts return cartID in GetCartItems,
     * while RemoveItem/UpdateQuantity may require productId instead.
     * Prefer mapped product id from current cart list and fall back to line id.
     */
    private fun resolveServerItemId(lineId: String): Int? {
        val mappedProductId = cartProducts.value
            .orEmpty()
            .firstOrNull { it.cartLineId == lineId }
            ?.id
            ?.takeIf { it > 0 }

        return mappedProductId ?: lineId.toIntOrNull()
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