package com.example.shopsphere.CleanArchitecture.domain

import javax.inject.Inject

class AddToCartUseCase @Inject constructor(private val repository: IRepository) {

//    suspend fun addToCart(cart: DomainAddToCartRequest): Result<List<DomainCartProduct>> {
//        return repository.addToCart(cart)
//    }
}