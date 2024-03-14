/*
 * Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.util

import com.telenav.sdk.common.model.*

/**
 * Help to generate vehicle VehicleInfoConfig
 * @author chzhwu@telenav.cn on 2022/7/03
 */
object VehicleInfoConfigHelper {

    fun createVehicleInfoConfig(): VehicleInfoConfig {
        val evConnectorTypes = ArrayList<Int>()
        evConnectorTypes.add(EvConnectorType.COMBO_1)
        val energyProfile =
            EnergyProfile.Builder().setEnergyType(3).setFuelCapacity(2.0f).setEvBatteryCapacity(65f)
                .setEvConnectorTypes(evConnectorTypes).setEvEnergyConsumption(150.0f)
                .setEvMaxChargingPower(87.0f)
                .build()

        return VehicleInfoConfig.Builder()
            .setModelName("tesla_model3")
            .setDrivetrainType(DrivetrainType.FOUR_WHEEL_DRIVE)
            .setEnergyLevel(EnergyLevel(3.4f, 15f))
            .setEnergyProfile(energyProfile)
            .setVehicleDimensions(VehicleDimensions(100, 200, 400, 600))
            .setVehicleStatus(VehicleStatus(1, 1))
            .setVehicleCategory(VehicleCategory.AUTO)
            .build()
    }

    fun createTruckInfoConfig(): VehicleInfoConfig {

        val axleProfile =
            AxleProfile.Builder().setMaxWeightPerAxle(2500).setMaxDistanceBetweenAxles(4550)
                .setAmountOfAxles(2).build()

        return VehicleInfoConfig.Builder()
            .setModelName("truck")
            .setDrivetrainType(DrivetrainType.FOUR_WHEEL_DRIVE)
            .setVehicleDimensions(VehicleDimensions(700, 210, 278, 4250, 4250))
            .setVehicleCategory(VehicleCategory.TRUCK)
            .setAxleProfile(axleProfile)
            .setHazardousMaterialTypes(listOf(HazardousMaterialType.EXPLOSIVE))
            .setTunnelRestrictionCode(TunnelRestrictionCode.TUNNEL_RESTRICTION_CODE_B)
            .build()
    }

    fun createEVVehicleInfoConfig(): VehicleInfoConfig {

        val evConnectorTypes = ArrayList<Int>()
        evConnectorTypes.add(EvConnectorType.COMBO_1)
        val energyProfile = EnergyProfile(EnergyType.ELECTRIC, 0f, 65f, evConnectorTypes, 150.0f)

        return VehicleInfoConfig.Builder()
            .setModelName("tesla_model3")
            .setDrivetrainType(DrivetrainType.FOUR_WHEEL_DRIVE)
            .setEnergyLevel(EnergyLevel(0f, 100f))
            .setEnergyProfile(energyProfile)
            .setVehicleDimensions(VehicleDimensions(100, 200, 400, 600))
            .setVehicleStatus(VehicleStatus(1, 1))
            .setVehicleCategory(VehicleCategory.AUTO)
            .build()
    }
}