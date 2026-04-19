package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coroutines.CleanArchitecture.domain.GetFavoriteProductsUseCase
import com.example.coroutines.CleanArchitecture.domain.IsProductFavoriteUseCase
import com.example.coroutines.CleanArchitecture.domain.ToggleFavoriteStatusUseCase
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
    private val isFavoriteUseCase: IsProductFavoriteUseCase
) : ViewModel() {

    private val _favoriteProducts = MutableLiveData<List<PresentationProductResult>>(emptyList())
    val favoriteProducts: LiveData<List<PresentationProductResult>> get() = _favoriteProducts

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _emptyState = MutableLiveData(false)
    val emptyState: LiveData<Boolean> = _emptyState

    private val _favoriteStatus = MutableLiveData<Boolean>()
    val favoriteStatus: LiveData<Boolean> = _favoriteStatus

    // Source of truth for fast `isFavoriteSync` lookups. Updated on the main thread
    // before the backend round-trip so tapping a heart flips color instantly, even
    // when the full product list refetch hasn't returned yet.
    private val _favoriteIds = MutableLiveData<Set<Int>>(emptySet())
    val favoriteIds: LiveData<Set<Int>> = _favoriteIds

    init {
        loadFavoriteProducts()
    }

    fun loadFavoriteProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            try {
                val favorites = getFavoriteProductsUseCase(emptyList())
                val mappedFavorites = favorites.getOrNull()?.mapToPresentation().orEmpty()
                _favoriteProducts.postValue(mappedFavorites)
                _favoriteIds.postValue(mappedFavorites.map { it.id }.toSet())
                _emptyState.postValue(mappedFavorites.isEmpty())
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun toggleFavorite(productId: Int) {
        val currentIds = _favoriteIds.value.orEmpty()
        val wasFav = productId in currentIds

        // Flip the ID set synchronously on the main thread so the very next
        // `isFavoriteSync` call (invoked right after onFavoriteClick) sees the new state.
        _favoriteIds.value = if (wasFav) currentIds - productId else currentIds + productId

        if (wasFav) {
            val pruned = _favoriteProducts.value.orEmpty().filterNot { it.id == productId }
            _favoriteProducts.value = pruned
            _emptyState.value = pruned.isEmpty()
        }

        viewModelScope.launch(Dispatchers.IO) {
            toggleFavoriteUseCase(productId)
            if (!wasFav) {
                // Adding — refetch so Saved tab picks up the full product (title/image/price).
                loadFavoriteProducts()
            }
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

    /** Synchronous check — reads the optimistic ID set, updated on every toggle. */
    fun isFavoriteSync(productId: Int): Boolean {
        return productId in _favoriteIds.value.orEmpty()
    }

    fun addFavoriteProduct(productId: Int) = toggleFavorite(productId)

    fun removeFavoriteProduct(productId: Int) = toggleFavorite(productId)
}
