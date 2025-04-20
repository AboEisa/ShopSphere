package com.example.shopsphere.CleanArchitecture.domain


import javax.inject.Inject

class GetProductsUseCase @Inject constructor(private val repository: IRepository) {

    suspend fun getProducts() : Result<List<DomainProductResult>> {
      return  repository.getProducts()
    }
}