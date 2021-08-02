/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.util

import android.content.Context
import android.location.Location
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * @author zhai.xiang on 2021/4/2
 */
object KmlParser {
    private const val TAG: String = "Kml"

    private const val ELEMENT_COORDINATES = "coordinates"
    private const val ELEMENT_LINE_STRING = "LineString"
    private const val REGEX_RAW_DATA = "\n"

    @JvmStatic
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(context: Context, rawFileId: Int) : List<Location>{
        val inputStream = context.resources.openRawResource(rawFileId)
        return readCoordinatesFromStream(inputStream).map { coordinate->
            Location(TAG).apply {
                this.longitude = coordinate.lon
                this.latitude = coordinate.lat
                this.bearing = coordinate.bearing
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readCoordinates(parser: XmlPullParser): List<KmlCoordinate> {
        val coordinates: MutableList<KmlCoordinate> = ArrayList<KmlCoordinate>()
        // split the raw data into chunks, each chunk representing raw data of 2 points: lat and lon
        parser.require(XmlPullParser.START_TAG, null, ELEMENT_COORDINATES)
        val rawCoordinates = readText(parser).split(REGEX_RAW_DATA)
        parser.require(XmlPullParser.END_TAG, null, ELEMENT_COORDINATES)
        //parse each chunk into a coordinate
        for (s in rawCoordinates) {
            KmlCoordinate.parse(s)?.let {
                coordinates.add(it)
            }
        }
        return coordinates
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readCoordinatesFromStream(inputStream: InputStream): List<KmlCoordinate> {
        val parser  = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        parser.nextTag()
        return readTag(parser)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readTag(parser : XmlPullParser) : List<KmlCoordinate>{
        val coordinates: MutableList<KmlCoordinate> = ArrayList<KmlCoordinate>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when(parser.name) {
                "kml" -> coordinates.addAll(readTag(parser))
                "Document" -> coordinates.addAll(readTag(parser))
                "Placemark" -> coordinates.addAll(readTag(parser))
                "LineString" -> coordinates.addAll(readTag(parser))
                "coordinates" -> coordinates.addAll(readCoordinates(parser))
                else -> skip(parser)
            }
        }
        return coordinates
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }
}