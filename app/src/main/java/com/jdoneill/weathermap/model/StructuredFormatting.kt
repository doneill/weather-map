package com.jdoneill.weathermap.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class StructuredFormatting {

    @SerializedName("main_text")
    @Expose
    val mainText: String = ""

    @SerializedName("secondary_text")
    @Expose
    val secondaryText: String = ""

}
