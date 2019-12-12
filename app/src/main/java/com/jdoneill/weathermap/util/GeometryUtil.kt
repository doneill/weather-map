package com.jdoneill.weathermap.util

import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences

class GeometryUtil {
    companion object {
        /**
         * Convert to WGS84 for lat/lon format
         *
         * @param mapPoint Point to convert
         */
        @JvmStatic
        fun convertToWgs84(mapPoint: Point): Point =
                GeometryEngine.project(mapPoint, SpatialReferences.getWgs84()) as Point
    }
}
