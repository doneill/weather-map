package com.jdoneill.weathermap.service

import com.google.gson.GsonBuilder

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlaceService {

    val api: PlaceAPI
        get() {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS

            val client = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build()

            val gson = GsonBuilder().setPrettyPrinting().create()
            val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build()

            return retrofit.create(PlaceAPI::class.java)
        }

    companion object {
        private val BASE_URL = "https://maps.googleapis.com/maps/"
    }
}
