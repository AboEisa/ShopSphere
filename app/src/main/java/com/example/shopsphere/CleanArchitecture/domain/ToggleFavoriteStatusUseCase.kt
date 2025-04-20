package com.example.coroutines.CleanArchitecture.domain

import com.example.shopsphere.CleanArchitecture.domain.IRepository
import javax.inject.Inject

class ToggleFavoriteStatusUseCase @Inject constructor(private val repository: IRepository)  {

    suspend operator  fun invoke(movieId: Int) {
        repository.toggleFavorite(movieId)
    }
}