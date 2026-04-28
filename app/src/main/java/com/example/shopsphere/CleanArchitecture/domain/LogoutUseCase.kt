package com.example.shopsphere.CleanArchitecture.domain

import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import javax.inject.Inject

/**
 * Calls the server's /Logout endpoint and then clears local auth state.
 *
 * Server-side failure does NOT block local logout: if the user wants out,
 * we honour that even if the network is flaky. The remote call is best-effort.
 */
class LogoutUseCase @Inject constructor(
    private val remote: IRemoteDataSource,
    private val repository: IRepository
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        runCatching { remote.logout() }   // best-effort, ignore failure
        repository.logout()
    }
}
