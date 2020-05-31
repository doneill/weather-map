package com.jdoneill.weathermap.data

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
    var minTemp: Float = 0.0f
    @SerializedName("temp_min")
    var maxTemp: Float = 0.0f
}

class Sys {
    @SerializedName("sunrise")
    var sunrise: Long = 0
    @SerializedName("sunset")
    var sunset: Long = 0
}

class Constants {
    companion object {
        var HTTP_API_OPENWEATHERMAP_URL = "http://api.openweathermap.org/data/2.5/"
    }
}
