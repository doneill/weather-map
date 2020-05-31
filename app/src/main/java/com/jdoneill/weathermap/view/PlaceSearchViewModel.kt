package com.jdoneill.weathermap.view

import androidx.lifecycle.ViewModel

import com.jdoneill.weathermap.model.Predictions
import com.jdoneill.weathermap.util.singleArgViewModelFactory

import java.util.ArrayList
import java.util.HashMap

class PlaceSearchViewModel(private val latLng: String) : ViewModel() {

    companion object {
        // the map to pass the the model
        val FACTORY = singleArgViewModelFactory(::PlaceSearchViewModel)
    }

    fun placeFromPrediction(places: Any, predictions: List<Predictions>): String {
        var placeId = ""
        var placeName = ""
        var description = ""

        if (places is HashMap<*, *>) {
            for (place in places.entries) {
                if (place.key == "place") {
                    placeName = place.value as String
                } else if (place.key == "desc") {
                    description = place.value as String
                }
            }

            for (i in predictions.indices) {
                if (placeName == predictions[i].structuredFormatting.mainText &&
                    description == predictions[i].structuredFormatting.secondaryText) {
                    placeId = predictions[i].placeId
                    break
                }
            }
        }
        return placeId
    }

    fun listOfPredictions(predictions: List<Predictions>): ArrayList<HashMap<String, String>> {
        val places = ArrayList<HashMap<String, String>>()
        var results: HashMap<String, String>

        for (i in predictions.indices) {
            results = HashMap()
            results["place"] = predictions[i].structuredFormatting.mainText
            results["desc"] = predictions[i].structuredFormatting.secondaryText
            places.add(results)
        }

        return places
    }
}
