
package com.example.shopsphere.CleanArchitecture.domain.auth


import com.example.shopsphere.CleanArchitecture.domain.IRepository
import javax.inject.Inject


class RegisterUseCase @Inject constructor(private val repo: IRepository) {
    suspend operator fun invoke(name: String, email: String, password: String) = repo.registerEmail(name, email, password)
}