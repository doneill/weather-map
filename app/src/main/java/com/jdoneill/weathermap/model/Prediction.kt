package com.jdoneill.weathermap.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Prediction {

    @SerializedName("predictions")
    @Expose
    lateinit var predictions: List<Predictions>
}
