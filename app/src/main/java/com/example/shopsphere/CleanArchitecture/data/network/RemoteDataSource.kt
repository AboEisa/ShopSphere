package com.example.shopsphere.CleanArchitecture.data.network



import com.example.shopsphere.CleanArchitecture.data.models.ProductResult
import com.example.shopsphere.CleanArchitecture.data.models.ProductsModel
import javax.inject.Inject

class RemoteDataSource @Inject constructor(private val apiService: ApiServices) : IRemoteDataSource {
    override suspend fun getProducts(): Result<List<ProductResult>> {
        return try {
            val data = apiService.getProducts()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }

    }

    override suspend fun getProductsByCategory(category: String): Result<List<ProductResult>> {

        return try {
            val data = apiService.getProductsByCategory(category)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}