package com.example.shopsphere.CleanArchitecture.domain

import javax.inject.Inject

class GetCardProductsUseCase @Inject constructor(private val repository: IRepository) {

    suspend operator fun invoke(ids: List<Int>): Result<List<DomainProductResult>> {
        return repository.getCartProducts(ids)
    }
}