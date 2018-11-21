package com.jdoneill.weathermap.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Predictions {

    @SerializedName("place_id")
    @Expose
    val placeId: String = ""

    @SerializedName("structured_formatting")
    @Expose
    lateinit var structuredFormatting: StructuredFormatting

}
