/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.telenav.sdk.demo.main.LanePatternCustomImageView
import com.telenav.sdk.map.direction.model.GuidanceLaneInfo

@BindingAdapter("imageResource")
fun setImageSrc(view: ImageView, imageResource: Int) {
    view.setImageResource(imageResource)
}

data class ImageItems(val tag: Int, val drawable: Int)

@BindingAdapter("laneImages")
fun lanePatternImageData(viewLanePattern: LanePatternCustomImageView, imageList: List<ImageItems>) {
    viewLanePattern.setLanePatternCustomImages(imageList)
}

@BindingAdapter("lanePatternChange")
fun lanePatternChangeData(
    viewLanePattern: LanePatternCustomImageView,
    guideLaneList: List<GuidanceLaneInfo>
) {
    viewLanePattern.removeAllViews()
    viewLanePattern.populateImages(guideLaneList)
}
