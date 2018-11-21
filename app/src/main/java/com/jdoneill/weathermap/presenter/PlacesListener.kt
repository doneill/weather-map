package com.jdoneill.weathermap.presenter

import com.jdoneill.weathermap.model.Predictions
import com.jdoneill.weathermap.model.Result

interface PlacesListener {

    fun getPredictionsList(predictions: List<Predictions>)

    fun getResult(result: Result)
}
