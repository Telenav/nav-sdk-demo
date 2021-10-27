package com.telenav.sdk.demo.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.telenav.sdk.uikit.ImageItems
import com.telenav.sdk.uikit.TnLaneGuidanceView
import com.telenav.sdk.uikit.TnCurrentTurnView
import com.telenav.sdk.map.direction.model.LaneInfo

@BindingAdapter("imageResource")
fun setImageSrc(view: ImageView, imageResource: Int) {
    view.setImageResource(imageResource)
}

@BindingAdapter("laneImages")
fun lanePatternImageData(viewLanePattern: TnLaneGuidanceView, imageList: List<ImageItems>) {
    viewLanePattern.setLanePatternCustomImages(imageList)
}

@BindingAdapter("lanePatternChange")
fun lanePatternChangeData(
    viewLanePattern: TnLaneGuidanceView,
    guideLaneList: List<LaneInfo>
) {
    viewLanePattern.removeAllViews()
    viewLanePattern.populateImages(guideLaneList)
}

@BindingAdapter("currentTurnNextMiles")
fun setCurrentTurnNextMiles(view: TnCurrentTurnView?, text: String?) {
       text?.let{view?.setCurrentTurnNextMiles(it)}
}

@BindingAdapter("currentTurnStreetNextTurn")
fun setCurrentTurnStreetNxtTurn(view: TnCurrentTurnView?, text: String?) {
    text?.let { view?.setCurrentTurnNextTurn(it) }
}

@BindingAdapter("currentTurnDrawable")
fun setImageSrc(view: TnCurrentTurnView?, imageResource: Int) {
    view?.populateImages(imageResource)
}