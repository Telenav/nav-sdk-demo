/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.search

import android.location.Location
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType

/**
 * This data structure is used to show POI on map
 * @author zhai.xiang on 2021/3/16
 */
data class SearchResultModel(val name : String, val geoLocation: Location, val navLocation : Location, val distance : Double, val id : String){

    companion object{
        private const val NAME = "Search"

        fun wrapperFromEntity(entity : Entity) : SearchResultModel?{
            val name : String
            val geoLocation : Location
            val navLocation : Location
            when(entity.type){
                EntityType.ADDRESS -> {
                    name = entity.address.formattedAddress
                    geoLocation = Location(NAME).apply {
                        this.latitude = entity.address.geoCoordinates.latitude
                        this.longitude = entity.address.geoCoordinates.longitude
                    }
                    navLocation = Location(NAME).apply {
                        this.latitude = entity.address.navCoordinates.latitude
                        this.longitude = entity.address.navCoordinates.longitude
                    }
                }
                EntityType.PLACE ->{
                    name = entity.place.name
                    geoLocation = Location(NAME).apply {
                        this.latitude = entity.place.address.geoCoordinates.latitude
                        this.longitude = entity.place.address.geoCoordinates.longitude
                    }
                    navLocation = Location(NAME).apply {
                        this.latitude = entity.place.address.navCoordinates.latitude
                        this.longitude = entity.place.address.navCoordinates.longitude
                    }
                }
                else ->{
                    return null
                }
            }
            return SearchResultModel(name, geoLocation, navLocation, entity.distance, entity.id)
        }
    }
}
