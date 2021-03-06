package com.jdoneill.weathermap.model

import com.google.gson.annotations.SerializedName

class WeatherData {

    @SerializedName("name")
    var name: String = ""

    var weather = arrayListOf<Weather>()
    var main: Main = Main()
    var sys: Sys = Sys()
}

class Weather {
    var name: String = ""
    var description: String = ""
}

class Main {
    var temp: Float = 0.0f
    @SerializedName("temp_max")
    var maxTemp: Float = 0.0f
    @SerializedName("temp_min")
    var minTemp: Float = 0.0f
}

class Sys {
    @SerializedName("sunrise")
    var sunrise: Long = 0
    @SerializedName("sunset")
    var sunset: Long = 0
}
