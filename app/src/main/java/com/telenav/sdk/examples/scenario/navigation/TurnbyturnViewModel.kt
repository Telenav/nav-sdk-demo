/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.telenav.sdk.examples.util.AndroidThreadUtils
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.model.ManeuverInfo
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.model.Action
import com.telenav.sdk.map.direction.model.LaneInfo
import com.telenav.sdk.uikit.ImageItems
import com.telenav.sdk.uikit.turn.TnTurnListItem
import com.telenav.sdk.uikit.turn.TnTurnListRecyclerViewAdapter
import java.math.BigDecimal
import java.math.RoundingMode

class TurnbyturnViewModel(val tnTurnListAdapter: TnTurnListRecyclerViewAdapter) : ViewModel() {

    val timeToArrival = MutableLiveData<String>()
    val showNavigationDetails = MutableLiveData(false)
    val totalDistanceRemaining = MutableLiveData<String>()
    val tripTimeRemaining = MutableLiveData<String>()
    val distanceRemainingToNextTurn = MutableLiveData<String>()
    val turnDirectionDrawable = MutableLiveData<Int>()
    val nextTurnStreetName = MutableLiveData<String>()
    val turnListVisibility = MutableLiveData(false)

    val LaneAssets = MutableLiveData<List<ImageItems>>(listOf())
    val laneInfo = MutableLiveData<List<LaneInfo>>(listOf())
    val lanePatternCustomImages = MutableLiveData<List<ImageItems>>(listOf())

    private var currentLegIndex = 0
    private var currentStepIndex = 0

    fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        navEvent.travelEstToDestination?.let {
            if (!it.arrivalToStop.isNullOrEmpty()) {
                timeToArrival.postValue(it.arrivalToStop)
            }

        }

        navEvent.travelEstToDestination?.let {
            totalDistanceRemaining.postValue(getMilesOrFeet(it.distanceToStop))
            tripTimeRemaining.postValue(getTimeRemainingToArrival(it.timeToStop))
        }
        updateTurnListItem(navEvent)
        distanceRemainingToNextTurn.postValue(getMilesOrFeet(navEvent.distanceToTurn))
        navEvent.currentManeuver?.let {
            turnDirectionDrawable.postValue(getTurnDrawable(it.turnAction))
            nextTurnStreetName.postValue(it.streetName)
            it.laneInfo?.let { laneInfoList -> laneInfo.postValue(laneInfoList) }
                ?: laneInfo.postValue(listOf())
        }
    }

    private var navigationSession: NavigationSession? = null

    fun handleNavTurnList(navigationSession: NavigationSession?) {
        this.navigationSession = navigationSession
        navigationSession?.let { navSession ->
            navSession.getManeuverList()?.let {
                val turnList = getTurnListItem(it)
                tnTurnListAdapter.setupData(turnList)
                turnListVisibility.postValue(turnList.isNotEmpty())
            }
        }
    }

    //private methods
    private fun updateTurnListItem(navEvent: NavigationEvent) {
        if (!((currentLegIndex == navEvent.legIndex) && (currentStepIndex == navEvent.stepIndex))) {
            currentLegIndex = navEvent.legIndex
            currentStepIndex = navEvent.stepIndex
            AndroidThreadUtils.runOnUiThread(Runnable {
                navigationSession?.let { navSession ->
                    navSession.getManeuverList()?.let {
                        val turnList = getTurnListItem(it)
                        tnTurnListAdapter.setupData(turnList)
                        turnListVisibility.postValue(turnList.isNotEmpty())
                    }
                }
            })
        }
    }

    private fun getTurnListItem(maneuverList: List<ManeuverInfo>): List<TnTurnListItem> =
        maneuverList
            .filter { it.legIndex > currentLegIndex || (it.legIndex == currentLegIndex && it.stepIndex > currentStepIndex) }
            .map {
                TnTurnListItem(
                    it.streetName,
                    getMilesOrFeet(it.lengthMeters),
                    getTurnDrawable(it.turnAction),
                    it.stepInfo
                )
            }

    private fun getTimeRemainingToArrival(timeToStop: Int): String {
        return if (timeToStop > 60) {
            "${(timeToStop / 60)} min"
        } else {
            " 1 min"
        }
    }

    private fun getMilesOrFeet(distanceToStop: Double): String {
        val miles = distanceToStop * 0.00062137
        return if (miles > 0.1) {
            "${BigDecimal(miles).setScale(1, RoundingMode.HALF_EVEN)} mi"
        } else {
            "${BigDecimal(distanceToStop * 3.2808).setScale(0, RoundingMode.HALF_EVEN)} ft"
        }
    }

    private fun getTurnDrawable(@Action turnType: Int): Int {
        return when (turnType) {
            Action.TURN_RIGHT -> R.drawable.ic_turn_right_white
            Action.TURN_LEFT -> R.drawable.ic_turn_left_white
            Action.CONTINUE -> R.drawable.ic_continue_straight
            Action.TURN_SLIGHT_LEFT -> R.drawable.ic_turn_slight_left_white
            Action.TURN_SLIGHT_RIGHT -> R.drawable.ic_turn_slight_right
            Action.STOP_RIGHT -> R.drawable.ic_stop_right
            Action.STOP_LEFT -> R.drawable.ic_stop_left
            else -> R.drawable.ic_continue_straight
        }
    }


}


