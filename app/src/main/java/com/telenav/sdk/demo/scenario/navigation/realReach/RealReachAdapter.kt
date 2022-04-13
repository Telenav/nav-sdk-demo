/*
 *  Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.demo.scenario.navigation.realReach

import android.widget.RadioButton
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.telenav.sdk.map.direction.model.RequestMode

/**
 * @author wu.changzhong on 2022/3/23
 */
@BindingAdapter("requestMode")
fun TextView.requestMode(requestMode: Int) {

    text = when (requestMode) {
        RequestMode.HYBRID.ordinal -> {
            "hybrid"
        }
        RequestMode.CLOUD_ONLY.ordinal -> {
            "cloud"
        }
        RequestMode.EMBEDDED_ONLY.ordinal -> {
            "embedded"
        }
        else -> "hybrid"
    }
}
