
/* Copyright 2017 jdoneill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.jdoneill.weathermap.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.annotation.NonNull
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.TextView

import com.esri.arcgisruntime.geometry.GeometryEngine
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

import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.ui.PlaceAutocomplete

import com.jdoneill.weathermap.BuildConfig
import com.jdoneill.weathermap.R
import com.jdoneill.weathermap.data.Weather

import kotlinx.android.synthetic.main.activity_main.locationFab
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_main.mapView

import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.util.*

const val APIKEY = BuildConfig.API_KEY
const val REQUEST_CODE_AUTOCOMPLETE = 1
// degree sign
const val DEGREE: String = "\u00B0"

class MainActivity : AppCompatActivity(), AnkoLogger {

    // mapping
    private lateinit var map: ArcGISMap
    private lateinit var mvOverlay: GraphicsOverlay
    private lateinit var callout: Callout
    private lateinit var locationDisplay: LocationDisplay
    // menu items
    private lateinit var clearLayersItem: MenuItem
    private lateinit var precipLayerItem: MenuItem
    private lateinit var tempLayerItem: MenuItem
    private lateinit var placeSearchItem: MenuItem
    // runtime permissions
    private var reqPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    // subdomains for web tiled layer
    private var subDomains = Arrays.asList("a")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // show map
        map = ArcGISMap(Basemap.createLightGrayCanvas())
        mapView.map = map

        // graphics overlay for tapped location marker
        mvOverlay = addGraphicsOverlay(mapView)

        // get the MapView location display
        locationDisplay = mapView.locationDisplay

        // permission state
        val permFineLoc = (ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED)
        val permCoarseLoc = (ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[1]) == PackageManager.PERMISSION_GRANTED)
        // check if permissions needed
        if(permFineLoc && permCoarseLoc){
            // have required permissions
            locationDisplay.startAsync()
            
            if( locationDisplay.isStarted ){
                val centerPnt = locationDisplay.location.position
                weatherAtLocation(centerPnt, mvOverlay)
            }
        }else{
            // request permissions at runtime
            val requestCode = 2
            ActivityCompat.requestPermissions(this@MainActivity, reqPermissions, requestCode)
            toast("Error in DataSourceChangedListener")
        }

        // respond to single taps on mapview
        mapView.onTouchListener = object: DefaultMapViewOnTouchListener(this, mapView) {
            override fun onSingleTapConfirmed(motionEvent: MotionEvent?): Boolean {
                // clear any graphics and callouts
                mvOverlay.graphics.clear()
                mapView.callout.dismiss()
                // get the point that was clicked and convert it to a point in map coordinates
                val screenPoint: android.graphics.Point = android.graphics.Point(motionEvent!!.x.toInt(), motionEvent.y.toInt())
                // create a map point from screen point
                val mapPoint: Point = mapView.screenToLocation(screenPoint)
                // get the weather at tapped location
                weatherAtLocation(mapPoint, mvOverlay)

                return super.onSingleTapConfirmed(motionEvent)
            }

        }

        // turn on/off location display
        locationFab.setOnClickListener { _ ->
            if(locationDisplay.isStarted){
                locationDisplay.stop()
            }else{
                // clear any graphics and callouts
                mvOverlay.graphics.clear()
                mapView.callout.dismiss()
                // start location display
                locationDisplay.startAsync()
                // zoom to location and display weather
                val centerPnt = locationDisplay.location.position
                weatherAtLocation(centerPnt, mvOverlay)
            }
        }

        // allow fab to reposition based on attribution bar layout
        val params = locationFab.layoutParams as CoordinatorLayout.LayoutParams
        mapView.addAttributionViewLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            val heightDelta = bottom - oldBottom
            params.bottomMargin += heightDelta
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        // assign menu items
        clearLayersItem = menu.getItem(0)
        precipLayerItem = menu.getItem(1)
        tempLayerItem = menu.getItem(2)
        placeSearchItem = menu.getItem(3)
        // set clear layers item checked by default
        clearLayersItem.isChecked = true

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {

        // place search
        R.id.place_search -> consume {
            // open auto complete intent
            openAutocompleteActivity()
        }

        // clear all layers
        R.id.layer_clear -> consume{
            map.operationalLayers.clear()
            clearLayersItem.isChecked = true
        }
        // add precipitation layer
        R.id.layer_precip -> consume{
            map.operationalLayers.clear()
            // add open weather precipitation layer
            val templateUri = "http://{subDomain}.tile.openweathermap.org/map/precipitation_new/{level}/{col}/{row}.png?appid=$APIKEY"
            val openPrecipLayer = WebTiledLayer(templateUri, subDomains)
            openPrecipLayer.loadAsync()
            openPrecipLayer.addDoneLoadingListener {
                if(openPrecipLayer.loadStatus == LoadStatus.LOADED){
                    info { "Open precip layer loaded" }
                    map.operationalLayers.add(openPrecipLayer)
                }
            }
            // zoom out to see layer
            if(mapView.mapScale < 4000000.0) mapView.setViewpointScaleAsync(4000000.0)
            precipLayerItem.isChecked = true
        }
        // add temperature layer
        R.id.layer_temp -> consume{
            map.operationalLayers.clear()
            // add open weather temperature layer
            val templateUri = "http://{subDomain}.tile.openweathermap.org/map/temp_new/{level}/{col}/{row}.png?appid=$APIKEY"
            val openTempLayer = WebTiledLayer(templateUri, subDomains)
            openTempLayer.loadAsync()
            openTempLayer.addDoneLoadingListener {
                if(openTempLayer.loadStatus == LoadStatus.LOADED){
                    info { "Open precip layer loaded" }
                    map.operationalLayers.add(openTempLayer)
                }
            }
            // zoom out to see layer
            if(mapView.mapScale < 4000000.0) mapView.setViewpointScaleAsync(4000000.0)
            tempLayerItem.isChecked = true
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
            locationDisplay.startAsync()
            val centerPnt = locationDisplay.location.position
            weatherAtLocation(centerPnt, mvOverlay)
        } else {
            toast("denied")
        }
    }

    /**
     * Notification on selected place
     */
    private fun openAutocompleteActivity() = try {
        // The autocomplete activity requires Google Play Services to be available. The intent
        // builder checks this and throws an exception if it is not the case.
        val intent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN).build(this)
        startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE)
    } catch (e:GooglePlayServicesRepairableException) {
        // Indicates that Google Play Services is either not installed or not up to date.
        GoogleApiAvailability.getInstance().getErrorDialog(this, e.connectionStatusCode, 0 /* requestCode */).show()
    } catch (e:GooglePlayServicesNotAvailableException) {
        // Indicates that Google Play Services is not available and the problem is not easily resolvable.
        val message = ("Google Play Services is not available: ${GoogleApiAvailability.getInstance().getErrorString(e.errorCode)}")
        info { "PLACES: " + message }
        toast(message)
    }

    /**
     * Called after the autocomplete activity has finished to return its result.
     */
    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check that the result was from the autocomplete widget.
        if (requestCode == REQUEST_CODE_AUTOCOMPLETE) when (resultCode) {
            RESULT_OK -> {
                mvOverlay.graphics.clear()
                mapView.callout.dismiss()
                // Get the user's selected place from the Intent.
                val place = PlaceAutocomplete.getPlace(this, data)
                // get lat/lon of searched place
                val latLng = place.latLng
                // create arcgis point
                val placePnt = Point(latLng.longitude, latLng.latitude, SpatialReferences.getWgs84())
                // get the weather
                weatherAtLocation(placePnt, mvOverlay)
            }
            PlaceAutocomplete.RESULT_ERROR -> {
                val status = PlaceAutocomplete.getStatus(this, data)
                error({ "PLACES: Error: Status = $status.toString()" })
            }
            RESULT_CANCELED -> {
                // Indicates that the activity closed before a selection was made.
            }
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
     * Convert to WGS84 for lat/lon format
     *
     * @param mapPoint Point to convert
     */
    private fun convertToWgs84(mapPoint: Point) : Point =
            GeometryEngine.project(mapPoint, SpatialReferences.getWgs84()) as Point

    /**
     * Get weather from location
     *
     * @param location Location as Point
     */
    private fun weatherAtLocation(location: Point, graphicOverlay: GraphicsOverlay){
        val wgs84Pnt = convertToWgs84(location)
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

        // create a textview for the callout
        val calloutContent = TextView(applicationContext)
        calloutContent.setTextColor(Color.BLACK)
        // create text from string resource
        val calloutText = getString(R.string.callout_text, cityName, temp, DEGREE, highTemp, DEGREE, lowTemp, DEGREE)
        calloutContent.text = calloutText
        // get callout, set content and geoelement graphic
        callout = mapView.callout
        callout.content = calloutContent
        callout.setGeoElement(locationGraphic, mapPoint)
        callout.show()
        // center on the location, zoom in when scaled out
        val mapScale = mapView.mapScale
        if(mapScale < 350000.0){
            mapView.setViewpointCenterAsync(mapPoint)
        }else{
            mapView.setViewpointCenterAsync(mapPoint, 1050000.0)
        }
    }
}
