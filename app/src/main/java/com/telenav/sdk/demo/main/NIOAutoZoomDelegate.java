package com.telenav.sdk.demo.main;

import androidx.annotation.NonNull;

import com.telenav.map.api.controllers.AutoZoom.AutoZoomData;
import com.telenav.map.api.controllers.AutoZoom.AutoZoomDelegate;
import com.telenav.map.api.controllers.AutoZoom.StepInfo;

public class NIOAutoZoomDelegate implements AutoZoomDelegate {

    private static final float KPH_TO_MS = 1 / 3.6f;
    private float currentZoom = 0;
    private float currentAnimationTime =0;
    @Override
    public AutoZoomData getTargetAutoZoomData(StepInfo stepInfo, boolean is3d, float currentZoomLevel) {
        if (stepInfo == null) {
            return null;
        }

        if (stepInfo.isNextTightTurn()){
            return new AutoZoomData(currentZoomLevel, is3d ? 60f : 0, currentAnimationTime);
        }
        float zoomLevel = getZoomLevel(stepInfo.getDistanceToTurn());
        if (Math.abs(currentZoom - zoomLevel) > 1e-6){
            currentZoom = zoomLevel;
            currentAnimationTime = getAnimationTime(Math.abs(currentZoomLevel - zoomLevel));
            currentAnimationTime = currentAnimationTime > 4 ? 3: currentAnimationTime;

        }


        return new AutoZoomData(zoomLevel, is3d ? 60f : 0, currentAnimationTime);
    }

    @Override
    public void enable(boolean enable) {

    }

    private float getZoomLevel(float distance) {
        //y = 1.4114ln(x) - 2.0923
        if (distance > 6000) {
            return 6.67899787f;             //500;
        } else if (distance > 3800) {
            return 6.272963393f;            //375;
        } else if (distance > 2500) {
            return 5.958018585f;            //300;
        } else if (distance > 1800) {
            return 5.385745131f;            //200;
        } else if (distance > 1200) {
            return 4.979710654f;            //150;
        } else if (distance > 800) {
            return 4.407437201f;            //100;
        } else if (distance > 400) {
            return 4.001402723f;            //75;
        } else if (distance > 200) {
            return 3.42912927f;             //50;
        } else if (distance > 100) {
            return 2.708149984f;            //30;
        } else {
            return 2.135876531f;            //20;
        }
    }

    private float getZoomLevelBaseSpeed(float speed){
        if (speed > 100 * KPH_TO_MS){
            return  5.385745131f;            //200;
        }else if (speed > 80 * KPH_TO_MS){
            return 4.407437201f;            //100;
        }else{
            return 3.42912927f;             //50;
        }
    }

    private float getAnimationTime(float delta) {
        if(delta <=  1e-6)
            return 0;
        return delta * 3.0f;
    }
}
