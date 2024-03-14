/*
 * Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.examples.case

import com.telenav.map.api.MapView
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.api.touch.listeners.TouchListener

/**
 * @author Ivantsov Mykola
 * @since 14 Oct 2022
 */
class SetMapViewOnTouchListenerUseCase {
    operator fun invoke(mapView: MapView, isEnabled: Boolean, listener: TouchListener) {
        mapView.setOnTouchListener(if (isEnabled) listener else TouchListener { _: TouchType, _: TouchPosition -> /*do nothing*/ })
    }
}