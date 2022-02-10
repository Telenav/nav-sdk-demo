package com.telenav.sdk.demo.scenario.mapview

import android.location.Location
import androidx.lifecycle.ViewModel
import com.telenav.map.api.Annotation
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.AnnotationsController
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.CameraController
import com.telenav.map.api.controllers.RoutesController
import com.telenav.map.api.factories.AnnotationFactory
import com.telenav.map.engine.GLMapRouteAnnotation
import com.telenav.map.internal.EVBubbleType
import com.telenav.map.internal.SmartBubbleType
import com.telenav.map.internal.Utils
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*

class HighlightedETEBubbleViewModel : ViewModel() {

    private val routeAnnotations = ArrayList<Annotation>()
    private val routeAnnotationsMap = HashMap<String, Annotation>()
    private var routeIds = ArrayList<String>()
    private var routeList: ArrayList<Utils.Route?> = ArrayList()

    val styleIds =
        arrayOf(GLMapRouteAnnotation.STYLE_SMART_BUBBLE, GLMapRouteAnnotation.STYLE_EV_BUBBLE)
    val evBubbleType = arrayOf(
        "simple",
        "battery",
        "battery_depleted",
        "expandable_time",
        "expandable_additional_time",
        "expanded"
    )
    val smartBubbleType = arrayOf(
        "default",
        "unfocused",
        "selected",
        "two_texts",
        "ftue",
        "low_battery"
    )
    var styleIdSelected: String = styleIds.first()
    var evBubbleTypeSelected: Float = EVBubbleType.SIMPLE
    var smartBubbleTypeSelected: Float = SmartBubbleType.DEFAULT

    val startLocation = Location("MOCK").apply {
        this.latitude = 37.353396
        this.longitude = -121.99414
        this.bearing = 45.0f
    }
    val stopLocation = Location("MOCK").apply {
        this.latitude = 37.351183
        this.longitude = -121.970336
    }

    companion object {
        private const val ROUTE_COUNT = 3
        private const val DEFAULT_VALUE_NOT_SELECTED = 0F
        private const val TAG = "HighlightedETEBubbleViewModel"
    }

    fun addRouteAnnotation(annotation: Annotation): Boolean {
        return routeAnnotations.add(annotation)
    }

    private fun removeAnnotationElementFromList(annotation: Annotation) {
        routeAnnotations.remove(annotation)
    }

    private fun putRouteAnnotation(key: String, annotation: Annotation): Annotation? {
        return routeAnnotationsMap.put(key, annotation)
    }

    private fun removeRouteAnnotationFromMap(key: String): Annotation? {
        return routeAnnotationsMap.remove(key)
    }

    private fun getRouteAnnotationList(): List<Annotation> {
        return routeAnnotations
    }

    fun getRouteAnnotationMap(): Map<String, Annotation> {
        return routeAnnotationsMap
    }

    private fun addRoteIdList(routes: List<String>) {
        routeIds = routes as ArrayList<String>
    }

    fun clearAll() {
        routeAnnotations.clear()
        routeAnnotationsMap.clear()
        routeIds.clear()
        routeList.clear()
    }

    fun getRouteIdList(): List<String> {
        return routeIds
    }

    fun addRouteList(routes: List<Utils.Route?>) {
        routeList = routes as ArrayList
    }

    fun removeAnnotations(
        annotationsController: AnnotationsController,
        annotationList: List<Annotation>
    ) {
        annotationsController.remove(annotationList)
    }

    fun createRouteAnnotation(routeId: String, factory: AnnotationFactory): Annotation {
        return factory.createRouteAnnotation(routeId)
    }

    fun getAnnotationFactory(annotationsController: AnnotationsController): AnnotationFactory {
        return annotationsController.factory()
    }

