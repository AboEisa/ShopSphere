package com.example.shopsphere.CleanArchitecture.domain.auth

import com.example.shopsphere.CleanArchitecture.domain.IRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

class FacebookLoginUseCase @Inject constructor(
    private val repo: IRepository
) {
    suspend operator fun invoke(accessToken: String): Result<FirebaseUser> {
        return try {
            repo.facebookSignIn(accessToken)   // return Result<FirebaseUser>
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
