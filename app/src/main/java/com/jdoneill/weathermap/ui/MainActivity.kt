
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
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.*
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.jdoneill.weathermap.R
import com.jdoneill.weathermap.data.Weather
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    // degree sign
    private val DEGREE: String = "\u00B0"
    private lateinit var callout: Callout
    private lateinit var locationDisplay: LocationDisplay

    private var reqPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // show map
        val map = ArcGISMap(Basemap.createDarkGrayCanvasVector())
        mapView.map = map
        // graphics overlay for tapped location marker
        val mvOverlay = addGraphicsOverlay(mapView)

        // get the MapView location display
        locationDisplay = mapView.locationDisplay

        val permFineLoc = (ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[0]) === PackageManager.PERMISSION_GRANTED)
        val permCoarseLoc = (ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[1]) === PackageManager.PERMISSION_GRANTED)

        if(permFineLoc && permCoarseLoc){
            locationDisplay.startAsync()
            val centerPnt = locationDisplay.location.position
            weatherAtLocation(centerPnt, mvOverlay)
        }else{
            val requestCode = 2
            ActivityCompat.requestPermissions(this@MainActivity, reqPermissions, requestCode)
            toast("Error in DataSourceChangedListner")
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

        fab.setOnClickListener { view ->

            if(locationDisplay.isStarted){
                locationDisplay.stop()
            }else{
                // clear any graphics and callouts
                mvOverlay.graphics.clear()
                mapView.callout.dismiss()
                locationDisplay.startAsync()
                val centerPnt = locationDisplay.location.position
                weatherAtLocation(centerPnt, mvOverlay)
            }

        }

        val params = fab.layoutParams as CoordinatorLayout.LayoutParams
        mapView.addAttributionViewLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val heightDelta = bottom - oldBottom
            params.bottomMargin += heightDelta
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }

    override fun onRequestPermissionsResult(requestCode:Int, @NonNull permissions:Array<String>, @NonNull grantResults:IntArray) =
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationDisplay.startAsync()
            } else {
                toast("denied")
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
        val locationMarker = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CROSS, Color.WHITE, 15.0f)
        val locationGraphic = Graphic(mapPoint, locationMarker)
        dataOverlay.graphics.add(locationGraphic)

        // create a textview for the callout
        val calloutContent = TextView(applicationContext)
        calloutContent.setTextColor(Color.BLACK)
        // format coordinates to 4 decimal places
        calloutContent.text = "Name: $cityName | Temp: $temp$DEGREE | High: $highTemp$DEGREE | Low: $lowTemp$DEGREE"
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
            mapView.setViewpointCenterAsync(mapPoint, 350000.0)
        }


    }
}
