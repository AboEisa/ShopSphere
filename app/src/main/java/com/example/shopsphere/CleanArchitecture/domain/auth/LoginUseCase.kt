

package com.example.shopsphere.CleanArchitecture.domain.auth


import com.example.shopsphere.CleanArchitecture.domain.IRepository
import javax.inject.Inject


class LoginUseCase @Inject constructor(private val repo: IRepository) {
    suspend operator fun invoke(email: String, password: String) = repo.loginEmail(email, password)
}