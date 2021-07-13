package com.telenav.sdk.demo.scenario.ota

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.telenav.sdk.examples.R

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