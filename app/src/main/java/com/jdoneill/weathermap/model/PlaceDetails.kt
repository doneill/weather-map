package com.jdoneill.weathermap.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PlaceDetails {
    @SerializedName("result")
    @Expose
    var result: Result? = null
}
