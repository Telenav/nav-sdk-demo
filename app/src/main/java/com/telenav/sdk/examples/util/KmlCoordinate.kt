/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.util

/**
 * @author zhai.xiang on 2021/4/6
 */
class KmlCoordinate(val lat: Double, val lon: Double, val bearing : Float = 0.0f) {

    companion object{
        private const val REGEX_COORDINATES_SEPARATOR = ","

        /**
         * Instantiate a new coordinate from raw data
         *
         * @param rawData rawdata containing coordinates
         */
        fun parse(rawData: String) : KmlCoordinate?{
            val values = rawData.split(REGEX_COORDINATES_SEPARATOR)
            return try {
                val lat = values[0].toDouble()
                val lon = values[1].toDouble()
                val bearing = if (values.size >= 3) values[2].toFloat() else 0f
                KmlCoordinate(lat, lon, bearing)
            }catch (e: Throwable){
                e.printStackTrace()
                null
            }
        }
    }




}