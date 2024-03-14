package com.telenav.sdk.examples.scenario.mapview

import android.location.Location
import androidx.lifecycle.ViewModel
import com.telenav.map.api.Annotation
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.AnnotationsController
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.CameraController
import com.telenav.map.api.controllers.RoutesController
import com.telenav.map.api.factories.AnnotationFactory
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*

class HighlightedETEBubbleViewModel : ViewModel() {

    private val routeAnnotationsMap = HashMap<String, Annotation>()
    private var routeIds = ArrayList<String>()
    var isUseOldApi: Boolean = false

    val styleIds =
        arrayOf("smart-bubble", "ev-bubble")

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
    var evBubbleTypeSelected: Float = EV_BUBBLE_TYPE_SIMPLE
    var smartBubbleTypeSelected: Float = SMART_BUBBLE_TYPE_DEFAULT

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
        private const val EV_BUBBLE_TYPE_SIMPLE = 0F
        private const val SMART_BUBBLE_TYPE_DEFAULT = 0F
        private const val TAG = "HighlightedETEBubbleViewModel"
        const val BUBBLE_STYLE_KEY = "smart-bubble-type"
        const val MAIN_TEXT_STYLE_KEY = "main-text"
    }

    private fun putRouteAnnotation(key: String, annotation: Annotation): Annotation? {
        return routeAnnotationsMap.put(key, annotation)
    }

    private fun getRouteAnnotations(key: String): Annotation? {
        return routeAnnotationsMap[key]
    }

    private fun removeRouteAnnotationFromMap(key: String): Annotation? {
        return routeAnnotationsMap.remove(key)
    }

    fun getRouteAnnotationMap(): Map<String, Annotation> {
        return routeAnnotationsMap
    }

    private fun addRoteIdList(routes: List<String>) {
        routes.forEach {
            routeIds.add(it)
        }
    }

    private fun removeRoteId(routeId: String) {
        routeIds.remove(routeId)
    }

    fun clearAll(annotationsController: AnnotationsController, routesController: RoutesController) {

        val routeIdRemoveList = ArrayList(routeIds)

        if (routeIdRemoveList.isNotEmpty()) {
            val annotations = mutableListOf<Annotation>()
            routeIdRemoveList.forEach { routeIdToRemove ->
                getRouteAnnotations(routeIdToRemove)?.let {
                    annotations.add(it)
                }
                removeRoteId(routeIdToRemove)
                removeRouteAnnotationFromMap(routeIdToRemove)
                routesController.remove(routeIdToRemove)
            }
            annotationsController.remove(annotations)
        }
        routeAnnotationsMap.clear()
        routeIds.clear()
    }

    private fun getAnnotationFactory(annotationsController: AnnotationsController): AnnotationFactory {
        return annotationsController.factory()
    }

    private fun showRouteAnnotation(routeId: String, annotationsController: AnnotationsController) {
        showRouteAnnotationNewApi(routeId, annotationsController)
    }

    private fun showRouteAnnotationNewApi(
        routeId: String,
        annotationsController: AnnotationsController
    ) {
        //create route annotation, you can create a lot of route annotations
        //but you have to store all your annotations, after annotations will be added to the engine
        //you will use it to update annotation view
        val factory = getAnnotationFactory(annotationsController)
        val annotation = createRouteAnnotationNewApi(
            routeId,
            style = styleIdSelected,
            factory
        ).apply {
            displayText = createAnnotationTextInfo("Selected Route")
        }
        annotation.updateFloatValue(BUBBLE_STYLE_KEY, getSelectedBubbleType())
        annotation.updateStringValue(MAIN_TEXT_STYLE_KEY, "Test Text")
        //all annotations that were created by you have to add to the engine
        annotationsController.add(listOf(annotation))

        putRouteAnnotation(routeId, annotation)
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
                clearAll(annotationsController, routesController)
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
                        //this is one of the most important steps
                        //here we will create a smart-bubble annotation and associate the route with it
                        printDebugLog("route id: $id")
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

    private fun createRouteAnnotationNewApi(
        routeId: String,
        style: String,
        factory: AnnotationFactory
    ): Annotation {
        return factory.createRouteAnnotation(routeId, style)
    }

    private fun printErrorLog(msg: String, ex: Throwable) {
        TaLog.e(TAG, msg, ex)
    }

    private fun printDebugLog(msg: String) {
        TaLog.d(TAG, "HighlightedETEBubbleFragment | $msg")
    }

    private fun showRegion(
        region: Camera.Region?,
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
    ): Camera.Region? {
        return routesController.region(routeIDs)
    }

    private fun addRoutes(
        routes: List<Route?>,
        routesController: RoutesController
    ): List<String> {
        return routesController.add(routes)
    }

}