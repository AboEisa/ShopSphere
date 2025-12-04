package com.example.shopsphere.CleanArchitecture.data

import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.models.mapToDomain
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.domain.DomainProductResult
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class Repository @Inject constructor(
    private val remoteDataSource: IRemoteDataSource,
    private val sharedPreferencesHelper: SharedPreference,
    private val firebaseAuth: FirebaseAuth
) : IRepository {


    override suspend fun getProducts(): Result<List<DomainProductResult>> {
        return try {
            val remoteData = remoteDataSource.getProducts()
            Result.success(remoteData.getOrNull()?.map { it.mapToDomain() } ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductsByCategory(category: String): Result<List<DomainProductResult>> {

        return try {
            val remoteData = remoteDataSource.getProductsByCategory(category)
            Result.success(remoteData.getOrNull()?.map { it.mapToDomain() } ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavoriteProducts(ids: List<Int>): Result<List<DomainProductResult>> {
        return try {
            val favoriteIds = sharedPreferencesHelper.getFavoriteProducts()
            val allProducts = remoteDataSource.getProducts().getOrThrow()
            val favorites = allProducts.filter { favoriteIds.contains(it.id) }
            Result.success(favorites.map { it.mapToDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun toggleFavorite(productId: Int) {
        if (sharedPreferencesHelper.isFavorite(productId)) {
            sharedPreferencesHelper.removeFavoriteProduct(productId)
        } else {
            sharedPreferencesHelper.addFavoriteProduct(productId)
        }
    }

    override suspend fun isFavorite(productId: Int): Boolean {
        return sharedPreferencesHelper.isFavorite(productId)
    }

    override suspend fun getFavoriteIds(): List<Int> {
        return sharedPreferencesHelper.getFavoriteProducts()
    }

//    override suspend fun addToCart(cart: DomainAddToCartRequest): Result<List<DomainCartProduct>> {
//        return try {
//            val remoteData = remoteDataSource.addToCart(cart.mapToData())
//            Result.success(remoteData.getOrNull()?.map { it.mapToDomain() } ?: emptyList())
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    override suspend fun getCartProducts(ids: List<Int>): Result<List<DomainProductResult>> {
        return try {
            val cartIds = sharedPreferencesHelper.getCartProducts()
            val allProducts = remoteDataSource.getProducts().getOrThrow()
            val cartProducts = allProducts.filter { cartIds.contains(it.id) }
            Result.success(cartProducts.map { it.mapToDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




    override suspend fun login(email: String, password: String): Result<Unit> =
        try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun loginWithGoogle(idToken: String): Result<Unit> =
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }



    override suspend fun registerEmail(
        name: String,
        email: String,
        password: String
    ): Result<Boolean> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(
                email,
                password
            ).await()

            val user = authResult.user

            if (user != null) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user.updateProfile(profileUpdates).await()
                Result.success(true)
            } else {
                Result.failure(Exception("User creation failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCartItemCount(): Int {
        return sharedPreferencesHelper.getCartItemCount()
    }

    override fun logout() = firebaseAuth.signOut()

    override fun currentUserId(): String? = firebaseAuth.currentUser?.uid

}