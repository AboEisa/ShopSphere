package com.example.coroutines.CleanArchitecture.domain

import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import javax.inject.Inject

class GetFavoriteProductsUseCase @Inject constructor(private val repository: IRepository) {


    suspend operator fun invoke(ids: List<Int>): Result<List<DomainProductResult>> {
        return repository.getFavoriteProducts(ids)
    }
}