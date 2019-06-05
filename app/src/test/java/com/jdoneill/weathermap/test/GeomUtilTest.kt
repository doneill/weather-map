package com.jdoneill.weathermap.test

import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReference
import com.esri.arcgisruntime.geometry.SpatialReferences

import com.jdoneill.weathermap.util.GeometryUtil

import org.junit.Test
import org.junit.Assert.assertEquals

class GeomUtilTest {

    @Test
    fun testConvertToWgs84() {
        val actualPoint = Point(-13618179.044288, 6040288.345644, SpatialReferences.getWebMercator())
        val convertedPoint = GeometryUtil.convertToWgs84(actualPoint)
        val expectedPoint = Point(-122.334184, 47.598323, SpatialReference.create(4326))
        assertEquals(expectedPoint.x, convertedPoint.x, 0.1)
    }

}