package com.telenav.sdk.demo.scenario.ota

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.controllers.ShapesController
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.geo.Attributes
import com.telenav.map.geo.Shape
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.core.Callback
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datacollector.model.SendEventResponse
import com.telenav.sdk.datacollector.model.event.SetHomeEvent
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.SDKImplement
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import com.telenav.sdk.ota.api.OtaService
import com.telenav.sdk.ota.model.AreaStatus
import com.telenav.sdk.ota.model.ResetStatus
import kotlinx.android.synthetic.main.fragment_ota_home_area.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.layout_content_map_with_log.*
import kotlinx.android.synthetic.main.layout_operation_homearea.*
import java.util.*


/**
 * This fragment shows how to use home area and streaming info.
 * @author zhai.xiang on 2021/1/25
 */
class OtaHomeAreaFragment : Fragment() {
    private var homeAreaLocation: Location = locationA
    private var homeAreaRectangleId: ShapesController.Id? = null
    private var zoomLevel = 4f

    companion object {
        private val locationA = Location("Ota").apply {
            this.latitude = 37.398762
            this.longitude = -121.977216
            this.bearing = 45.0f
        }

        private val locationB = Location("Ota").apply {
            this.latitude = 37.398800
            this.longitude = -121.978000
        }
        private const val TIME_OUT = 1800
    }

