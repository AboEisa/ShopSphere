package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coroutines.CleanArchitecture.domain.GetFavoriteProductsUseCase
import com.example.coroutines.CleanArchitecture.domain.IsProductFavoriteUseCase
import com.example.coroutines.CleanArchitecture.domain.ToggleFavoriteStatusUseCase
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.models.mapToPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val getFavoriteProductsUseCase: GetFavoriteProductsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteStatusUseCase,
    private val isFavoriteUseCase: IsProductFavoriteUseCase,
    private val sharedPreference: SharedPreference
) : ViewModel() {

    private val _favoriteProducts = MutableLiveData<List<PresentationProductResult>>()
    val favoriteProducts: LiveData<List<PresentationProductResult>> get() = _favoriteProducts

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _emptyState = MutableLiveData(false)
    val emptyState: LiveData<Boolean> = _emptyState

    private val _favoriteStatus = MutableLiveData<Boolean>()
    val favoriteStatus: LiveData<Boolean> = _favoriteStatus

    init {
        loadFavoriteProducts()
        observePreferenceChanges()
    }

    private fun observePreferenceChanges() {
        viewModelScope.launch {
            sharedPreference.changes.collect {
                loadFavoriteProducts()
            }
        }
    }

    fun loadFavoriteProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val favoriteIds = sharedPreference.getFavoriteProducts()
                val favorites = getFavoriteProductsUseCase(favoriteIds)
                _favoriteProducts.postValue(favorites.getOrNull()?.mapToPresentation())
                _emptyState.postValue(favorites.getOrNull()?.isEmpty() == true)
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun toggleFavorite(productId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            toggleFavoriteUseCase(productId)
            loadFavoriteProducts()
        }
    }

    fun checkFavoriteStatus(productId: Int) {
        viewModelScope.launch {
            _favoriteStatus.value = isFavoriteUseCase(productId)
        }
    }

    suspend fun isFavorite(productId: Int): Boolean {
        return isFavoriteUseCase(productId)
    }

    fun addFavoriteProduct(productId: Int) {
        sharedPreference.addFavoriteProduct(productId)
        loadFavoriteProducts()
    }

    fun removeFavoriteProduct(productId: Int) {
        sharedPreference.removeFavoriteProduct(productId)
    }
}
