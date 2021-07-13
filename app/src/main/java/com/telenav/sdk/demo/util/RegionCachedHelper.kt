/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.util

import android.content.Context
import com.google.gson.Gson
import com.telenav.sdk.common.model.Region

/**
 * @author zhai.xiang on 2021/2/20
 */
object RegionCachedHelper{
    private const val KEY_MODEL = "model"

    fun saveSDKDataModel(context: Context, model : InitSDKDataModel){
        PreferenceUtil.saveString(context, KEY_MODEL, Gson().toJson(model))
    }

    fun getSDKDataModel(context: Context): InitSDKDataModel?{
        val text = PreferenceUtil.getString(context, KEY_MODEL)
        return Gson().fromJson(text, InitSDKDataModel::class.java)
    }

    fun getRegion(context: Context):Region {
        return getSDKDataModel(context)?.region ?: Region.NA
    }
}