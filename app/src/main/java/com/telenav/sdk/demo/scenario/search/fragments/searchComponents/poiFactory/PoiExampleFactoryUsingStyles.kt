package com.telenav.sdk.demo.scenario.search.fragments.searchComponents.poiFactory

import android.location.Location
import com.telenav.map.api.Annotation
import com.telenav.map.api.POIAnnotationParams
import com.telenav.map.api.factories.AnnotationFactory
import com.telenav.map.api.search.MapMode
import com.telenav.map.api.search.PoiAnnotationFactory
import com.telenav.map.api.search.PoiSearchEntity
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.searchEngine.SearchEngineExample
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.searchEngine.SearchEngineExample.Companion.DISCOUNT_STORE
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.searchEngine.SearchEngineExample.Companion.E10
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.searchEngine.SearchEngineExample.Companion.PARKING_LOT
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.searchEngine.SearchEngineExample.Companion.SPECIALTY_FOOD

/**
 * The example displays how you can implement the interface [PoiAnnotationFactory]
 * for using Atlas resources.
 *
 * @author Mykola Ivantsov - (p)
 */
class PoiExampleFactoryUsingStyles(
    private val annotationFactory: AnnotationFactory
) : PoiAnnotationFactory {

    override fun create(entity: PoiSearchEntity, mapMode: MapMode): Annotation {
        return createPOIAnnotation(
            annotationFactory,
            entity.location,
            getAnnotationStyle(entity.data.getString(SearchEngineExample.BUNDLE_CATEGORY)),
            POI_DEFAULT_TEXT
        )
    }

    private fun createPOIAnnotation(
        annotationsController: AnnotationFactory,
        location: Location,
        styleKey: String,
        text: String
    ): Annotation {
        return annotationsController.create(POIAnnotationParams(styleKey, location, text))
    }

    //All general categories have subcategories that you can be different for different projects
    private fun getAnnotationStyle(category: String?): String {
        val result = when (category) {
            //region Food categories
            SPECIALTY_FOOD -> {
                POI_FOOD
            }
            //endregion
            //region Parking categories
            PARKING_LOT -> {
                POI_PARKING
            }
            //endregion
            //region Shopping categories
            DISCOUNT_STORE -> {
                POI_SHOPPING
            }
            //endregion
            //region Automotive
            E10 -> {
                POI_CHARGING_STATION
            }
            //endregion
            else -> {
                POI_DEFAULT
            }
        }
        printDebugLog("getAnnotationStyle(category = $category) = $result")
        return result
    }

    private fun printDebugLog(msg: String) {
        TaLog.d("POI_SEARCH", "msg: $msg | Thread.name: ${Thread.currentThread().name}")
    }

    companion object {
        const val POI_DEFAULT_TEXT = ""

        //region these elements can be different and has different names in your style.tss file.
        const val POI_FOOD = "poi_annotations.food"
        const val POI_PARKING = "poi_annotations.parking"
        const val POI_SHOPPING = "poi_annotations.shopping"
        const val POI_CHARGING_STATION = "poi_annotations.chargingstation"
        const val POI_DEFAULT = "poi_annotations.default"
        //endregion
    }
}
