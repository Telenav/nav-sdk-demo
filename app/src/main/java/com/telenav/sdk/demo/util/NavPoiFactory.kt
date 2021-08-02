/*
 * Copyright © 2018 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.util

import android.content.Context
import com.telenav.map.api.Annotation
import com.telenav.map.api.factories.AnnotationFactory
import com.telenav.map.api.search.MapMode
import com.telenav.map.api.search.PoiAnnotationFactory
import com.telenav.map.api.search.PoiSearchEntity
import com.telenav.sdk.examples.R

/**
 * @author zhai.xiang on 2021/7/3
 */
class NavPoiFactory(val context: Context, val annotationFactory: AnnotationFactory) : PoiAnnotationFactory {
    private val gasBitmap = BitmapUtils.getBitmapFromVectorDrawable(context, R.drawable.baseline_local_gas_station_24)!!
    private val foodDay = BitmapUtils.getBitmapFromVectorDrawable(context, R.drawable.baseline_restaurant_24_day)!!
    private val foodNight = BitmapUtils.getBitmapFromVectorDrawable(context, R.drawable.baseline_restaurant_24_night)!!

    override fun create(entity: PoiSearchEntity, mode : MapMode): Annotation {
        if (mode == MapMode.DAY){
            return annotationFactory.create(context,getBitmap(entity.data.getString("category"),mode), entity.location).apply {
                this.style = Annotation.Style.ScreenAnnotationFlagNoCulling
            }
        }
        entity.icon = getBitmap(entity.data.getString("category"),mode)
        return annotationFactory.create(context,getBitmap(entity.data.getString("category"),mode), entity.location).apply {
            this.style = Annotation.Style.ScreenAnnotationFlagNoCulling
        }
    }

    private fun getBitmap(category : String?, mode: MapMode): Annotation.UserGraphic{
        if (category == NavSearchEngine.SEARCH_ELECTRIC_CHARGE_STATION){
            return Annotation.UserGraphic(gasBitmap)
        }

        return if (mode == MapMode.DAY){
            Annotation.UserGraphic(foodDay)
        }else{
            Annotation.UserGraphic(foodNight)
        }
    }
}
