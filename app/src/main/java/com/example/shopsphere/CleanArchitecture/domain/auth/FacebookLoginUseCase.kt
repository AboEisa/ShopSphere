package com.example.shopsphere.CleanArchitecture.domain.auth

import com.example.shopsphere.CleanArchitecture.domain.IRepository

class FacebookLoginUseCase(private val repo: IRepository) {
    suspend operator fun invoke(accessToken: String) = repo.loginWithFacebook(accessToken)
}
