package com.jdoneill.weathermap.ui

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

import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.layers.WebTiledLayer
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.Callout
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.LocationDisplay
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol

import com.jdoneill.weathermap.BuildConfig
import com.jdoneill.weathermap.R
import com.jdoneill.weathermap.data.Weather
import com.jdoneill.weathermap.presenter.WeatherClient
import com.jdoneill.weathermap.util.GeometryUtil

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val APIKEY = BuildConfig.API_KEY
// degree sign
const val DEGREE: String = "\u00B0"

class MainActivity : AppCompatActivity(), AnkoLogger {

    // mapping
    private lateinit var map: ArcGISMap
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

        map = ArcGISMap(Basemap.createLightGrayCanvasVector())
        mapView.map = map
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
            weatherAtLocation(placePnt, mapOverlay)
        } else {
            map.addDoneLoadingListener {
                val centerPnt = locationDisplay.location.position
                weatherAtLocation(centerPnt, mapOverlay)
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
        val weatherLayer = listOf("Clear Layers", "Precipitation", "Temperature")
        layerFab.setOnClickListener {
            selector("Weather Layers", weatherLayer) { _, i ->
                when {
                    // clear all layers
                    weatherLayer[i] == "Clear Layers" -> map.operationalLayers.clear()
                    // add precipitation layer
                    weatherLayer[i] == "Precipitation" -> {
                        map.operationalLayers.clear()
                        // add open weather precipitation layer
                        val templateUri = "http://{subDomain}.tile.openweathermap.org/map/precipitation_new/{level}/{col}/{row}.png?appid=$APIKEY"
                        val openPrecipLayer = WebTiledLayer(templateUri, subDomains)
                        openPrecipLayer.loadAsync()
                        openPrecipLayer.addDoneLoadingListener {
                            if (openPrecipLayer.loadStatus == LoadStatus.LOADED) {
                                info { "Open precip layer loaded" }
                                map.operationalLayers.add(openPrecipLayer)
                            }
                        }
                        // zoom out to see layer
                        if (mapView.mapScale < 4000000.0) mapView.setViewpointScaleAsync(4000000.0)
                    }
                    // add temperature layer
                    weatherLayer[i] == "Temperature" -> {
                        map.operationalLayers.clear()
                        // add open weather temperature layer
                        val templateUri = "http://{subDomain}.tile.openweathermap.org/map/temp_new/{level}/{col}/{row}.png?appid=$APIKEY"
                        val openTempLayer = WebTiledLayer(templateUri, subDomains)
                        openTempLayer.loadAsync()
                        openTempLayer.addDoneLoadingListener {
                            if (openTempLayer.loadStatus == LoadStatus.LOADED) {
                                info { "Open precip layer loaded" }
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
                weatherAtLocation(mapPoint, mapOverlay)
            }
        }

        // turn on/off location display
        locationFab.setOnClickListener {
            if (locationDisplay.isStarted) {
                locationDisplay.stop()
            } else {
                // clear any graphics and callouts
                mapOverlay.graphics.clear()
                mapView.callout.dismiss()
                // start location display
                locationDisplay.startAsync()
                // zoom to location and display weather
                val centerPnt = locationDisplay.location.position
                weatherAtLocation(centerPnt, mapOverlay)
            }
        }

        // allow fab to reposition based on attribution bar layout
        val params = locationFab.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
        mapView.addAttributionViewLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            val heightDelta = bottom - oldBottom
            params.bottomMargin += heightDelta
        }
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
            toast("denied")
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
     * Get weather from location
     *
     * @param location Location as Point
     */
    private fun weatherAtLocation(location: Point, graphicOverlay: GraphicsOverlay) {
        // check incoming location sr for api compatibility
        val wgs84Pnt = if (location.spatialReference != SpatialReferences.getWgs84()) {
            GeometryUtil.convertToWgs84(location)
        } else {
            location
        }

        val network = WeatherClient()

        CoroutineScope(Dispatchers.IO).launch {
            val weather = network.getWeatherForCoord(wgs84Pnt.y.toFloat(), wgs84Pnt.x.toFloat())
            withContext(Dispatchers.Main) {
                presentData(weather, location, graphicOverlay)
            }
        }
    }

    /**
     * Present the weather data in a Callout
     *
     * @param weather weather data
     * @param mapPoint location to show Callout
     * @param dataOverlay GraphicsOverlay to add Marker
     */
    private fun presentData(weather: Weather, mapPoint: Point, dataOverlay: GraphicsOverlay) = with(weather) {
        val cityName = name
        val temp = main.temp
        val highTemp = main.minTemp
        val lowTemp = main.maxTemp

        val sunrise = sys.sunrise.times(1000)
        val sunset = sys.sunset.times(1000)

        val df = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        val rise = df.format(sunrise.let { Date(it).time })
        val set = df.format(sunset.let { Date(it).time })

        // create a marker at tapped location
        val locationMarker = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 15.0f)
        val locationGraphic = Graphic(mapPoint, locationMarker)
        dataOverlay.graphics.add(locationGraphic)

        // create a textview for the mCallout
        val calloutContent = TextView(applicationContext)
        calloutContent.setTextColor(Color.BLACK)
        // create text from string resource
        val calloutText = getString(R.string.callout_text, cityName, temp, DEGREE, highTemp, DEGREE, lowTemp, DEGREE, rise, set)
        calloutContent.text = calloutText
        // get mapCallout, set content and geoelement graphic
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

    companion object {
        const val EXTRA_LATLNG: String = "com.jdoneill.placesearch.LATLNG"
    }
}
