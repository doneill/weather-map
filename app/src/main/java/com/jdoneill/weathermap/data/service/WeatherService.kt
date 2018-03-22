package com.jdoneill.weathermap.data.service

import com.jdoneill.weathermap.data.Weather
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interact with Open Weather Map REST API
 */
interface WeatherService {

    @GET("/data/2.5/weather")
    fun weatherByCoord(@Query("units") units: String, @Query("lat") lat: Float, @Query("lon") lon:Float, @Query("APPID") apiKey: String): Call<Weather>
}