    fun showRouteAnnotation(routeId: String, annotationsController: AnnotationsController) {
        val factory = getAnnotationFactory(annotationsController)
        val annotation = createRouteAnnotation(
            routeId,
            style = styleIdSelected,
            bubbleType = getSelectedBubbleType(),
            factory
        )
        val text = createAnnotationTextInfo("Selected Route")
        displayAnnotationTextInfo(text, annotation)
        removeAnnotationElementFromList(annotation)
        addRouteAnnotation(annotation)
        removeRouteAnnotationFromMap(routeId)
        putRouteAnnotation(routeId, annotation)
        addAnnotationList(annotationsController, getRouteAnnotationList())
    }

    private fun addAnnotationList(
        annotationsController: AnnotationsController,
        annotations: List<Annotation>
    ) {
        printDebugLog("before annotationsController.add $annotations")
        annotationsController.add(annotations)
    }
    fun createRouteAnnotation(
        routeId: String,
        style: String,
        bubbleType: Float,
        factory: AnnotationFactory
    ): Annotation {
        return factory.createRouteAnnotation(routeId, style, bubbleType)
    }

    fun getSelectedBubbleType(): Float {
        return when (styleIdSelected) {
            styleIds.first() -> {
                smartBubbleTypeSelected
            }
            else -> {
                evBubbleTypeSelected
            }
        }
    }

    private fun createAnnotationTextInfo(text: String): Annotation.TextDisplayInfo {
        return Annotation.TextDisplayInfo.Centered(text)
    }

    private fun displayAnnotationTextInfo(
        textDisplayInfo: Annotation.TextDisplayInfo,
        annotation: Annotation
    ) {
        annotation.displayText = textDisplayInfo
    }

    fun requestDirection(
        routesController: RoutesController,
        cameraController: CameraController,
        annotationsController: AnnotationsController,
        begin: Location,
        end: Location,
        result: (Boolean) -> Unit
    ) {
        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(ROUTE_COUNT)
            .build()
        val task = DirectionClient.Factory.hybridClient()
            .createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            try {
                routesController.clear()
                clearAll()
                val requestOk = response.response.status == DirectionErrorCode.OK
                val requestIsNotEmpty = response.response.result.isNotEmpty()

                if (requestOk && requestIsNotEmpty) {
                    val routeList: List<Route?> = response.response.result!!

                    val routeIdList = addRoutes(routeList, routesController)

                    val routeId = routeIdList.first()
                    highlight(routeId, routesController)

                    routesController.updateRouteProgress(routeId)

                    val region = region(routeIdList, routesController)
                    val marginsPercentages = createMarginsPercentages(0.20, 0.20)
                    showRegion(region, marginsPercentages, cameraController)
                    routeIdList.forEach { id ->
                        showRouteAnnotation(id, annotationsController)
                    }
                    addRoteIdList(routeIdList)
                    result(true)

                } else {
                    result(false)
                }
                task.dispose()
            } catch (ex: Exception) {
                result(false)
                printErrorLog("crash", ex)
            }
        }
    }

    private fun printErrorLog(msg: String, ex: Throwable) {
        TaLog.e(TAG, msg, ex)
    }
    private fun printDebugLog(msg: String) {
        TaLog.d(TAG, "HighlightedETEBubbleViewModel | $msg")
    }

    private fun showRegion(
        region: Camera.Region,
        percentageMargins: Margins.Percentages,
        cameraController: CameraController
    ) {
        cameraController.showRegion(region, percentageMargins)
    }

    private fun createMarginsPercentages(
        lrPercentage: Double,
        tbPercentage: Double
    ): Margins.Percentages {
        return Margins.Percentages(lrPercentage, tbPercentage)
    }

    private fun highlight(routeId: String, routesController: RoutesController) {
        routesController.highlight(routeId)
    }

    private fun region(
        routeIDs: List<String>,
        routesController: RoutesController
    ): Camera.Region {
        return routesController.region(routeIDs)
    }

    private fun addRoutes(
        routes: List<Route?>,
        routesController: RoutesController
    ): List<String> {
        return routesController.add(routes)
    }

}