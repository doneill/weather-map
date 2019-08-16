package com.jdoneill.weathermap.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ListView
import android.widget.SearchView
import android.widget.SimpleAdapter

import com.jdoneill.weathermap.R
import com.jdoneill.weathermap.model.Predictions
import com.jdoneill.weathermap.model.Result
import com.jdoneill.weathermap.presenter.PlaceAutocomplete
import com.jdoneill.weathermap.presenter.PlacesListener

import java.util.ArrayList
import java.util.HashMap

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class PlaceSearchActivity : AppCompatActivity(), PlacesListener {

    private lateinit var mPlaceAutocomplete: PlaceAutocomplete
    private lateinit var mPredictions: List<Predictions>
    private lateinit var mPlacesListView: ListView
    private lateinit var mLatLng: String
    private lateinit var mPlaceName: String
    private lateinit var mDesc: String

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_search_list)

        val toolbar = findViewById<Toolbar>(R.id.search_toolbar)
        setSupportActionBar(toolbar)

        // get the intent
        val intent = intent
        mLatLng = intent.getStringExtra(MainActivity.EXTRA_LATLNG)

        mPlacesListView = findViewById(R.id.lvPlaces)

        mPlacesListView.setOnItemClickListener { _, _, pos, _ ->
            var placeId = ""
            val items = mPlacesListView.getItemAtPosition(pos)

            if (items is HashMap<*, *>) {
                for (item in items.entries) {
                    if (item.key == "place") {
                        mPlaceName = item.value as String
                    } else if (item.key == "desc") {
                        mDesc = item.value as String
                    }
                }

                for (i in mPredictions.indices) {
                    if (mPlaceName == mPredictions[i].structuredFormatting.mainText &&
                            mDesc == mPredictions[i].structuredFormatting.secondaryText) {
                        placeId = mPredictions[i].placeId
                        break
                    }
                }
            }
            mPlaceAutocomplete.getResultFromPlaceId(placeId)
        }

        mPlaceAutocomplete = PlaceAutocomplete(this)
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
            setIconifiedByDefault(false) // Do not iconify the widget; expand it by default
            requestFocus()

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    mPlacesListView.visibility = View.VISIBLE
                    mPlaceAutocomplete.getPredictions(newText, mLatLng)
                    return false
                }
            })
        }

        return true
    }

    override fun getPredictionsList(predictions: List<Predictions>) {
        this.mPredictions = predictions

        val places = ArrayList<HashMap<String, String>>()
        var results: HashMap<String, String>

        for (i in 0 until mPredictions.size) {
            results = HashMap()
            results["place"] = mPredictions[i].structuredFormatting.mainText
            results["desc"] = mPredictions[i].structuredFormatting.secondaryText
            places.add(results)
        }

        // Creating an simple 2 line adapter for list view
        val adapter = SimpleAdapter(this, places, android.R.layout.simple_list_item_2,
                arrayOf("place", "desc"),
                intArrayOf(android.R.id.text1, android.R.id.text2))

        mPlacesListView.adapter = adapter
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
