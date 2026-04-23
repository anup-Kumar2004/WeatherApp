package com.example.weatherapp.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @SerializedName("current_weather") val currentWeather: CurrentWeather,
    val hourly: HourlyData,
    val daily: DailyData,
    val timezone: String   // ← ADD THIS (e.g. "America/New_York", "Asia/Kolkata")
)
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    @SerializedName("is_day") val isDay: Int  // 1 = day, 0 = night
)

data class HourlyData(
    val time: List<String>,
    @SerializedName("temperature_2m") val temperature: List<Double>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    // ✅ Fixed: "relativehumidity_2m" → "relative_humidity_2m" (matches renamed API field)
    @SerializedName("relative_humidity_2m") val humidity: List<Int>
)

data class DailyData(
    val time: List<String>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("temperature_2m_max") val tempMax: List<Double>,
    @SerializedName("temperature_2m_min") val tempMin: List<Double>,
    @SerializedName("precipitation_sum") val precipitation: List<Double>,
    // ✅ Fixed: "windspeed_10m_max" → "wind_speed_10m_max" (matches renamed API field)
    @SerializedName("wind_speed_10m_max") val windspeed: List<Double>
)