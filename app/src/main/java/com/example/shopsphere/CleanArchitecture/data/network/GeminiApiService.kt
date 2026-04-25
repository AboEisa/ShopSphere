package com.example.shopsphere.CleanArchitecture.data.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Thin Retrofit wrapper around Google's Generative Language API.
 * We use the non-streaming generateContent endpoint — simple, reliable,
 * and good enough for short shopping-assistant replies.
 */
interface GeminiApiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
