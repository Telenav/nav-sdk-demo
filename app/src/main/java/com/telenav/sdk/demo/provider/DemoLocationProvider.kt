/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.provider

import android.content.Context
import android.location.Location
import com.telenav.sdk.common.model.LocationProvider

/**
 * @author zhai.xiang on 2021/4/1
 */
interface DemoLocationProvider : LocationProvider{
    /**
     * start provide location
     */
    fun start()

    /**
     * stop provide location
     */
    fun stop()

    /**
     * Set location location
     */
    fun setLocation(location : Location)


    object Factory{
        fun createProvider(context: Context, type : ProviderType) : DemoLocationProvider {
            return when (type){
                ProviderType.REAL_GPS -> RealLocationProvider(context)
                ProviderType.FILE -> FileLocationProvider(context)
                else -> SimulationLocationProvider(context)
            }
        }
    }

    enum class ProviderType{
        REAL_GPS,
        SIMULATION,
        FILE
    }
}