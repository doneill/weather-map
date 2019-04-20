package com.jdoneill.weathermap.presenter

import com.jdoneill.weathermap.BuildConfig
import com.jdoneill.weathermap.model.PlaceDetails
import com.jdoneill.weathermap.model.Prediction
import com.jdoneill.weathermap.service.PlaceService

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PlaceAutocomplete(private val mListener: PlacesListener) {
    private val placeService: PlaceService = PlaceService()

    fun getPredictions(text: String, latLng: String) {
        placeService
                .api
                .getPredictions(APIKEY, text, latLng, "100")
                .enqueue(object : Callback<Prediction> {
                    override fun onResponse(call: Call<Prediction>, response: Response<Prediction>) {
                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result != null) {
                                mListener.getPredictionsList(result.predictions)
                            }
                        }
                    }

                    override fun onFailure(call: Call<Prediction>, t: Throwable) {
                        try {
                            throw InterruptedException("Error communicating with the server")
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }

                    }
                })
    }

    fun getResultFromPlaceId(placeId: String) {
        placeService
                .api
                .getDetails(APIKEY, placeId)
                .enqueue(object : Callback<PlaceDetails> {
                    override fun onResponse(call: Call<PlaceDetails>, response: Response<PlaceDetails>) {
                        if (response.isSuccessful) {
                            val placeDetails = response.body()
                            if (placeDetails != null) {
                                mListener.getResult(placeDetails.result!!)
                            }
                        }
                    }

                    override fun onFailure(call: Call<PlaceDetails>, t: Throwable) {
                        try {
                            throw InterruptedException("Error communicating with the server")
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }

                    }
                })
    }

    companion object {
        private const val APIKEY = BuildConfig.PLACES_API_KEY
    }

}
