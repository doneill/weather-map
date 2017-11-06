/* Copyright 2017 jdoneill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.jdoneill.weathermap.data

import com.google.gson.annotations.SerializedName


class Weather{

    @SerializedName("name")
    var name: String = ""

    var main: Main = Main()
    var sys: Sys = Sys()
}

class Main{
    var temp: Float = 0.0f
    @SerializedName("temp_max")
    var minTemp: Float = 0.0f
    @SerializedName("temp_min")
    var maxTemp: Float = 0.0f
}

class Sys{
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