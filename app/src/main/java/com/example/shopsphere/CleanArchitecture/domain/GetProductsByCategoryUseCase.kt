package com.example.shopsphere.CleanArchitecture.domain

import javax.inject.Inject

class GetProductsByCategoryUseCase  @Inject constructor(private val repository: IRepository) {

    suspend fun getProductsByCategory(category: String): Result<List<DomainProductResult>> {
        return repository.getProductsByCategory(category)
    }
}