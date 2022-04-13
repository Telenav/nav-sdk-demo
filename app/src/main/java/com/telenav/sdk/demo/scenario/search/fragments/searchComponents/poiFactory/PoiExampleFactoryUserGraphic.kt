package com.telenav.sdk.demo.scenario.search.fragments.searchComponents.poiFactory

import android.content.Context
import com.telenav.map.api.Annotation
import com.telenav.map.api.factories.AnnotationFactory
import com.telenav.map.api.search.MapMode
import com.telenav.map.api.search.PoiAnnotationFactory
import com.telenav.map.api.search.PoiSearchEntity
import com.telenav.sdk.demo.util.BitmapUtils
import com.telenav.sdk.examples.R
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.searchEngine.SearchEngineExample
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.searchEngine.SearchEngineExample.Companion.SEARCH_ELECTRIC_CHARGE_STATION
import com.telenav.sdk.demo.scenario.search.fragments.searchComponents.searchEngine.SearchEngineExample.Companion.SEARCH_SAVED_PLACES

/**
 * That class can be used to show an example implementation of PoiAnnotationFactory that have
 * to use to inject in SearchController using method injectPoiAnnotationFactory(..).
 *
 * This implementation shows how you can use your custom graphics with the Bitmap class.
 *
 * Try do not to use similar implementations because it can take really much memory and
 * can be the root cause OutOfMemoryException. You should prefer the implementation
 * in that you delegate create Annotation POI to the engine.
 * You can take a look at the next implementation that can be preferred
 * for including in your project [PoiExampleFactoryUsingStyles]
 *
 * @author Mykola Ivantsov - (p)
 */
class PoiExampleFactoryUserGraphic(
    private val context: Context,
    private val annotationFactory: AnnotationFactory
) : PoiAnnotationFactory {

    private val evChargeDay = createUserGraphic(R.drawable.ev_charge_day)
    private val evChargeNight = createUserGraphic(R.drawable.ev_charge_night)
    private val restaurantDay = createUserGraphic(R.drawable.restaurant_day)
    private val restaurantNight = createUserGraphic(R.drawable.restaurant_night)
    private val normal = createUserGraphic(R.drawable.ic_test_icon_day)

    override fun create(entity: PoiSearchEntity, mapMode: MapMode): Annotation {
        return annotationFactory.create(
            context.applicationContext,
            getAnnotationUserGraphic(
                entity.data.getString(SearchEngineExample.BUNDLE_CATEGORY),
                mapMode
            ),
            entity.location
        ).apply {
            this.style = Annotation.Style.ScreenAnnotationFlagNoCulling
        }
    }

    private fun getAnnotationUserGraphic(category: String?, mode: MapMode): Annotation.UserGraphic {
        return when {
            category == SEARCH_ELECTRIC_CHARGE_STATION && mode == MapMode.DAY -> {
                evChargeDay
            }
            category == SEARCH_ELECTRIC_CHARGE_STATION && mode == MapMode.NIGHT -> {
                evChargeNight
            }
            category == SEARCH_SAVED_PLACES -> {
                normal
            }
            mode == MapMode.DAY -> {
                restaurantDay
            }
            mode == MapMode.NIGHT -> {
                restaurantNight
            }
            else -> {
                normal
            }
        }
    }

    private fun createUserGraphic(drawableId: Int): Annotation.UserGraphic {
        return Annotation.UserGraphic(
            BitmapUtils.getBitmapFromVectorDrawable(
                context.applicationContext,
                drawableId
            )!!
        )
    }
}
