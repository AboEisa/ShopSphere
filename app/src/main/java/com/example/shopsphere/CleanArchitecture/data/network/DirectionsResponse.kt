package com.example.shopsphere.CleanArchitecture.data.network

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    val status: String,
    @SerializedName("error_message")
    val errorMessage: String? = null,
    val routes: List<DirectionsRoute> = emptyList()
)

data class DirectionsRoute(
    @SerializedName("overview_polyline")
    val overviewPolyline: DirectionsOverviewPolyline? = null,
    val legs: List<DirectionsLeg> = emptyList()
)

data class DirectionsLeg(
    val steps: List<DirectionsStep> = emptyList()
)

data class DirectionsStep(
    val polyline: DirectionsOverviewPolyline? = null
)

data class DirectionsOverviewPolyline(
    val points: String? = null
)
