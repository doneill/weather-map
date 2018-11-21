package com.jdoneill.weathermap.model

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName

class Location {
    @SerializedName("lat")
    @Expose
    val lat:Double = 0.0

    @SerializedName("lng")
    @Expose
    val lng:Double = 0.0
}