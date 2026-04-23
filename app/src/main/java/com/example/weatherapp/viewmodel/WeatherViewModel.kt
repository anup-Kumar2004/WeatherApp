package com.example.weatherapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.model.GeoLocation
import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.repository.WeatherRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class WeatherViewModel : ViewModel() {

    private val repository = WeatherRepository()

    private val _weatherData = MutableLiveData<WeatherResponse>()
    val weatherData: LiveData<WeatherResponse> = _weatherData

    private val _searchResults = MutableLiveData<List<GeoLocation>>()
    val searchResults: LiveData<List<GeoLocation>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _cityName = MutableLiveData<String>()
    val cityName: LiveData<String> = _cityName

    // ADD this new LiveData field alongside the others:
    private val _timezone = MutableLiveData<String>()
    val timezone: LiveData<String> = _timezone


    // ADD these two fields at the top alongside other private fields:
    private var lastLatitude: Double = 0.0
    private var lastLongitude: Double = 0.0

    // ADD a getter so MainActivity can read them:
    fun getLastCoordinates(): Pair<Double, Double> = Pair(lastLatitude, lastLongitude)
    fun hasCoordinates(): Boolean = lastLatitude != 0.0 || lastLongitude != 0.0

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private var searchJob: Job? = null

    fun fetchWeather(latitude: Double, longitude: Double, cityName: String) {
        lastLatitude = latitude      // ← ADD
        lastLongitude = longitude    // ← ADD
        _isLoading.value = true
        _cityName.value = cityName

        viewModelScope.launch {
            val result = repository.getWeatherForecast(latitude, longitude)

            result.onSuccess { data ->
                _weatherData.value = data
                _timezone.value = data.timezone   // ← ADD THIS LINE
            }
            result.onFailure { error ->
                _errorMessage.value = "Failed to fetch weather: ${error.message}"
            }

            // Single-step fetch — stop loading after it resolves either way
            _isLoading.value = false
        }
    }

    // REPLACE the existing searchCity() function:
    fun searchCity(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L)
            val result = repository.searchCity(query)
            result.onSuccess { data ->
                _searchResults.value = data.results ?: emptyList()
            }
            // On failure — do nothing, don't post empty list
            // The hint text handles the "no results" case
        }
    }

    fun fetchWeatherWithReverseGeocode(latitude: Double, longitude: Double) {
        lastLatitude = latitude      // ← ADD
        lastLongitude = longitude    // ← ADD
        _isLoading.value = true
        _cityName.value = "Fetching location..."

        viewModelScope.launch {

            // ── Step 1: Fire both calls in parallel, same as before ──
            val weatherDeferred = async { repository.getWeatherForecast(latitude, longitude) }
            val cityDeferred    = async { repository.findNearestCityName(latitude, longitude) }

            // ── Step 2: Wait ONLY for weather data ──
            // Don't touch cityDeferred yet — let it keep running in background
            val weatherResult = weatherDeferred.await()

            weatherResult.onSuccess { data ->
                _weatherData.value = data
                _timezone.value = data.timezone   // ← ADD THIS LINE
            }
            weatherResult.onFailure { error ->
                _errorMessage.value = "Failed to fetch weather: ${error.message}"
            }

            // ── Step 3: Stop shimmer as soon as weather arrives ──
            // User sees the screen NOW, city name still says "Fetching location..."
            _isLoading.value = false

            // ── Step 4: City name updates whenever it finishes ──
            // This runs after the screen is already showing — user sees
            // the city label quietly update from "Fetching location..." to the real name
            val cityResult = cityDeferred.await()

            cityResult.onSuccess { name ->
                _cityName.value = name
            }
            cityResult.onFailure {
                _cityName.value = "Current Location"
            }
        }
    }

    fun refreshWeather() {
        if (!hasCoordinates()) return

        viewModelScope.launch {
            _isRefreshing.value = true        // ← use isRefreshing, NOT isLoading
            val result = repository.getWeatherForecast(lastLatitude, lastLongitude)
            result.onSuccess { data ->
                _weatherData.value = data
                _timezone.value = data.timezone
            }
            result.onFailure { error ->
                _errorMessage.value = "Refresh failed: ${error.message}"
            }
            _isRefreshing.value = false
        }
    }


    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}