package com.telenav.sdk.demo.search

import android.graphics.PointF
import android.graphics.Rect
import android.view.View
import com.telenav.map.api.models.RegionForRoutesInfo
import com.telenav.map.views.TnMapView
import com.telenav.sdk.entity.model.base.GeoPoint

/**
 * Fits route overview into the map area not covered by the search overlay (left side).
 *
 * TASDK [com.telenav.map.api.controllers.CameraController.showRegionForRoutes] expects a
 * screen [Rect] describing where the route should be fully visible (same convention as scoutnav).
 */
object MapRouteCameraHelper {

    data class MapGeoBounds(
        val bottomLeft: GeoPoint,
        val topRight: GeoPoint,
    )

    fun computeVisibleRect(
        mapView: View,
        searchOverlay: View,
        navButton: View,
        paddingPx: Int,
        rectSearchButton: View? = null,
    ): Rect {
        val overlayRect = Rect()
        searchOverlay.getHitRect(overlayRect)

        val left = (overlayRect.right + paddingPx).coerceIn(0, mapView.width)
        val top = paddingPx
        val right = (mapView.width - paddingPx).coerceAtLeast(left + 1)
        val bottom = when {
            rectSearchButton?.visibility == View.VISIBLE -> {
                val buttonRect = Rect()
                rectSearchButton.getHitRect(buttonRect)
                (buttonRect.top - paddingPx).coerceAtLeast(top + 1)
            }
            navButton.visibility == View.VISIBLE -> {
                val navRect = Rect()
                navButton.getHitRect(navRect)
                (navRect.top - paddingPx).coerceAtLeast(top + 1)
            }
            else -> (mapView.height - paddingPx).coerceAtLeast(top + 1)
        }
        return Rect(left, top, right, bottom)
    }

    /**
     * Converts the map area not covered by the search overlay into geographic bounds
     * (bottom-left / top-right corners), same convention as scoutnav search-here.
     */
    fun computeVisibleGeoBounds(
        mapView: TnMapView,
        searchOverlay: View,
        navButton: View,
        rectSearchButton: View?,
        paddingPx: Int,
    ): MapGeoBounds? {
        val visibleRect = computeVisibleRect(
            mapView,
            searchOverlay,
            navButton,
            paddingPx,
            rectSearchButton,
        )
        if (visibleRect.width() <= 0 || visibleRect.height() <= 0) return null
        val camera = mapView.getCameraController() ?: return null
        val bottomLeft = camera.viewportToWorld(
            PointF(visibleRect.left.toFloat(), visibleRect.bottom.toFloat())
        ) ?: return null
        val topRight = camera.viewportToWorld(
            PointF(visibleRect.right.toFloat(), visibleRect.top.toFloat())
        ) ?: return null
        return MapGeoBounds(
            bottomLeft = GeoPoint(bottomLeft.latitude, bottomLeft.longitude),
            topRight = GeoPoint(topRight.latitude, topRight.longitude),
        )
    }

    fun showRoutesInVisibleRegion(
        mapView: TnMapView,
        routeIds: List<String>,
        visibleRect: Rect
    ): Boolean {
        if (routeIds.isEmpty() || visibleRect.width() <= 0 || visibleRect.height() <= 0) {
            return false
        }
        val camera = mapView.getCameraController() ?: return false
        val regionRoutesInfo = RegionForRoutesInfo(
            routeIds,
            visibleRect,
            gridAligned = true,
            showFullRouteOverview = true,
            includeCVP = true
        )
        if (camera.showRegionForRoutes(regionRoutesInfo)) {
            return true
        }
        val region = mapView.getRoutesController()?.region(routeIds) ?: return false
        camera.showRegion(region, visibleRect)
        return true
    }
}
