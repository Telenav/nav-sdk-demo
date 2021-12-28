/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.mapview

import android.location.Location
import androidx.lifecycle.ViewModel
import com.telenav.map.api.Annotation
import com.telenav.map.api.POIAnnotationParams
import com.telenav.map.api.controllers.AnnotationsController

class POIAnnotationViewModel : ViewModel() {

    /**
     * Current supported POI categories in the TSS layout.
     * Example only, values can change in any time
     */
    val poiCategories = listOf(
        "poi_annotations.default",

        "poi_annotations.atm",
        "poi_annotations.fuel",
        "poi_annotations.food",
        "poi_annotations.coffee",
        "poi_annotations.food",
        "poi_annotations.campingsites",
        "poi_annotations.shopping",

        "poi_annotations.attraction",
        "poi_annotations.chargingstation",
        "poi_annotations.favorites",
        "poi_annotations.health",
        "poi_annotations.home",
        "poi_annotations.hotel",
        "poi_annotations.parking",
        "poi_annotations.rest",
        "poi_annotations.work"
    )

    val baseLocation = Location("").apply {
        this.latitude = 37.353396
        this.longitude = -121.99414
        this.bearing = 45.0f
    }

    val defaultAnnotationLocation = Location("").apply {
        this.latitude = 37.351183
        this.longitude = -121.970336
    }

    fun createPOIAnnotation(
        annotationsController: AnnotationsController,
        styleKey: String,
        text: String
    ): Annotation {
        return createPOIAnnotation(
            annotationsController = annotationsController,
            location = defaultAnnotationLocation,
            styleKey = styleKey,
            text = text
        )
    }

    fun createPOIAnnotation(
        annotationsController: AnnotationsController,
        location: Location,
        styleKey: String,
        text: String
    ): Annotation {
        val factory = annotationsController.factory()
        return factory.create(POIAnnotationParams(styleKey, location, text))
    }


}
