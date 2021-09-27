/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.Annotation
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.api.touch.TouchedAnnotation
import com.telenav.map.api.touch.listeners.TouchListener
import com.telenav.map.internal.TnAnnotation
import com.telenav.sdk.demo.util.BitmapUtils
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.fragment_map_view_annotation.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.layout_content_map.btn_show_menu
import kotlinx.android.synthetic.main.layout_content_map.mapView
import kotlinx.android.synthetic.main.layout_operation_annotation.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * This fragment shows how to operate annotation in MapView
 * @author zhai.xiang on 2021/1/13
 */
class MapViewAnnotationFragment : Fragment() {
    private val annotationList = ArrayList<Annotation>()

    private val bundleA = Bundle()
    private val bundleB = Bundle()
    private val pxUnit = 50

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map_view_annotation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_map_view_annotation)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        btn_show_menu.setOnClickListener {
            drawer_layout.open()
        }
        mapViewInit(savedInstanceState)
        setMapViewClickListener()
        setClickListener()
    }

    /**
     * the initialize function must be called after SDK is initialized
     */
    private fun mapViewInit(savedInstanceState: Bundle?) {
        mapView.initialize(savedInstanceState, null)
    }

    /**
     * This method shows how to add click listener to mapView or Annotation
     */
    private fun setMapViewClickListener(){
        mapView.setOnTouchListener(TouchListener { touchType, position ->
            if (touchType == TouchType.LongClick) {
                addAnnotation(position)
            }
        })
        mapView.setOnAnnotationTouchListener { touchType, position, touchedAnnotations ->
            Log.i("ANNOTATION_TOUCH_TAG", "Getting click type ${touchType}, " + "annotation numbers: ${touchedAnnotations.size} ")
            for(item in touchedAnnotations) {
                val mapAnno = item.annotation as TnAnnotation
                Log.i("ANNOTATION_TOUCH_TAG", "touched annotation id: ${mapAnno?.annotationId}")
                if (touchType == TouchType.Click) {
                    removeTheAnnotation(item)
                }
            }
        }
    }

    private fun setClickListener() {

        btn_clear_all.setOnClickListener {
            mapView.annotationsController().clear()
            annotationList.clear()
        }
        btn_clear_A.setOnClickListener {
            removeAnnotationsByFilter(bundleA)
        }
        btn_clear_B.setOnClickListener {
            removeAnnotationsByFilter(bundleB)
        }

        var clickCount = 0
        btn_update.setOnClickListener{
            val resourceId= if (clickCount++ %2 == 0){
                R.drawable.map_pin_orange_icon_unfocused
            }else{
                R.drawable.map_pin_red_icon_unfocused
            }
            updateAnnotation(resourceId)
        }
    }

    private fun updateAnnotation(resourceId :Int){
        val bitmap = BitmapUtils.getBitmapFromVectorDrawable(requireContext(), resourceId)
        var userGraphic = Annotation.UserGraphic(bitmap!!)
        for (annotation in annotationList){

            annotation.setUserGraphic(userGraphic)
        }
        mapView.annotationsController().update(annotationList)
    }

    /**
     * This method shows how to remove the annotation by click.
     */
    private fun removeTheAnnotation(touchedAnnotation: TouchedAnnotation) {
        mapView.annotationsController().remove(listOf(touchedAnnotation.annotation))
        annotationList.remove(touchedAnnotation.annotation)
    }

    /**
     * This method shows how to remove annotations by filter the extra information
     */
    private fun removeAnnotationsByFilter(extraInfo: Bundle) {
        val annotationsToBeRemoved = annotationList.filter {
            it.extraInfo == extraInfo
        }
        mapView.annotationsController().remove(annotationsToBeRemoved)
        annotationList.removeAll(annotationsToBeRemoved)
    }

    /**
     * This method shows how to show an annotation.
     * Setting style, type, extraInfo can help achieve the desired function.
     */
    private fun addAnnotation(position: TouchPosition) {
        val annotation = createAnnotation(position)
        annotation.style = getSelectedStyle()
        annotation.type = getSelectedType()
        annotation.extraInfo = getSelectedBundle()
        val annotationOffset = getAnnotationOffset()
        annotation.iconX = annotationOffset.first
        annotation.iconY = annotationOffset.second
        mapView.annotationsController().add(listOf(annotation))
        val mapAnno = annotation as TnAnnotation
        Log.i("ANNOTATION_TOUCH_TAG", "add annotation id ${mapAnno?.annotationId}")
        annotationList.add(annotation)
    }

    private fun createAnnotation(position: TouchPosition): Annotation {
        return when (rg_create_method.checkedRadioButtonId) {
            R.id.rb_create_resource -> createAnnotationWithResource(position)
            R.id.rb_create_bitmap -> createAnnotationWithBitmap(position)
            R.id.rb_bitmap_text -> createAnnotationWithText(position)
            R.id.rb_create_heavy_congestion_bubble -> createCongestionBubble(Annotation.ExplicitStyle.HeavyCongestionBubble, position)
            R.id.rb_create_light_congestion_bubble -> createCongestionBubble(Annotation.ExplicitStyle.LightCongestionBubble, position)
            else -> createAnnotationWithResource(position)
        }
    }

    /**
     * This method shows how to create an Annotation using resource
     */
    private fun createAnnotationWithResource(position: TouchPosition): Annotation {
        val factory = mapView.annotationsController().factory()
        return factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, position.geoLocation!!)
    }

    /**
     * This method shows how to create an Annotation using bitmap
     */
    private fun createAnnotationWithBitmap(position: TouchPosition): Annotation {
        val factory = mapView.annotationsController().factory()
        val bitmap = createBitmapFromView(createView(position))
        return factory.create(requireContext(), Annotation.UserGraphic(bitmap), position.geoLocation!!)
    }

    private fun createAnnotationWithText(position: TouchPosition) : Annotation {
        val factory = mapView.annotationsController().factory()
        val offset = getTextOffset()
        return factory.create(requireContext(), Annotation.UserGraphic(getBitmap()), position.geoLocation!!).apply {
            this.displayText = Annotation.TextDisplayInfo("Text", offset.first, offset.second).apply {
                this.textColor = Color.RED
            }
        }
    }

    private fun getBitmap() : Bitmap{
        return Bitmap.createBitmap(4 * pxUnit, 4 * pxUnit, Bitmap.Config.ARGB_8888).apply {
            for (i in 0 until 4 * pxUnit){
                for (j in 0 until 4 * pxUnit){
                    val dark = (i / pxUnit + j / pxUnit) % 2 == 0
                    if (dark) {
                        this.setPixel(i, j, Color.BLUE)
                    }else{
                        this.setPixel(i, j, Color.GREEN)
                    }
                }
            }
        }
    }

    /**
     * This method shows how to create a congestion bubble
     */
    private fun createCongestionBubble(explicitStyle : Annotation.ExplicitStyle, position: TouchPosition): Annotation {
        val factory = mapView.annotationsController().factory()
        val congestionBubble = factory.create(explicitStyle, position.geoLocation!!)
        congestionBubble?.displayText = Annotation.TextDisplayInfo.Centered("450 m | 10 min")
        congestionBubble?.getDisplayText()?.textColor = 0xffffffff.toInt()
        congestionBubble?.getDisplayText()?.textSize = 20.0f
        return congestionBubble
    }

    /**
     * Create a view to show
     */
    private fun createView(position: TouchPosition): View {
        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.layout_complex_annotation,
                null, false)
        layout.findViewById<TextView>(R.id.tv_position).text = String.format(Locale.getDefault(),"[ %.6f , %.6f ]",
                position.geoLocation?.latitude ?: 0f, position.geoLocation?.longitude ?: 0f)
        return layout
    }

    /**
     * Create bitmap by view
     */
    private fun createBitmapFromView(view: View): Bitmap {
        val width = Resources.getSystem().displayMetrics.widthPixels
        val height = Resources.getSystem().displayMetrics.heightPixels
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        view.measure(width, height)
        view.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun getSelectedStyle(): Annotation.Style {
        return when (rg_style.checkedRadioButtonId) {
            R.id.rb_style_ScreenAnnotationPopup -> Annotation.Style.ScreenAnnotationPopup
            R.id.rb_style_ScreenAnnotationPopupGrouping -> Annotation.Style.ScreenAnnotationPopupGrouping
            R.id.rb_style_ScreenAnnotationPin -> Annotation.Style.ScreenAnnotationPin
            R.id.rb_style_ScreenAnnotationFlag -> Annotation.Style.ScreenAnnotationFlag
            R.id.rb_style_ScreenAnnotationFlagGrouping -> Annotation.Style.ScreenAnnotationFlagGrouping
            R.id.rb_style_SpriteAnnotationFlag -> Annotation.Style.SpriteAnnotationFlag
            R.id.rb_style_SpriteIncident -> Annotation.Style.SpriteIncident
            R.id.rb_style_SpriteAnnotationFlagGrouping -> Annotation.Style.SpriteAnnotationFlagGrouping
            else -> Annotation.Style.ScreenAnnotationPin
        }
    }

    private fun getSelectedType(): Annotation.Type {
        return when (rg_type.checkedRadioButtonId) {
            R.id.rb_type_Flat -> Annotation.Type.Flat
            R.id.rb_type_Screen2D -> Annotation.Type.Screen2D
            R.id.rb_type_ViewerFacing -> Annotation.Type.ViewerFacing
            R.id.rb_type_LatLonToScreen2D -> Annotation.Type.LatLonToScreen2D
            else -> Annotation.Type.Screen2D
        }
    }

    private fun getSelectedBundle(): Bundle? {
        return when (rg_inject_object.checkedRadioButtonId) {
            R.id.rb_inject_A -> bundleA
            R.id.rb_inject_B -> bundleB
            R.id.rb_inject_default -> null
            else -> null
        }
    }

    private fun getAnnotationOffset() : Pair<Double , Double>{
        return when(rg_annotation_offset.checkedRadioButtonId){
            R.id.rb_annotation_offset_default -> Pair(0.0 ,0.0)
            R.id.rb_annotation_offset_neg -> Pair(-0.5 , -0.5)
            R.id.rb_annotation_offset_pos -> Pair(0.5 , 0.5)
            else -> Pair(0.0 ,0.0)
        }
    }

    private fun getTextOffset() : Pair<Int , Int>{
        return when(rg_text_offset.checkedRadioButtonId){
            R.id.rb_text_offset_default -> Pair(0 ,0)
            R.id.rb_text_offset_neg -> Pair(-pxUnit , -pxUnit)
            R.id.rb_text_offset_pos -> Pair(pxUnit , pxUnit)
            else -> Pair(0 ,0)
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