package com.example.weatherapp.model

import com.google.gson.annotations.SerializedName

data class NominatimResponse(
    @SerializedName("address") val address: NominatimAddress
)

data class NominatimAddress(
    @SerializedName("city") val city: String?,
    @SerializedName("town") val town: String?,
    @SerializedName("village") val village: String?,
    @SerializedName("county") val county: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("country") val country: String?
)