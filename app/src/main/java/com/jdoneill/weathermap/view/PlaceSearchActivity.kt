package com.jdoneill.weathermap.view

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ListView
import android.widget.SearchView
import android.widget.SimpleAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.jdoneill.weathermap.R
import com.jdoneill.weathermap.model.Predictions
import com.jdoneill.weathermap.model.Result
import com.jdoneill.weathermap.presenter.PlaceAutocomplete
import com.jdoneill.weathermap.presenter.PlacesListener
import java.util.ArrayList
import java.util.HashMap

class PlaceSearchActivity : AppCompatActivity(), PlacesListener {

    private lateinit var placeAutocomplete: PlaceAutocomplete
    private lateinit var predictions: List<Predictions>
    private lateinit var placesListView: ListView
    private lateinit var latLng: String
    private lateinit var placeName: String
    private lateinit var description: String

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_search_list)

        val toolbar = findViewById<Toolbar>(R.id.search_toolbar)
        setSupportActionBar(toolbar)

        // get the intent
        val intent = intent
        
        latLng = intent.getStringExtra(MainActivity.EXTRA_LATLNG)

        placesListView = findViewById(R.id.lvPlaces)

        placesListView.setOnItemClickListener { _, _, pos, _ ->
            var placeId = ""
            val items = placesListView.getItemAtPosition(pos)

            if (items is HashMap<*, *>) {
                for (item in items.entries) {
                    if (item.key == "place") {
                        placeName = item.value as String
                    } else if (item.key == "desc") {
                        description = item.value as String
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
            placeAutocomplete.getResultFromPlaceId(placeId)
        }

        placeAutocomplete = PlaceAutocomplete(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.search, menu)

        val searchMenuItem = menu.findItem(R.id.placeSearch)
        searchMenuItem.expandActionView()

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        (menu.findItem(R.id.placeSearch).actionView!! as SearchView).apply {
            // Assumes current activity is the searchable activity
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isIconifiedByDefault = false // Do not iconify the widget; expand it by default
            requestFocus()

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    placesListView.visibility = View.VISIBLE
                    placeAutocomplete.getPredictions(newText, latLng)
                    return false
                }
            })
        }

        return true
    }

    override fun getPredictionsList(predictions: List<Predictions>) {
        this.predictions = predictions

        val places = ArrayList<HashMap<String, String>>()
        var results: HashMap<String, String>

        for (i in this.predictions.indices) {
            results = HashMap()
            results["place"] = this.predictions[i].structuredFormatting.mainText
            results["desc"] = this.predictions[i].structuredFormatting.secondaryText
            places.add(results)
        }

        // Creating an simple 2 line adapter for list view
        val adapter = SimpleAdapter(this, places, android.R.layout.simple_list_item_2,
                arrayOf("place", "desc"),
                intArrayOf(android.R.id.text1, android.R.id.text2))

        placesListView.adapter = adapter
    }

    override fun getResult(result: Result) {
        val lat = result.geometry.location.lat
        val lon = result.geometry.location.lng

        openMapView(lat, lon)
    }

    /**
     * Notification on selected place
     */
    private fun openMapView(lat: Double, lon: Double) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(EXTRA_PLACE_LATITUDE, lat)
        intent.putExtra(EXTRA_PLACE_LONGITUDE, lon)
        startActivity(intent)
    }

    companion object {
        const val EXTRA_PLACE_LATITUDE = "com.jdoneill.placesearch.LATITUDE"
        const val EXTRA_PLACE_LONGITUDE = "com.jdoneill.placesearch.LONGITUDE"
    }
}
