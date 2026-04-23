package com.example.weatherapp.repository

import com.example.weatherapp.model.GeocodingResponse
import com.example.weatherapp.model.GeoLocation
import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.network.GeocodingRetrofitInstance
import com.example.weatherapp.network.NominatimRetrofitInstance
import com.example.weatherapp.network.RetrofitInstance

class WeatherRepository {

    suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double
    ): Result<WeatherResponse> {
        return try {
            val response = RetrofitInstance.api.getWeatherForecast(
                latitude = latitude,
                longitude = longitude
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchCity(cityName: String): Result<GeocodingResponse> {
        return try {
            val response = GeocodingRetrofitInstance.api.searchCity(
                cityName = cityName
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Returns a recognizable city name string for display
    // Walks up the hierarchy: village → town → city → county → state
    // until it finds one that Open-Meteo geocoding recognizes
    suspend fun findNearestCityName(
        latitude: Double,
        longitude: Double
    ): Result<String> {
        return try {
            // Step 1 — Ask Nominatim what's at these coordinates
            val nominatimResponse = NominatimRetrofitInstance.api.reverseGeocode(
                latitude = latitude,
                longitude = longitude
            )
            val address = nominatimResponse.address
            val country = address.country ?: ""

            // Step 2 — Build priority list from most specific to broadest
            val namesToTry = listOfNotNull(
                address.city,
                address.town,
                address.village,
                address.county,
                address.state
            ).filter { it.isNotBlank() }

            // Step 3 — Try each name in Open-Meteo geocoding
            // Use the first one it recognizes
            var recognizedName: String? = null
            for (placeName in namesToTry) {
                val searchResult = searchCity(placeName)
                if (searchResult.isSuccess) {
                    val firstResult = searchResult.getOrNull()?.results?.firstOrNull()
                    if (firstResult != null) {
                        recognizedName = placeName
                        break
                    }
                }
            }

            // Step 4 — Return recognized name or best fallback
            val displayName = recognizedName ?: namesToTry.firstOrNull() ?: "Current Location"
            val fullName = if (country.isNotBlank()) "$displayName,\n$country" else displayName
            Result.success(fullName)

        } catch (e: Exception) {
            // Nominatim failed — fall back gracefully
            Result.success("Current Location")
        }
    }
}