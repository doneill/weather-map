package com.jdoneill.weathermap.data.service

import com.jdoneill.weathermap.model.PlaceDetails
import com.jdoneill.weathermap.model.Prediction

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaceAPI {

    @GET("api/place/autocomplete/json")
    fun getPredictions(@Query("key") apikey: String, @Query("input") input: String, @Query("location") location: String, @Query("radius") radius: String): Call<Prediction>

    @GET("api/place/details/json")
    fun getDetails(@Query("key") apikey: String, @Query("placeid") placeId: String): Call<PlaceDetails>
}
