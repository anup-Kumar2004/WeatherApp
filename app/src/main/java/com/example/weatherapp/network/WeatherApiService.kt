package com.example.weatherapp.network

import com.example.weatherapp.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        // ✅ Fixed: "relativehumidity_2m" → "relative_humidity_2m" (Open-Meteo renamed it)
        @Query("hourly") hourly: String = "temperature_2m,weather_code,relative_humidity_2m",
        // ✅ Fixed: "windspeed_10m_max" → "wind_speed_10m_max" (Open-Meteo renamed it)
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): WeatherResponse
}