    private val spannableStringBuilder = SpannableStringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ota_home_area, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_ota_home_area)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        btn_show_menu.setOnClickListener {
            drawer_layout.open()
        }
        mapViewInit(savedInstanceState)
        operationInit()
    }

    private fun mapViewInit(savedInstanceState: Bundle?) {
        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = {
                mapView.cameraController().position =
                    Camera.Position.Builder().setLocation(locationA).setZoomLevel(10f).build()
                mapView.vehicleController().setLocation(locationA)
                initAnnotation()
            }
        )
        mapView.initialize(mapViewConfig)
    }

    private fun initAnnotation() {
        val factory = mapView.annotationsController().factory()
        val annotationA = factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, locationA)
        val annotationB = factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, locationB)
        mapView.annotationsController().add(listOf(annotationA, annotationB))
    }

    private fun operationInit() {
        btn_update.setOnClickListener {
            otaUpdate()
        }
        btn_reset.setOnClickListener {
            otaReset()
        }
        btn_status.setOnClickListener {
            getOtaStatus()
        }
        btn_map_mode.setOnClickListener {
            showMapMode()
        }
        btn_clear.setOnClickListener {
            spannableStringBuilder.clear()
            tv_log.text = ""
        }
        btn_request_cloud.setOnClickListener {
            requestDirectionCloud()
        }
        btn_request_onboard.setOnClickListener {
            requestDirectionOnboard()
        }
        iv_zoom_in.setOnClickListener {
            zoomLevel += 1
            mapView.cameraController().position =
                Camera.Position.Builder().setLocation(locationA).setZoomLevel(zoomLevel).build()
        }
        iv_zoom_out.setOnClickListener {
            zoomLevel -= 1
            mapView.cameraController().position =
                Camera.Position.Builder().setLocation(locationA).setZoomLevel(zoomLevel).build()
        }
        mapView.setOnTouchListener { touchType: TouchType, data: TouchPosition ->
            if (touchType == TouchType.LongClick && data.geoLocation != null) {
                setHomeArea(data.geoLocation!!)
            }
        }
    }

    /**
     * This method shows how to update ota data using async way.
     */
    private fun otaUpdate() {
        val client = OtaService.getHomeAreaClient()

        addLog("start ota updating...")
        addLog("location: [${homeAreaLocation.latitude},${homeAreaLocation.longitude}]")
        client.updateRequest()
            .setCurrentLocation(homeAreaLocation.latitude, homeAreaLocation.longitude)
            .setTimeout(TIME_OUT)
            .asyncCall(object : Callback<AreaStatus> {
                override fun onFailure(error: Throwable?) {
                    addLog("ota update fail: ${error?.message}", Color.RED)
                }

                override fun onSuccess(areaStatus: AreaStatus) {
                    addLog("ota update ${objectToString(areaStatus)}", Color.BLUE)
                    showArea(areaStatus)
                }
            })
    }

    /**
     * This method shows how to reset ota data using async way.
     */
    private fun otaReset() {
        val request = OtaService.getHomeAreaClient().resetRequest()
        addLog("start ota reset...")
        request.asyncCall(object : Callback<ResetStatus> {
            override fun onSuccess(resetStatus: ResetStatus) {
                addLog("ota reset success", Color.BLUE)
            }

            override fun onFailure(error: Throwable?) {
                addLog("ota reset fail:${error?.message}", Color.RED)
            }
        })
    }

    /**
     * This method shows how to update ota data using sync way.
     */
    private fun getOtaStatus() {
        addLog("getting ota status...")
        val areaStatus = OtaService.getHomeAreaClient().statusRequest().execute()
        addLog("get ota status success：${objectToString(areaStatus)}", Color.BLUE)
        showArea(areaStatus)
    }

    private fun setHomeArea(location: Location) {
        addLog("setting home area...")
        val event = SetHomeEvent.builder()
            .setActionType(SetHomeEvent.ActionType.SET)
            .setEntityId("EntityId")
            .setLabel("setHomeArea")
            .setLat(homeAreaLocation.latitude)
            .setLon(homeAreaLocation.longitude)
        DataCollectorService.getClient()
            .sendEventRequest()
            .setEvent(event.setActionType(SetHomeEvent.ActionType.REMOVE).build())
            .execute()
        DataCollectorService
            .getClient()
            .sendEventRequest()
            .setEvent(event.setActionType(SetHomeEvent.ActionType.SET).build())
            .asyncCall(object : Callback<SendEventResponse> {
                override fun onSuccess(reponse: SendEventResponse) {
                    addLog("Set home area success!", Color.BLUE)
                    homeAreaLocation = location
                    showAnnotation(location)
                }

                override fun onFailure(e: Throwable) {
                    addLog("Set home area failed!", Color.RED)
                    Toast.makeText(requireContext(), "Set home area failed!", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun addLog(text: String, color: Int = Color.BLACK) {
        activity?.runOnUiThread {
            val time = DateFormat.format("HH:mm:ss", Calendar.getInstance())
            val appendText = "$time: $text\n"
            spannableStringBuilder.append(
                appendText, ForegroundColorSpan(color),
                SpannableString.SPAN_INCLUSIVE_INCLUSIVE
            )
            tv_log?.text = spannableStringBuilder
            nestedScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun objectToString(route: Any): String {
        return Gson().toJson(route)
    }

    /**
     * request route in cloud only mode
     */
    private fun requestDirectionCloud() {
        addLog("request cloud route...")
        requestDirection(locationA, locationB, RequestMode.CLOUD_ONLY) {
            if (it) {
                addLog("request cloud route success", Color.BLUE)
            } else {
                addLog("request cloud route fail", Color.RED)
            }
        }
    }

    /**
     * request route in onboard mode
     */
    private fun requestDirectionOnboard() {
        addLog("request onboard route...")
        requestDirection(locationA, locationB, RequestMode.EMBEDDED_ONLY) {
            if (it) {
                addLog("request onboard route success", Color.BLUE)
            } else {
                addLog("request onboard route fail", Color.RED)
            }
        }
    }

    private fun requestDirection(
        begin: Location,
        end: Location,
        model: RequestMode,
        result: (Boolean) -> Unit
    ) {
        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(1)
            .build()
        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, model)
        showRoute()
        task.runAsync { response ->
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                activity?.runOnUiThread {
                    showRoute(response.response.result[0])
                    result(true)
                }

            } else {
                activity?.runOnUiThread {
                    showRoute()
                    result(false)
                }
            }
            task.dispose()
        }
    }

    private fun showRoute(route: Route? = null) {
        activity?.let {
            mapView.routesController().clear()
            if (route != null) {
                val ids = mapView.routesController().add(listOf(route))
                mapView.cameraController().showRegion(mapView.routesController().region(ids))
            }
        }
    }

    /**
     * This function is used to get the map mode for debug use. Please do not write code like this!!
     */
    private fun showMapMode() {
        val service = (SDK.getInstance() as SDKImplement).getMapContentService()
        addLog("map data: " + objectToString(service.version), Color.BLUE)
    }

    private fun showAnnotation(location: Location) {
        mapView.annotationsController().clear()
        val factory = mapView.annotationsController().factory()
        val annotation =
            factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, location)
        mapView.annotationsController().add(listOf(annotation))
    }

    /**
     * This function shows how to draw home area in the map
     */
    private fun showArea(areaStatus: AreaStatus) {
        areaStatus.areaGeometry?.coordinates?.let {
            if (homeAreaRectangleId != null) {
                mapView.shapesController().remove(homeAreaRectangleId!!)
                homeAreaRectangleId = null
            }
            val coords = ArrayList<LatLon>()
            for (point in it) {
                coords.add(LatLon(point.latitude, point.longitude))
            }
            coords.add(coords.first())
            val attributes = Attributes.Builder()
                .setShapeStyle("route.trace")
                .setColor(Color.BLACK)
                .setLineWidth(100.0f)
                .build();

            val shape = Shape(Shape.Type.Polyline, attributes, coords)

            val collectionBuilder = Shape.Collection.Builder()
            collectionBuilder.addShape(shape)

            homeAreaRectangleId = mapView.shapesController().add(collectionBuilder.build())
            if (homeAreaRectangleId != null) {
                mapView.shapesController().setAlphaValue(homeAreaRectangleId!!, 0.5f)
            }
        }
    }
}