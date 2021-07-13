package com.telenav.sdk.demo.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.main.SecondFragment

class SecondActivity : AppCompatActivity() {
    var fragmentManager: FragmentManager? = null
    var beginTransaction : FragmentTransaction? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actiivty_second)
        fragmentManager = supportFragmentManager
        beginTransaction = fragmentManager?.beginTransaction()
        val secondFragment = SecondFragment()
        beginTransaction?.add(R.id.container,secondFragment);
        beginTransaction?.commit()
    }
}