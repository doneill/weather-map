package com.jdoneill.weathermap.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.mapping.ArcGISMap

import com.jdoneill.weathermap.presenter.WeatherClient
import com.jdoneill.weathermap.util.GeometryUtil
import com.jdoneill.weathermap.util.singleArgViewModelFactory

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class MainViewModel(private val map: ArcGISMap) : ViewModel() {

    companion object {
        // the map to pass the the model
        val FACTORY = singleArgViewModelFactory(::MainViewModel)
    }

    private val _snackBar = MutableLiveData<String?>()

    val snackbar: LiveData<String?>
        get() = _snackBar

    /**
     * Get weather from location
     *
     * @param location Location as Point
     */
    suspend fun weatherDataResponse(location: Point) : Map<String, Any> {
        // check incoming location sr for api compatibility
        val wgs84Pnt = if (location.spatialReference != SpatialReferences.getWgs84()) {
            GeometryUtil.convertToWgs84(location)
        } else {
            location
        }

        val response = mutableMapOf<String, Any>()

        val network = WeatherClient()
        val weatherData = network.getWeatherForCoord(wgs84Pnt.y.toFloat(), wgs84Pnt.x.toFloat())

        response["name"] = weatherData.name
        response["temp"] = weatherData.main.temp
        response["minTemp"] = weatherData.main.minTemp
        response["maxTemp"] = weatherData.main.maxTemp
        response["weather"] = weatherData.weather

        val sunrise = weatherData.sys.sunrise.times(1000)
        val sunset = weatherData.sys.sunset.times(1000)

        val df = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        val rise = df.format(sunrise.let { Date(it).time })
        val set = df.format(sunset.let { Date(it).time })

        response["rise"] = rise
        response["set"] = set

        return response
    }
}