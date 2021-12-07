/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.util

import com.telenav.sdk.common.model.Category
import com.telenav.sdk.common.model.EnergyConsumptionModel
import com.telenav.sdk.common.model.VehicleProfile
import com.telenav.sdk.common.model.VehicleType

/**
 * Help to generate vehicle profile with different type
 * @author zhai.xiang on 2021/11/11
 */
object VehicleProfileHelper {
    fun createVehicleProfile(@Category category: Int): VehicleProfile {
        val vehicleType = VehicleType.Factory.createInstance(category)!!
        val consumptionModel = EnergyConsumptionModel.Builder().build()
        return VehicleProfile.Factory.Companion.create(vehicleType, 0, 0,
                0, 0, consumptionModel, 0.5)
    }
}