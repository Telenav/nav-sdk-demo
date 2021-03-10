/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.main

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.telenav.sdk.demo.R
import com.telenav.sdk.demo.util.ImageItems
import com.telenav.sdk.demo.util.TurnImageUtil

import com.telenav.sdk.map.direction.model.GuidanceLaneInfo
import com.telenav.sdk.map.direction.model.HighlightedLane

class LanePatternCustomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    LinearLayout(context, attrs, defStyleAttr) {
    private val laneCustomImages: MutableList<ImageItems> = mutableListOf<ImageItems>()

    init {
        LayoutInflater.from(context).inflate(R.layout.custom_image_view, this, true)
    }

    /**
     * we can set custom images here if one wants to change default images
     * @param imageList : list of ImageItems
     * */
    fun setLanePatternCustomImages(imageList: List<ImageItems>) {
        laneCustomImages.clear()
        laneCustomImages.addAll(imageList)
    }

    fun populateImages(guideLaneList: List<GuidanceLaneInfo>) {
        guideLaneList.forEach {
            val dynamicImage = ImageView(context)
            dynamicImage.layoutParams = LinearLayout.LayoutParams(60, 60)
            val params = dynamicImage.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(20, 5, 20, 5)

            dynamicImage.layoutParams = params
            val directionImage = TurnImageUtil.getTurnImageRes(it.pattern, laneCustomImages)
            dynamicImage.setImageResource(directionImage)

            setAlpha(dynamicImage, it.highlight)
            this.addView(dynamicImage)
        }
    }

    private fun setAlpha(image: ImageView, highlight: Int) {
        image.alpha = .3F
        when (highlight) {
            HighlightedLane.CONTINUE, HighlightedLane.LEFT, HighlightedLane.RIGHT,
            HighlightedLane.LEFT_UTURN, HighlightedLane.RIGHT_UTURN -> image.alpha = 1f
        }

    }
}