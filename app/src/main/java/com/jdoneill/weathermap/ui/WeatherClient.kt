/* Copyright 2017 jdoneill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.jdoneill.weathermap.ui

import com.jdoneill.weathermap.BuildConfig
import com.jdoneill.weathermap.data.Constants
import com.jdoneill.weathermap.data.Weather
import com.jdoneill.weathermap.data.service.WeatherService
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