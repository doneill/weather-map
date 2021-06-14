package com.jdoneill.weathermap.presenter

import com.google.gson.GsonBuilder

import com.jdoneill.weathermap.BuildConfig
import com.jdoneill.weathermap.model.WeatherData
import com.jdoneill.weathermap.service.WeatherService

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

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
        val loggingInterceptor = HttpLoggingInterceptor()

        loggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val gson = GsonBuilder().setPrettyPrinting().create()

        val network = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build()
        val weatherService = network.create(WeatherService::class.java)

        return weatherService.weatherByCoord("imperial", lat, lon, apiKey)
    }

    companion object {
        private const val BASE_URL = "http://api.openweathermap.org/"
    }
}
