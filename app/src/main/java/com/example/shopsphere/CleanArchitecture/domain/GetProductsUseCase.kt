package com.example.shopsphere.CleanArchitecture.domain


import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(private val repository: IRepository) {

    suspend fun getProducts() : Result<List<DomainProductResult>> {
      return  repository.getProducts()
    }

    /**
     * Cache-first products feed — emits the disk cache instantly (when
     * present) and then the fresh network result. Use this on screens
     * where instant render matters more than absolute freshness.
     */
    fun observeProducts(): Flow<Result<List<DomainProductResult>>> =
        repository.observeProducts()
}