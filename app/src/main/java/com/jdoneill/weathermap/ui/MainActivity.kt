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
import com.esri.arcgisruntime.mapping.view.*
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol

import com.jdoneill.weathermap.BuildConfig
import com.jdoneill.weathermap.R
import com.jdoneill.weathermap.data.Weather
import com.jdoneill.weathermap.presenter.WeatherClient
import com.jdoneill.weathermap.util.GeometryUtil

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

const val APIKEY = BuildConfig.API_KEY
// degree sign
const val DEGREE: String = "\u00B0"

class MainActivity : AppCompatActivity(), AnkoLogger {

    // mapping
    private lateinit var mMap: ArcGISMap
    private lateinit var mOverlay: GraphicsOverlay
    private lateinit var mCallout: Callout
    private lateinit var mLocationDisplay: LocationDisplay
    // menu items
    private lateinit var placeSearchItem: MenuItem
    // runtime permissions
    private var reqPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    // subdomains for web tiled layer
    private var subDomains = Arrays.asList("a")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val extras = intent.extras

        // show mMap
        mMap = ArcGISMap(Basemap.createDarkGrayCanvasVector())
        mapView.map = mMap

        // graphics overlay for tapped location marker
        mOverlay = addGraphicsOverlay(mapView)

        // get the MapView location display
        mLocationDisplay = mapView.locationDisplay

        if (extras != null) {
            val lat:Double = extras.getDouble(PlaceSearchActivity.EXTRA_PLACE_LATITUDE)
            val lon:Double = extras.getDouble(PlaceSearchActivity.EXTRA_PLACE_LONGITUDE)
            mOverlay.graphics.clear()
            mapView.callout.dismiss()
            // create arcgis point
            val placePnt = Point(lon, lat, SpatialReferences.getWgs84())
            // get the weather
            weatherAtLocation(placePnt, mOverlay)
        } else {
            mMap.addDoneLoadingListener {
                val centerPnt = mLocationDisplay.location.position
                weatherAtLocation(centerPnt, mOverlay)
            }
        }

