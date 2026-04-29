package com.example.shopsphere.CleanArchitecture.domain.auth

import com.example.shopsphere.CleanArchitecture.domain.IRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repo: IRepository
) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Result<String> {
        return try {
            val result = repo.registerEmail(firstName, lastName, email, password)

            if (result.isSuccess) {
                // After successful registration, automatically login to get the JWT token
                val loginResult = repo.login(email, password)
                
                if (loginResult.isSuccess) {
                    val uid = repo.currentUserId()
                    if (uid != null) {
                        Result.success(uid)
                    } else {
                        Result.failure(Exception("Registration succeeded but user ID is null"))
                    }
                } else {
                    Result.failure(
                        loginResult.exceptionOrNull() ?: Exception("Registration succeeded but auto-login failed")
                    )
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