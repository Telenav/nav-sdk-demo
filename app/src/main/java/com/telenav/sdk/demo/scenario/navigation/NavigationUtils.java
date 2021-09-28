/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation;

import com.google.gson.Gson;
import com.telenav.sdk.common.model.LatLon;
import com.telenav.sdk.drivesession.model.AlongRouteTrafficIncidentInfo;
import com.telenav.sdk.drivesession.model.TrafficIncidentLocation;

/**
 * Navigation tool class
 * @author wuchangzhong on 2021/09/14
 */
public class NavigationUtils {

    /**
     * Get the location of ncident along the route
     * @param alongRouteTrafficIncidentInfo
     * @return
     */
    public static String getIncidentLatLon(AlongRouteTrafficIncidentInfo alongRouteTrafficIncidentInfo) {
        if (alongRouteTrafficIncidentInfo != null) {
            TrafficIncidentLocation incidentLocation = alongRouteTrafficIncidentInfo.getIncidentLocation();
            if (incidentLocation != null) {
                LatLon location = incidentLocation.getIncidentLocation();
                if (location != null) {
                    return new Gson().toJson(location);
                }
            }
        }
        return null;
    }
}
