package com.example.shopsphere.CleanArchitecture.data.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface DirectionsApiServices {

    @GET("maps/api/directions/json")
    suspend fun getDrivingDirections(
        @Header("X-Android-Package") androidPackage: String,
        @Header("X-Android-Cert") androidCertSha1: String,
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("alternatives") alternatives: Boolean = false,
        @Query("key") apiKey: String
    ): DirectionsResponse
}
