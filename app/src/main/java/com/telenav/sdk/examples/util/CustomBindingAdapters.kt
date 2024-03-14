package com.telenav.sdk.examples.util

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.telenav.sdk.alert.model.LaneGuidanceNotification
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

@BindingAdapter("laneGuidanceChange")
fun laneGuidanceChangeData(
    viewLanePattern: TnLaneGuidanceView,
    guideLaneList: LaneGuidanceNotification?
) {
    viewLanePattern.renderLaneInfo(guideLaneList)
}

@BindingAdapter("lanePatternChange")
fun lanePatternChangeData(
    viewLanePattern: TnLaneGuidanceView,
    guideLaneList: List<LaneInfo>
) {
}

//@BindingAdapter("laneGuidanceChange")
//fun laneGuidanceChangeData(
//    viewLanePattern: TnLaneGuidanceView,
//    laneGuidance: DisplayLaneGuidance?
//) {
//    viewLanePattern.renderLaneGuidance(laneGuidance)
//}

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

@BindingAdapter("currentShieldIcon")
fun setShieldIcon(view: TnCurrentTurnView?, bitmap: Bitmap?) {
    view?.setShieldIcon(bitmap)
}