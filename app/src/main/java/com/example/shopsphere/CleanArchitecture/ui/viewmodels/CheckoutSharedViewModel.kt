package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CheckoutSharedViewModel : ViewModel() {
    private val _address = MutableLiveData<Pair<String, String>?>()
    val address: LiveData<Pair<String, String>?> = _address

    private val _cardLastFour = MutableLiveData<String?>()
    val cardLastFour: LiveData<String?> = _cardLastFour

    fun setAddress(nick: String, full: String) {
        _address.value = nick to full
    }

    fun setCardLastFour(lastFour: String) {
        _cardLastFour.value = lastFour
    }
}