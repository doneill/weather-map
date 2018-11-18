package com.jdoneill.weathermap.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class StructuredFormatting {

    @SerializedName("main_text")
    @Expose
    val mainText: String? = null

    @SerializedName("secondary_text")
    @Expose
    val secondaryText: String? = null

}
