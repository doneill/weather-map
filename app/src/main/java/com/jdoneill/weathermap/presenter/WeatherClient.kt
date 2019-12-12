package com.jdoneill.weathermap.presenter

import com.jdoneill.weathermap.BuildConfig
import com.jdoneill.weathermap.data.Constants
import com.jdoneill.weathermap.data.Weather
import com.jdoneill.weathermap.service.WeatherService
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherClient {
    private val APIKEY: String = BuildConfig.API_KEY

    /**
     * Get weather forecast at coordinate location
     *
     * @lat coordinate latitude
     * @lon coordinate longitude
     */
    fun getWeatherForCoord(lat: Float, lon: Float): Call<Weather> {
        val network = Retrofit.Builder()
                .baseUrl(Constants.HTTP_API_OPENWEATHERMAP_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val weatherServices = network.create(WeatherService::class.java)
        return weatherServices.weatherByCoord("imperial", lat, lon, APIKEY)
    }
}
