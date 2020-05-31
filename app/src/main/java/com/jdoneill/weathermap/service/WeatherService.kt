package com.jdoneill.weathermap.service

import com.jdoneill.weathermap.data.WeatherData
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interact with Open Weather Map REST API
 */
interface WeatherService {

    // ------------------------------------------------------------------------
    // Public API Methods
    // ------------------------------------------------------------------------

    @GET("/data/2.5/weather")
    suspend fun weatherByCoord(
        @Query("units") units: String,
        @Query("lat") lat: Float,
        @Query("lon") lon: Float,
        @Query("APPID") apiKey: String
    ): WeatherData

}
