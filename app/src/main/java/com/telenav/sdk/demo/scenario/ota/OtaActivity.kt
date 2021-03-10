/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.ota

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.telenav.sdk.demo.R
import kotlinx.coroutines.*

/**
 * @author zhai.xiang on 2021/1/25
 */
class OtaActivity : AppCompatActivity() {
    companion object{
        fun getCallingIntent(context : Context) = Intent(context, OtaActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ota)
    }

}