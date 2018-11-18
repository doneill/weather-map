package com.jdoneill.weathermap.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Predictions {

    @SerializedName("description")
    @Expose
    var description: String? = null

    @SerializedName("id")
    @Expose
    var id: String? = null

    @SerializedName("place_id")
    @Expose
    val placeId: String? = null

    @SerializedName("reference")
    @Expose
    var reference: String? = null

    @SerializedName("structured_formatting")
    @Expose
    val structuredFormatting: StructuredFormatting? = null

}
