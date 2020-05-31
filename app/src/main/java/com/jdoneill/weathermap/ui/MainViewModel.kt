package com.jdoneill.weathermap.ui

import androidx.lifecycle.ViewModel

import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.mapping.ArcGISMap

import com.jdoneill.weathermap.data.WeatherData
import com.jdoneill.weathermap.presenter.WeatherClient
import com.jdoneill.weathermap.util.GeometryUtil
import com.jdoneill.weathermap.util.singleArgViewModelFactory

class MainViewModel(private val map: ArcGISMap) : ViewModel() {

    companion object {
        // the map to pass the the model
        val FACTORY = singleArgViewModelFactory(::MainViewModel)
    }

    /**
     * Get weather from location
     *
     * @param location Location as Point
     */
    suspend fun weatherAtLocation(location: Point) : WeatherData {
        // check incoming location sr for api compatibility
        val wgs84Pnt = if (location.spatialReference != SpatialReferences.getWgs84()) {
            GeometryUtil.convertToWgs84(location)
        } else {
            location
        }

        val network = WeatherClient()
        return network.getWeatherForCoord(wgs84Pnt.y.toFloat(), wgs84Pnt.x.toFloat())
    }
}