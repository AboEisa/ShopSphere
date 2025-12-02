package com.example.shopsphere.CleanArchitecture.domain.auth

import com.example.shopsphere.CleanArchitecture.domain.IRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repo: IRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String
    ): Result<String> {
        return try {
            val result = repo.registerEmail(name, email, password)

            if (result.isSuccess) {
                val uid = repo.currentUserId()
                if (uid != null) {
                    Result.success(uid)
                } else {
                    Result.failure(Exception("Registration succeeded but user ID is null"))
                }
            } else {
                Result.failure(
                    result.exceptionOrNull() ?: Exception("Unknown registration error")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}