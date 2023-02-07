package com.telenav.sdk.demo.scenario.mapview

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.geo.Attributes
import com.telenav.map.geo.Shape
import com.telenav.sdk.demo.provider.DemoLocationProvider
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.fragment_map_snapshot.*
import kotlinx.android.synthetic.main.fragment_map_view_set_up.mapView
import kotlinx.android.synthetic.main.layout_action_bar.*

/**
 * Shows how to use the Snapshot API
 * @author wu.changzhong on 2021/7/15
 */
class MapViewSnapshotsFragment : Fragment() {

    lateinit var locationProvider : DemoLocationProvider
    lateinit var dialog :Dialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_snapshot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(requireContext(), DemoLocationProvider.ProviderType.SIMULATION)
        locationProvider.start()
        tv_title.text = getString(R.string.title_activity_map_view_snaps_shots)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit(savedInstanceState)
        mapOffscreenSnapshots.setOnClickListener {
            if (::dialog.isInitialized && dialog.isShowing) {
                dialog.dismiss()
            }
            testOffscreenSnapshot()
        }
        mapSnapshots.setOnClickListener {
            if (::dialog.isInitialized && dialog.isShowing) {
                dialog.dismiss()
            }
            testSnapshot()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationProvider.stop()
    }

    /**
     * the initialize function must be called after SDK is initialized
     */
    private fun mapViewInit(savedInstanceState: Bundle?){

        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = {
                activity?.runOnUiThread {
                    mapView.vehicleController().setLocation(locationProvider.lastKnownLocation)
                }
            }
        )
        mapView.initialize(mapViewConfig)
    }

    private fun testOffscreenSnapshot() {
        mapOffscreenSnapshots.text = "Generating offscreen screenshots"
        mapOffscreenSnapshots.isEnabled = false
        mapSnapshots.isEnabled = false
        mapView.generateOffscreenSnapshot(1000, 1000, {
            // Configure snapshot, set region, add annotations, load styles, etc...
            // Destin, FL

            //val location = Location("")
            //location.latitude = 30.3935
            //location.longitude = -86.4958
            //it.cameraController().position = Camera.Position.Builder().setLocation(location).setZoomLevel(7f).build()

            // Show the region of a pseudo-home area rectangle
            val region = Camera.Region()
            region.extend(30.3935, -86.4958)
            region.extend(30.3935, -86.5958)
            region.extend(30.4935,-86.5958)
            region.extend(30.4935,-86.4958)

            it.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))

            // Draw a pseudo-home area rectangle
            val coords = ArrayList<Location>()

            val rightDownCorner = Location("")
            rightDownCorner.latitude = 30.3935
            rightDownCorner.longitude = -86.4958
            coords.add(rightDownCorner)

            val leftDownCorner = Location("")
            leftDownCorner.latitude = 30.3935
            leftDownCorner.longitude = -86.5958
            coords.add(leftDownCorner)

            val leftUpCorner = Location("")
            leftUpCorner.latitude = 30.4935
            leftUpCorner.longitude = -86.5958
            coords.add(leftUpCorner)

            val rightUpcorner = Location("")
            rightUpcorner.latitude = 30.4935
            rightUpcorner.longitude = -86.4958
            coords.add(rightUpcorner)

            coords.add(coords.first())

            val attributes = Attributes.Builder()
                .setShapeStyle("route.trace")
                .setColor(0xFF00FF00.toInt())
                .setLineWidth(100.0f)
                .build()

            val shape = Shape(Shape.Type.Polyline, coords, attributes)

            // Don't have to keep track of shapeId since entire view will be closed
            it.shapesController().add(Shape.Collection.Builder()
                .addShape(shape)
                .build())
        }, {activity?.runOnUiThread {
            mapOffscreenSnapshots.text = "Generate offscreen screenshots"
            mapOffscreenSnapshots.isEnabled = true
            mapSnapshots.isEnabled = true
            displayBitmap(it, "Offscreen Snapshot")
        }})
    }

    private fun testSnapshot() {
        mapSnapshots.text = "Generating screenshots"
        mapSnapshots.isEnabled = false
        mapOffscreenSnapshots.isEnabled = false
        mapView.generateSnapshot {
            activity?.runOnUiThread {
                mapSnapshots.text = "Generate screenshots"
                mapSnapshots.isEnabled = true
                mapOffscreenSnapshots.isEnabled = true
                displayBitmap(it, "Screen Snapshot")
            }
        }
    }

    private fun displayBitmap(bitmap: Bitmap, message: String) {
        if (context!=null){
            dialog = Dialog(requireContext())
            dialog.setTitle(message)
            //builder.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT))
            val imageView = ImageView(context)
            imageView.setImageBitmap(bitmap)
            dialog.addContentView(imageView, RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))
            dialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}