package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult

class SharedCartViewModel : ViewModel() {
    private val _cartItems = MutableLiveData<List<PresentationProductResult>>(emptyList())
    val cartItems: LiveData<List<PresentationProductResult>> = _cartItems

    private val _totalPrice = MutableLiveData<Double>(0.0)
    val totalPrice: LiveData<Double> = _totalPrice

    fun setCartItems(items: List<PresentationProductResult>) {
        _cartItems.value = items
        _totalPrice.value = items.sumOf { it.price * (it.quantity ?: 1) }
    }
}