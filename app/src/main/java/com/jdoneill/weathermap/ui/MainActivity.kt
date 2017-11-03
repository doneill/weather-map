
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

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    // degree sign
    private val DEGREE: String = "\u00B0"
    private lateinit var callout: Callout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // show map
        val map = ArcGISMap(Basemap.Type.DARK_GRAY_CANVAS_VECTOR, 47.495052, -121.786863, 12)
        mapView.map = map
        // graphics overlay for tapped location marker
        val mvOverlay = addGraphicsOverlay(mapView)

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
                // convert to WGS84 for lat/lon format
                val wgs84: Point = GeometryEngine.project(mapPoint, SpatialReferences.getWgs84()) as Point

                // get weather from location tapped
                val network = WeatherClient()
                val call = network.getWeatherForCoord(wgs84.y.toFloat(), wgs84.x.toFloat())
                call.enqueue(object : Callback<Weather> {
                    override fun onResponse(call: Call<Weather>?, response: Response<Weather>?) {
                        val weather: Weather? = response?.body()
                        // present date if main not null
                        weather?.let { presentData(it, mapPoint, mvOverlay) }

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
                return super.onSingleTapConfirmed(motionEvent)
            }

        }

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
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
        // center on the tapped location
        mapView.setViewpointCenterAsync(mapPoint)
    }
}
