package com.jdoneill.weathermap.presenter

import com.jdoneill.weathermap.BuildConfig
import com.jdoneill.weathermap.data.Constants
import com.jdoneill.weathermap.data.WeatherData
import com.jdoneill.weathermap.service.WeatherService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherClient {
    private val apiKey: String = BuildConfig.API_KEY

    /**
     * Get weather forecast at coordinate location
     *
     * @lat coordinate latitude
     * @lon coordinate longitude
     */
    suspend fun getWeatherForCoord(lat: Float, lon: Float): WeatherData {
        val network = Retrofit.Builder()
                .baseUrl(Constants.HTTP_API_OPENWEATHERMAP_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val weatherService = network.create(WeatherService::class.java)

        return weatherService.weatherByCoord("imperial", lat, lon, apiKey)
    }

    companion object {
        private const val BASE_URL = "http://api.openweathermap.org/"
    }
}
