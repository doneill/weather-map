package com.jdoneill.weathermap.service

import com.jdoneill.weathermap.data.Weather
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.android.inject
import retrofit2.Call
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
    ): Weather

}
