package com.telenav.sdk.examples.scenario.mapview

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.Camera
import com.telenav.map.geo.Attributes
import com.telenav.map.geo.Shape
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.provider.DemoLocationProvider
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentMapSnapshotBinding

/**
 * Shows how to use the Snapshot API
 * @author wu.changzhong on 2021/7/15
 */
class MapViewSnapshotsFragment : Fragment() {
    private var _binding: FragmentMapSnapshotBinding? = null
    private val binding get() = _binding!!

    lateinit var locationProvider: DemoLocationProvider
    lateinit var dialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapSnapshotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(
            requireContext(),
            DemoLocationProvider.ProviderType.SIMULATION
        )
        binding.include.tvTitle.text = getString(R.string.title_activity_map_view_snaps_shots)
        binding.include.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit(savedInstanceState)
        binding.mapOffscreenSnapshots.setOnClickListener {
            if (::dialog.isInitialized && dialog.isShowing) {
                dialog.dismiss()
            }
            testOffscreenSnapshot()
        }
        binding.mapSnapshots.setOnClickListener {
            if (::dialog.isInitialized && dialog.isShowing) {
                dialog.dismiss()
            }
            testSnapshot()
        }
    }

    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        activity?.runOnUiThread {
            binding.mapView.vehicleController().setLocation(locationProvider.getLastKnownLocation())
        }
    }

    /**
     * the initialize function must be called after SDK is initialized
     */
    private fun mapViewInit(savedInstanceState: Bundle?) {
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = mapViewReadyListener
        )
        binding.mapView.initialize(mapViewConfig)
    }

    private fun testOffscreenSnapshot() {
        binding.mapOffscreenSnapshots.text = "Generating offscreen screenshots"
        binding.mapOffscreenSnapshots.isEnabled = false
        binding.mapSnapshots.isEnabled = false
        binding.mapView.generateOffscreenSnapshot(1000, 1000, {
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
            region.extend(30.4935, -86.5958)
            region.extend(30.4935, -86.4958)

            it!!.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))

            // Draw a pseudo-home area rectangle
            val coords = ArrayList<LatLon>()
            coords.add(LatLon(30.3935, -86.4958))
            coords.add(LatLon(30.3935, -86.5958))
            coords.add(LatLon(30.4935, -86.5958))
            coords.add(LatLon(30.4935, -86.4958))
            coords.add(coords.first())

            val attributes = Attributes.Builder()
                .setShapeStyle("route.trace")
                .setColor(0xFF00FF00.toInt())
                .setLineWidth(100.0f)
                .build()

            val shape = Shape(Shape.Type.Polyline, attributes, coords)

            val collectionBuilder = Shape.Collection.Builder()
            collectionBuilder.addShape(shape)

            // Don't have to keep track of shapeId since entire view will be closed
            val shapeId = it.shapesController().add(collectionBuilder.build())
        }, {
            activity?.runOnUiThread {
                binding.mapOffscreenSnapshots.text = "Generate offscreen screenshots"
                binding.mapOffscreenSnapshots.isEnabled = true
                binding.mapSnapshots.isEnabled = true
                displayBitmap(it, "Offscreen Snapshot")
            }
        })
    }

    private fun testSnapshot() {
        binding.mapSnapshots.text = "Generating screenshots"
        binding.mapSnapshots.isEnabled = false
        binding.mapOffscreenSnapshots.isEnabled = false
        binding.mapView.generateSnapshot {
            activity?.runOnUiThread {
                binding.mapSnapshots.text = "Generate screenshots"
                binding.mapSnapshots.isEnabled = true
                binding.mapOffscreenSnapshots.isEnabled = true
                displayBitmap(it, "Screen Snapshot")
            }
        }
    }

    private fun displayBitmap(bitmap: Bitmap, message: String) {
        if (context != null) {
            dialog = Dialog(requireContext())
            dialog.setTitle(message)
            //builder.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )
            val imageView = ImageView(context)
            imageView.setImageBitmap(bitmap)
            dialog.addContentView(
                imageView, RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            dialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}