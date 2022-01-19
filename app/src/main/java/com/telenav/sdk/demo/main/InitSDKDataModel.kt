/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.main

import android.text.TextUtils
import com.telenav.sdk.common.model.Region

/**
 * @author zhai.xiang on 2021/2/24
 */
data class InitSDKDataModel(val region: Region, val mapDataPath: String,val key : String, val secret: String,
                            val url : String, val tag: String? = null) {

    fun getDisplayText(): String {
        val isStreamingText = when{
            TextUtils.isEmpty(url) && TextUtils.isEmpty(mapDataPath) ->"-error"
            TextUtils.isEmpty(url) -> "-embedded($mapDataPath)"
            TextUtils.isEmpty(mapDataPath) -> "-pure streaming"
            else -> "-hybrid($mapDataPath)"
        }

        return if (tag != null && tag.isNotEmpty()) {
            region.name + isStreamingText + " " + tag;
        } else {
            region.name + isStreamingText
        }
    }
}