/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.telenav.sdk.examples.R

/**
 * This activity contains scenarios related to map operations.
 * Therefore, the user could know how to operate the map.
 * @author zhai.xiang on 2021/1/11
 */
class MapViewActivity : AppCompatActivity(){
    companion object{
        const val TAG = "MapViewMenu"
        fun getCallingIntent(context : Context) = Intent(context, MapViewActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_view)
    }
}