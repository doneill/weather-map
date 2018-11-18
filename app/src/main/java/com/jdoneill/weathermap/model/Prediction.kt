package com.jdoneill.weathermap.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Prediction {

    @SerializedName("predictions")
    @Expose
    val predictions: List<Predictions>? = null

}
