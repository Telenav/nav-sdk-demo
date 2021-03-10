/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.demo.util

import com.telenav.sdk.drivesession.model.SpeedLimitType

/**
 * Speed point class which contains speed limit information
 */
data class SpeedLimitPoint(
        /**
         * Speed of the point, if no limit, the value is 0xFFFF.
         */
        val speed: Int,
        /**
         * The distance from this point to vehicle.
         */
        val distance: Int,
        /**
         * the speed unit type, 0 means mile per hour,1 means kilo meter per hour.
         */
        val speedUnit: Int,
        /**
         * the limit type.
         * @see SpeedLimitType
         */
        val limitType: Int,

        /**
         * the index of list, range from 0 to 2.
         */
        val index: Int,
) {
    companion object {
        const val NO_VALUE = 0xFFFF
        const val UNLIMITED_SPEED = 0xFFFE
        const val UNKNOWN_SPEED = 0
        const val KPH = 0
    }

    /**
     * Is the point no speed limit.
     * @return true if there is no limit speed.
     */
    fun isNoSpeedData() = speed == NO_VALUE

    /**
     * Is the point no speed limit.
     * @return true if there is no limit speed.
     */
    fun isUnlimited() = speed == UNLIMITED_SPEED

    /**
     * Is the speed unit is kilo meter per hour.
     * @return true if the unit is kilo meter per hour.
     */
    fun isMetric() = speedUnit == KPH


    fun getSpeedInTypeOf(isMetricUnit : Boolean) : Int{
        return when {
            isMetricUnit == isMetric() -> {
                speed
            }
            speedUnit == KPH -> {
                (speed * 1.609344).toInt()
            }
            else -> {
                (speed * 0.62173).toInt()
            }
        }
    }
}
