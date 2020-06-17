package com.jdoneill.weathermap.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.*
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.google.android.material.snackbar.Snackbar

import com.jdoneill.weathermap.BuildConfig
import com.jdoneill.weathermap.R
import com.jdoneill.weathermap.util.GeometryUtil

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.jetbrains.anko.selector

const val APIKEY = BuildConfig.API_KEY
// degree sign
const val DEGREE: String = "\u00B0"

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LATLNG: String = "com.jdoneill.placesearch.LATLNG"
    }

    // mapping
    private lateinit var map: ArcGISMap
    private lateinit var viewModel: MainViewModel
    private lateinit var mapOverlay: GraphicsOverlay
    private lateinit var mapCallout: Callout
    private lateinit var locationDisplay: LocationDisplay
    // menu items
    private lateinit var placeSearchItem: MenuItem
    // runtime permissions
    private var reqPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    // subdomains for web tiled layer
    private var subDomains = listOf("a")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        val extras = intent.extras

        map = ArcGISMap(Basemap.createLightGrayCanvas())
        mapView.map = map

        viewModel = ViewModelProvider(this, MainViewModel.FACTORY(map)).get(MainViewModel::class.java)
        // graphics overlay for tapped location marker
        mapOverlay = addGraphicsOverlay(mapView)

        // get the MapView location display
        locationDisplay = mapView.locationDisplay

        if (extras != null) {
            val lat: Double = extras.getDouble(PlaceSearchActivity.EXTRA_PLACE_LATITUDE)
            val lon: Double = extras.getDouble(PlaceSearchActivity.EXTRA_PLACE_LONGITUDE)
            mapOverlay.graphics.clear()
            mapView.callout.dismiss()
            // create arcgis point
            val placePnt = Point(lon, lat, SpatialReferences.getWgs84())
            // get the weather
            CoroutineScope(Dispatchers.IO).launch {
                val weatherResponse = viewModel.weatherDataResponse(placePnt)
                withContext(Dispatchers.Main) {
                    showCallout(weatherResponse, placePnt, mapOverlay)
                }
            }
        }
        else if (locationDisplay.isStarted) {
            map.addDoneLoadingListener {
                val centerPnt = locationDisplay.location.position

                CoroutineScope(Dispatchers.IO).launch {
                    val weatherResponse = viewModel.weatherDataResponse(centerPnt)
                    withContext(Dispatchers.Main) {
                        showCallout(weatherResponse, centerPnt, mapOverlay)
                    }
                }
            }
        }

        // permission state
        val permFineLoc = (ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED)
        val permCoarseLoc = (ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[1]) == PackageManager.PERMISSION_GRANTED)
        // check if permissions needed
        if (permFineLoc && permCoarseLoc) {
            // have required permissions
            locationDisplay.startAsync()
        } else {
            // request permissions at runtime
            val requestCode = 2
            ActivityCompat.requestPermissions(this@MainActivity, reqPermissions, requestCode)
        }

        // weather layer selector
        val weatherLayer = listOf(
                                            getString(R.string.layer_clear),
                                            getString(R.string.layer_precip),
                                            getString(R.string.layer_temp) )

        layerFab.setOnClickListener {
            selector(getString(R.string.layer_title), weatherLayer) { _, i ->
                when {
                    // clear all layers
                    weatherLayer[i] == "Clear Layers" -> map.operationalLayers.clear()
                    // add precipitation layer
                    weatherLayer[i] == "Precipitation" -> {
                        map.operationalLayers.clear()
                        val openPrecipLayer = viewModel.loadWeatherLayer("precipitation_new")

                        openPrecipLayer.addDoneLoadingListener {
                            if (openPrecipLayer.loadStatus == LoadStatus.LOADED) {
                                map.operationalLayers.add(openPrecipLayer)
                            }
                        }
                        // zoom out to see layer
                        if (mapView.mapScale < 4000000.0) mapView.setViewpointScaleAsync(4000000.0)
                    }
                    // add temperature layer
                    weatherLayer[i] == "Temperature" -> {
                        map.operationalLayers.clear()
                        val openTempLayer = viewModel.loadWeatherLayer("temp_new")

                        openTempLayer.addDoneLoadingListener {
                            if (openTempLayer.loadStatus == LoadStatus.LOADED) {
                                map.operationalLayers.add(openTempLayer)
                            }
                        }
                        // zoom out to see layer
                        if (mapView.mapScale < 4000000.0) mapView.setViewpointScaleAsync(4000000.0)
                    }
                }
            }
        }

        // respond to mapview interactions
        mapView.onTouchListener = object : DefaultMapViewOnTouchListener(this, mapView) {

            override fun onSingleTapConfirmed(motionEvent: MotionEvent?): Boolean {
                if (mapView.callout.isShowing) {
                    // clear any graphics and callouts
                    mapOverlay.graphics.clear()
                    mapView.callout.dismiss()
                }
                return super.onSingleTapConfirmed(motionEvent)
            }

            override fun onLongPress(motionEvent: MotionEvent?) {
                super.onLongPress(motionEvent)
                // clear any graphics and callouts
                mapOverlay.graphics.clear()
                mapView.callout.dismiss()
                // get the point that was clicked and convert it to a point in mMap coordinates
                val screenPoint: android.graphics.Point = android.graphics.Point(motionEvent!!.x.toInt(), motionEvent.y.toInt())
                // create a mMap point from screen point
                val mapPoint: Point = mapView.screenToLocation(screenPoint)
                // get the weather at tapped location
                CoroutineScope(Dispatchers.IO).launch {
                    val weatherResponse = viewModel.weatherDataResponse(mapPoint)
                    withContext(Dispatchers.Main) {
                        showCallout(weatherResponse, mapPoint, mapOverlay)
                    }
                }
            }
        }

        // turn on/off location display
        locationFab.setOnClickListener {
            if (locationDisplay.isStarted) {
                // clear any graphics and callouts
                mapOverlay.graphics.clear()
                mapView.callout.dismiss()
                // start location display
                locationDisplay.startAsync()
                // zoom to location and display weather
                val centerPnt = locationDisplay.location.position
                CoroutineScope(Dispatchers.IO).launch {
                    val weatherResponse = viewModel.weatherDataResponse(centerPnt)
                    withContext(Dispatchers.Main) {
                        showCallout(weatherResponse, centerPnt, mapOverlay)
                    }
                }
            } else {
                viewModel.displayMessage(getString(R.string.location_settings))
            }
        }

        // allow fab to reposition based on attribution bar layout
        val params = locationFab.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
        mapView.addAttributionViewLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            val heightDelta = bottom - oldBottom
            params.bottomMargin += heightDelta
        }

        viewModel.snackbar.observe(this, Observer {
            it?.let {
                Snackbar.make(mapView, it, Snackbar.LENGTH_SHORT).show()
                viewModel.onSnackbarShown()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        // assign menu items
        placeSearchItem = menu.getItem(0)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        // place search
        R.id.menu_search -> consume {
            // open auto complete intent
            openPlaceSearchActivity()
        }
        else -> super.onOptionsItemSelected(item)
    }

    private inline fun consume(f: () -> Unit): Boolean {
        f()
        return true
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationDisplay.startAsync()
        } else {
            viewModel.displayMessage(getString(R.string.location_settings))
        }
    }

    /**
     * Create a Graphics Overlay
     *
     * @param mapView MapView to add the graphics overlay to
     */
    private fun addGraphicsOverlay(mapView: MapView): GraphicsOverlay {
        // create graphics overlay
        val graphicsOverlay = GraphicsOverlay()
        // add overlay to MapView
        mapView.graphicsOverlays.add(graphicsOverlay)
        return graphicsOverlay
    }

    /**
     * Present the weather data in a Callout
     *
     * @param weatherResponse weather data response text
     * @param mapPoint location to show Callout
     * @param dataOverlay GraphicsOverlay to add Marker
     */
    private fun showCallout(weatherResponse: Map<String, Any>, mapPoint: Point, dataOverlay: GraphicsOverlay) {
        // create a marker at tapped location
        val locationMarker = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 15.0f)
        val locationGraphic = Graphic(mapPoint, locationMarker)
        dataOverlay.graphics.add(locationGraphic)

        // create a textview for the callout
        val calloutContent = TextView(applicationContext)
        calloutContent.setTextColor(Color.BLACK)

        val name = weatherResponse["name"] as String
        val temp = weatherResponse["temp"] as Float
        val maxTemp = weatherResponse["maxTemp"] as Float
        val minTemp = weatherResponse["minTemp"] as Float
        val rise = weatherResponse["rise"] as String
        val set = weatherResponse["set"] as String
        
        calloutContent.text = getString(R.string.callout_text, name, temp, DEGREE, maxTemp, DEGREE, minTemp, DEGREE, rise, set)
        mapCallout = mapView.callout
        mapCallout.content = calloutContent
        mapCallout.setGeoElement(locationGraphic, mapPoint)
        mapCallout.show()
        // center on the location, zoom in when scaled out
        val mapScale = mapView.mapScale
        if (mapScale < 350000.0) {
            mapView.setViewpointCenterAsync(mapPoint)
        } else {
            mapView.setViewpointCenterAsync(mapPoint, 10500.0)
        }
    }

    /**
     * Notification on selected place
     */
    private fun openPlaceSearchActivity() {
        val intent = Intent(this, PlaceSearchActivity::class.java)

        val centerPnt = GeometryUtil.convertToWgs84(mapView.visibleArea.extent.center)
        val lat = centerPnt.x.toString()
        val lon = centerPnt.y.toString()

        intent.putExtra(EXTRA_LATLNG, "$lat, $lon")
        startActivity(intent)
    }

}
