package com.example.weatherapp.network

import com.example.weatherapp.model.NominatimResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApiService {

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("format") format: String = "json",
        @Query("accept-language") language: String = "en",
        @Query("zoom") zoom: Int = 10
    ): NominatimResponse
}

object NominatimRetrofitInstance {

    private const val BASE_URL = "https://nominatim.openstreetmap.org/"

    private val httpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "WeatherApp/1.0 (Android)")
                .header("Accept-Language", "en")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val api: NominatimApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApiService::class.java)
    }
}