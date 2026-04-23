package com.example.weatherapp.model

sealed class WeeklyForecastItem {

    data class FeaturedDay(
        val label: String,           // "Tomorrow"
        val tempMin: Double,
        val tempMax: Double,
        val weatherCode: Int,
        val precipitation: Double,   // mm
        val windSpeed: Double,       // km/h
        val humidity: Int            // %
    ) : WeeklyForecastItem()

    data class SimpleDay(
        val dayName: String,         // "Wednesday", "Thursday"...
        val tempMin: Double,
        val tempMax: Double,
        val weatherCode: Int
    ) : WeeklyForecastItem()
}