package com.telenav.sdk.examples.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel of SetVehicleHeadingDialogFragment
 * @author zhai.xiang on 2021/1/21
 */
class SetVehicleHeadingViewModel : ViewModel() {
    val headingAngle = MutableLiveData<Float>(0.0f)
}