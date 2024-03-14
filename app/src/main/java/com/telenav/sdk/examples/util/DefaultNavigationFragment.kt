/*
 * Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.util

import androidx.fragment.app.Fragment
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.map.model.AlongRouteTraffic
import com.telenav.sdk.navigation.model.ChargingStationUnreachableEvent
import com.telenav.sdk.navigation.model.TimedRestrictionEdge

/**
 * Default navigation listener
 * @author zhai.xiang on 2022/8/10
 */
open class DefaultNavigationFragment : Fragment(), NavigationEventListener{
    override fun  onNavigationEventUpdated(navEvent: NavigationEvent) {
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
    }

    override fun onTurnByTurnListUpdated(maneuverInfoList: List<ManeuverInfo>) {
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
    }

    override fun onBetterRouteDetected(proposal: BetterRouteProposal) {
    }

    override fun onChargingStationUnreachableEventUpdated(unreachableEvent: ChargingStationUnreachableEvent) {
    }

    override fun onNavigationRouteUpdating(progress: BetterRouteUpdateProgress) {
    }

    override fun onDepartWaypoint(departureWaypointInfo: DepartureWaypointInfo) {
    }

    override fun onTimedRestrictionEventUpdated(timedRestrictionEdges: List<TimedRestrictionEdge>) {
    }
}