        // permission state
        val permFineLoc = (ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED)
        val permCoarseLoc = (ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[1]) == PackageManager.PERMISSION_GRANTED)
        // check if permissions needed
        if(permFineLoc && permCoarseLoc){
            // have required permissions
            mLocationDisplay.startAsync()
        }else{
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
                    weatherLayer[i] == "Clear Layers" -> mMap.operationalLayers.clear()
                    // add precipitation layer
                    weatherLayer[i] == "Precipitation" -> {
                        mMap.operationalLayers.clear()
                        // add open weather precipitation layer
                        val templateUri = "http://{subDomain}.tile.openweathermap.org/mMap/precipitation_new/{level}/{col}/{row}.png?appid=$APIKEY"
                        val openPrecipLayer = WebTiledLayer(templateUri, subDomains)
                        openPrecipLayer.loadAsync()
                        openPrecipLayer.addDoneLoadingListener {
                            if(openPrecipLayer.loadStatus == LoadStatus.LOADED){
                                info { "Open precip layer loaded" }
                                mMap.operationalLayers.add(openPrecipLayer)
                            }
                        }
                        // zoom out to see layer
                        if(mapView.mapScale < 4000000.0) mapView.setViewpointScaleAsync(4000000.0)
                    }
                    // add temperature layer
                    weatherLayer[i] == "Temperature" -> {
                        mMap.operationalLayers.clear()
                        // add open weather temperature layer
                        val templateUri = "http://{subDomain}.tile.openweathermap.org/mMap/temp_new/{level}/{col}/{row}.png?appid=$APIKEY"
                        val openTempLayer = WebTiledLayer(templateUri, subDomains)
                        openTempLayer.loadAsync()
                        openTempLayer.addDoneLoadingListener {
                            if(openTempLayer.loadStatus == LoadStatus.LOADED){
                                info { "Open precip layer loaded" }
                                mMap.operationalLayers.add(openTempLayer)
                            }
                        }
                        // zoom out to see layer
                        if(mapView.mapScale < 4000000.0) mapView.setViewpointScaleAsync(4000000.0)
                    }
                }
            }
        }

        // respond to mapview interactions
        mapView.onTouchListener = object : DefaultMapViewOnTouchListener( this, mapView) {

            override fun onSingleTapConfirmed(motionEvent: MotionEvent?): Boolean {
                if (mapView.callout.isShowing) {
                    // clear any graphics and callouts
                    mOverlay.graphics.clear()
                    mapView.callout.dismiss()
                }
                return super.onSingleTapConfirmed(motionEvent)
            }

            override fun onLongPress(motionEvent: MotionEvent?) {
                // clear any graphics and callouts
                mOverlay.graphics.clear()
                mapView.callout.dismiss()
                // get the point that was clicked and convert it to a point in mMap coordinates
                val screenPoint: android.graphics.Point = android.graphics.Point(motionEvent!!.x.toInt(), motionEvent.y.toInt())
                // create a mMap point from screen point
                val mapPoint: Point = mapView.screenToLocation(screenPoint)
                // get the weather at tapped location
                weatherAtLocation(mapPoint, mOverlay)
                super.onLongPress(motionEvent)
            }
        }

        // turn on/off location display
        locationFab.setOnClickListener {
            if(mLocationDisplay.isStarted){
                mLocationDisplay.stop()
            }else{
                // clear any graphics and callouts
                mOverlay.graphics.clear()
                mapView.callout.dismiss()
                // start location display
                mLocationDisplay.startAsync()
                // zoom to location and display weather
                val centerPnt = mLocationDisplay.location.position
                weatherAtLocation(centerPnt, mOverlay)
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

    private inline fun consume(f: () -> Unit): Boolean{
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray){
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationDisplay.startAsync()
        } else {
            toast("denied")
        }
    }

    /**
     * Create a Graphics Overlay
     *
     * @param mapView MapView to add the graphics overlay to
     */
    private fun addGraphicsOverlay(mapView: MapView) : GraphicsOverlay {
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
    private fun weatherAtLocation(location: Point, graphicOverlay: GraphicsOverlay){
        val wgs84Pnt = GeometryUtil.convertToWgs84(location)
        val network = WeatherClient()
        val call = network.getWeatherForCoord(wgs84Pnt.y.toFloat(), wgs84Pnt.x.toFloat())
        call.enqueue(object : Callback<Weather> {
            override fun onResponse(call: Call<Weather>?, response: Response<Weather>?) {
                val weather: Weather? = response?.body()
                // present date if main not null
                weather?.let { presentData(it, location, graphicOverlay) }

                val name = weather?.name
                val sys = weather?.sys
                val sunrise = sys?.sunrise
                val sunset = sys?.sunset
                println("station name: $name | sunrise: $sunrise | sunset: $sunset")
            }

            override fun onFailure(call: Call<Weather>?, t: Throwable?) {
                t?.printStackTrace()
            }
        })
    }

    /**
     * Present the weather data in a Callout
     *
     * @param weather weather data
     * @param mapPoint location to show Callout
     * @param dataOverlay GraphicsOverlay to add Marker
     */
    private fun presentData(weather: Weather, mapPoint: Point, dataOverlay: GraphicsOverlay) = with(weather){
        val cityName = name
        val temp = main.temp
        val highTemp = main.minTemp
        val lowTemp = main.maxTemp

        // create a marker at tapped location
        val locationMarker = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CROSS, Color.BLACK, 15.0f)
        val locationGraphic = Graphic(mapPoint, locationMarker)
        dataOverlay.graphics.add(locationGraphic)

        // create a textview for the mCallout
        val calloutContent = TextView(applicationContext)
        calloutContent.setTextColor(Color.BLACK)
        // create text from string resource
        val calloutText = getString(R.string.callout_text, cityName, temp, DEGREE, highTemp, DEGREE, lowTemp, DEGREE)
        calloutContent.text = calloutText
        // get mCallout, set content and geoelement graphic
        mCallout = mapView.callout
        mCallout.content = calloutContent
        mCallout.setGeoElement(locationGraphic, mapPoint)
        mCallout.show()
        // center on the location, zoom in when scaled out
        val mapScale = mapView.mapScale
        if(mapScale < 350000.0){
            mapView.setViewpointCenterAsync(mapPoint)
        }else{
            mapView.setViewpointCenterAsync(mapPoint, 10500.0)
        }
    }

    /**
     * Notification on selected place
     */
    private fun openPlaceSearchActivity() {
        val intent = Intent(this, PlaceSearchActivity::class.java)
        intent.putExtra(EXTRA_LATLNG, "47.498277,-121.783975")
        startActivity(intent)
    }

    companion object {
        const val EXTRA_LATLNG: String = "com.jdoneill.placesearch.LATLNG"
    }

}
