package com.example.weatherapp.model

data class GeocodingResponse(
    val results: List<GeoLocation>?
)

data class GeoLocation(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    val admin1: String?
)