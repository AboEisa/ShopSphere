package com.example.shopsphere.CleanArchitecture.domain.auth

import com.example.shopsphere.CleanArchitecture.domain.IRepository
import javax.inject.Inject

class GoogleLoginUseCase @Inject constructor(private val repo: IRepository) {
    suspend operator fun invoke(idToken: String) = repo.googleSignIn(idToken)
}