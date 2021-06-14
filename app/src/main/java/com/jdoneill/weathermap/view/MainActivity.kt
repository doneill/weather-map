package com.jdoneill.weathermap.view

import android.Manifest
import android.annotation.SuppressLint
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
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment

import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.view.*
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

import com.jdoneill.weathermap.BuildConfig
import com.jdoneill.weathermap.R
import com.jdoneill.weathermap.databinding.ActivityMainBinding
import com.jdoneill.weathermap.util.GeometryUtil

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.jetbrains.anko.selector

const val APIKEY = BuildConfig.API_KEY
const val ARCGIS_KEY = BuildConfig.ARCGIS_KEY
// degree sign
const val DEGREE: String = "\u00B0"

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LATLNG: String = "com.jdoneill.placesearch.LATLNG"
    }

    // view bindings
    private val mainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val mapView: MapView by lazy {
        mainBinding.mapView
    }

    private val locationDisplay: LocationDisplay by lazy { mapView.locationDisplay }

    private val locationButton: FloatingActionButton by lazy {
        mainBinding.locationFab
    }

    // mapping
    private lateinit var map: ArcGISMap
    private lateinit var viewModel: MainViewModel
    private lateinit var mapOverlay: GraphicsOverlay
    private lateinit var mapCallout: Callout
    // menu items
    private lateinit var placeSearchItem: MenuItem
    // runtime permissions
    private var reqPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION)

    //----------------------------------------------------------------------------------------------
    // lifecycle methods
    //----------------------------------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = mainBinding.root
        setContentView(view)

        setSupportActionBar(mainBinding.toolbar)
        setupMap()

        viewModel = ViewModelProvider(this, MainViewModel.FACTORY(map))
                .get(MainViewModel::class.java)

        mapOverlay = addGraphicsOverlay(mapView)

        requestPermissions()

        val extras = intent.extras
        if (extras != null) {
            zoomToPlaceResult(extras.getDouble(PlaceSearchActivity.EXTRA_PLACE_LONGITUDE),
                    extras.getDouble(PlaceSearchActivity.EXTRA_PLACE_LATITUDE))
        }
        else if (locationDisplay.isStarted) {
            zoomToLocation(locationDisplay.location.position)
        }

        val weatherLayerTypes = getWeatherLayerTypes()

        mainBinding.layerFab.setOnClickListener {
            selector(getString(R.string.layer_title), weatherLayerTypes) { _, i ->
                when {
                    weatherLayerTypes[i] == "Clear Layers" -> map.operationalLayers.clear()
                    weatherLayerTypes[i] == "Precipitation" -> addOperationalLayer("precipitation_new")
                    weatherLayerTypes[i] == "Temperature" -> addOperationalLayer("temp_new")
                }
            }
        }

        mapView.onTouchListener = object : DefaultMapViewOnTouchListener(this, mapView) {
            override fun onSingleTapConfirmed(motionEvent: MotionEvent?): Boolean {
                if (mapView.callout.isShowing) {
                    mapOverlay.graphics.clear()
                    mapView.callout.dismiss()
                }
                return super.onSingleTapConfirmed(motionEvent)
            }

            override fun onLongPress(motionEvent: MotionEvent?) {
                super.onLongPress(motionEvent)

                val screenPoint: android.graphics.Point = android.graphics.Point(
                        motionEvent!!.x.toInt(), motionEvent.y.toInt())

                if (locationDisplay.isStarted) {
                    zoomToLocation(mapView.screenToLocation(screenPoint))
                }

            }
        }

        // turn on/off location display
        locationButton.setOnClickListener {
            if (!locationDisplay.isStarted) {
                locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.RECENTER
                locationDisplay.startAsync()

                if (locationDisplay.location != null) {
                    if (locationDisplay.location.position != null) {
                        zoomToLocation(locationDisplay.location.position)
                    }
                }
            } else {
                locationDisplay.stop()
                viewModel.displayMessage(getString(R.string.location_settings))
            }
        }

        // allow fab to reposition based on attribution bar layout
        val params = locationButton.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
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

    override fun onDestroy() {
        super.onDestroy()
        mapView.dispose()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationDisplay.startAsync()
        } else {
            viewModel.displayMessage(getString(R.string.location_settings))
        }
    }

    //----------------------------------------------------------------------------------------------
    // private methods
    //----------------------------------------------------------------------------------------------

    private fun setupMap() {
        ArcGISRuntimeEnvironment.setApiKey(ARCGIS_KEY)
        map = ArcGISMap(BasemapStyle.ARCGIS_LIGHT_GRAY)
        mapView.map = map
    }

    private fun requestPermissions() {
        val requestCode = 2
        // fine location permission
        val permissionCheckFineLocation =
            ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[0]) ==
                    PackageManager.PERMISSION_GRANTED
        // coarse location permission
        val permissionCheckCoarseLocation =
            ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[1]) ==
                    PackageManager.PERMISSION_GRANTED
        if (!(permissionCheckFineLocation && permissionCheckCoarseLocation)) {
            ActivityCompat.requestPermissions(this@MainActivity, reqPermissions, requestCode)
        } else {
            if (!locationDisplay.isStarted) {
                locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.RECENTER
                locationDisplay.startAsync()
            }
            viewModel.displayMessage("app has location permissions")
        }
    }

    private fun getWeatherLayerTypes(): List<String> {
        return listOf(getString(R.string.layer_clear),
                getString(R.string.layer_precip),
                getString(R.string.layer_temp) )
    }

    private fun addOperationalLayer(type: String) {
        map.operationalLayers.clear()
        val openPrecipLayer = viewModel.loadWeatherLayer(type)

        openPrecipLayer.addDoneLoadingListener {
            if (openPrecipLayer.loadStatus == LoadStatus.LOADED) {
                map.operationalLayers.add(openPrecipLayer)
            }
        }
        if (mapView.mapScale < 4000000.0) mapView.setViewpointScaleAsync(4000000.0)
    }

    private fun addGraphicsOverlay(mapView: MapView): GraphicsOverlay {
        // create graphics overlay
        val graphicsOverlay = GraphicsOverlay()
        // add overlay to MapView
        mapView.graphicsOverlays.add(graphicsOverlay)
        return graphicsOverlay
    }

    private fun zoomToPlaceResult(lon: Double, lat: Double) {
        mapOverlay.graphics.clear()
        mapView.callout.dismiss()
        // create arcgis point
        val placePnt = Point(lon, lat, SpatialReferences.getWgs84())
        // get the weather
        CoroutineScope(Dispatchers.IO).launch {
            val weatherResponse = viewModel.weatherDataResponse(placePnt)
            withContext(Dispatchers.Main) {
                zoomToMarkerWithCallout(weatherResponse, placePnt, mapOverlay)
            }
        }
    }

    private fun zoomToLocation(point: Point) {
        mapOverlay.graphics.clear()
        mapView.callout.dismiss()

        CoroutineScope(Dispatchers.IO).launch {
            val weatherResponse = viewModel.weatherDataResponse(point)
            withContext(Dispatchers.Main) {
                zoomToMarkerWithCallout(weatherResponse, point, mapOverlay)
            }
        }
    }

    private fun zoomToMarkerWithCallout(weatherResponse: Map<String, Any>, mapPoint: Point, dataOverlay: GraphicsOverlay) {
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

    private fun openPlaceSearchActivity() {
        val intent = Intent(this, PlaceSearchActivity::class.java)

        val centerPnt = GeometryUtil.convertToWgs84(mapView.visibleArea.extent.center)
        val lat = centerPnt.x.toString()
        val lon = centerPnt.y.toString()

        intent.putExtra(EXTRA_LATLNG, "$lat, $lon")
        startActivity(intent)
    }

}
