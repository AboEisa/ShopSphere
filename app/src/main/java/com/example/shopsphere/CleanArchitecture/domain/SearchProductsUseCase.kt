package com.example.shopsphere.CleanArchitecture.domain

import javax.inject.Inject

class SearchProductsUseCase @Inject constructor(
    private val repository: IRepository
) {
    suspend operator fun invoke(query: String): Result<List<DomainProductResult>> =
        repository.searchProducts(query)
}
