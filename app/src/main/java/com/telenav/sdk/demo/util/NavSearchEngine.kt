/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.util

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import com.telenav.map.api.search.SearchEngine
import com.telenav.map.api.search.PoiSearchEntity
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.*
import com.telenav.sdk.entity.model.search.MultiboxQuery
import com.telenav.sdk.entity.model.search.PolygonGeoFilter
import com.telenav.sdk.entity.model.search.SearchFilters
import com.telenav.sdk.examples.R

/**
 * @author zhai.xiang on 2021/6/1
 */
class NavSearchEngine(val context: Context) : SearchEngine {
    var vehicleLocation: Location? = null
    val bitmap : Bitmap =  BitmapUtils.getBitmapFromVectorDrawable(context, R.drawable.baseline_local_gas_station_24)!!

    companion object {
        const val TAG = "NavSearchEngine"
    }

    override fun search(displayContent: List<String>, searchBox: List<GeoPoint>, resultSize: Int, language: String) : List<PoiSearchEntity>?{
        vehicleLocation?.let {
            val polygon = Polygon
                    .builder()
                    .setPoints(searchBox)
                    .build()
            val geoFilter = PolygonGeoFilter
                    .builder(polygon)
                    .build()
            val geoSearchFilters = SearchFilters
                    .builder()
                    .setGeoFilter(geoFilter)
                    .build()
            val entityClient = EntityService.getClient()
            val query = MultiboxQuery.Builder()
            displayContent.forEach {
                query.addTag(Tag.WHAT, it)
            }
            val result = entityClient.searchRequest()
                    .setQuery(query.build())
                    .setFilters(geoSearchFilters)
                    .setLimit(resultSize)
                    .setLocation(it.latitude, it.longitude)
                    .execute()
            return result.results?.mapNotNull { entity ->
                wrapperToPoiSearchEntity(entity)
            }
        }
        return emptyList()
    }

    fun wrapperToPoiSearchEntity(entity: Entity): PoiSearchEntity? {
        val name: String
        val geoLocation: Location
        val navLocation: Location
        when (entity.type) {
            EntityType.ADDRESS -> {
                name = entity.address.formattedAddress
                geoLocation = Location(TAG).apply {
                    this.latitude = entity.address.geoCoordinates.latitude
                    this.longitude = entity.address.geoCoordinates.longitude
                }
                navLocation = Location(TAG).apply {
                    this.latitude = entity.address.navCoordinates.latitude
                    this.longitude = entity.address.navCoordinates.longitude
                }
            }
            EntityType.PLACE -> {
                name = entity.place.name
                geoLocation = Location(TAG).apply {
                    this.latitude = entity.place.address.geoCoordinates.latitude
                    this.longitude = entity.place.address.geoCoordinates.longitude
                }
                navLocation = Location(TAG).apply {
                    this.latitude = entity.place.address.navCoordinates.latitude
                    this.longitude = entity.place.address.navCoordinates.longitude
                }
            }
            else -> {
                return null
            }
        }
        val bundle = Bundle().apply {
            this.putString("name", name)
            this.putParcelable("geoLocation", geoLocation)
            this.putParcelable("navLocation", navLocation)
            this.putDouble("distance", entity.distance)
        }
        return PoiSearchEntity(geoLocation, bitmap, bundle)
    }
}