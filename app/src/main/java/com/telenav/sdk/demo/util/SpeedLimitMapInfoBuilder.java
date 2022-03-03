package com.telenav.sdk.demo.util;

import com.telenav.sdk.drivesession.model.AdasMessageType;
import com.telenav.sdk.drivesession.model.adas.AdasMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
import com.nio.navi.adc.speedlimit.LocationStatus;
import com.nio.navi.adc.speedlimit.SpeedLimitHorizon;
import com.nio.navi.adc.speedlimit.SpeedLimitMapInfo;
import com.nio.navi.adc.speedlimit.SpeedPoint;
import com.telenav.sdk.drivesession.model.AdasMessageType;
import com.telenav.sdk.drivesession.model.SpeedLimitType;
import com.telenav.sdk.drivesession.model.adas.AdasMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
*/
public class SpeedLimitMapInfoBuilder {
    /*
    private List<AdasMessage> messages;

    private static final byte KPH = 0;
    private static final byte MPH = 1;
    private static final int NOT_LIMIT_SPEED = 999;
    private static final int NO_DEFINE_VALUE = 0;

    public SpeedLimitMapInfoBuilder setMessages(List<AdasMessage> messages) {
        this.messages = messages;
        return this;
    }

    public SpeedLimitMapInfo build() {
        if (messages == null) {
            return null;
        }
        SpeedLimitMapInfo info = new SpeedLimitMapInfo();
        boolean isUnitMetric = isMetric(messages);
        info.setSpeedLimitHorizon(generateHorizon(isUnitMetric, SpeedLimitType.TIME));
        info.setSpeedLimitHorizonRain(generateHorizon(isUnitMetric, SpeedLimitType.RAINY));
        info.setSpeedLimitHorizonSnow(generateHorizon(isUnitMetric, SpeedLimitType.SNOWY));
        info.setSpeedLimitHorizonFog(generateHorizon(isUnitMetric, SpeedLimitType.FOGGY));
        info.setSpeedUnit(isUnitMetric ? KPH : MPH);
        info.setLocationStatus(LocationStatus.LOCATION_OK);
        int countryCode = decodeCountryCode(messages);
        long gpsTimeStamp = decodeGpsTime(messages);
        byte index = getIndex(messages);

        if (countryCode != -1) {
            info.setCountryCode(countryCode);
        }
        if (gpsTimeStamp != -1) {
            info.setGpsTimeStamp(gpsTimeStamp);
        }
        if (index != -1) {
            info.setRoadSegmentChange(index);
        }
        return info;
    }

    private SpeedLimitHorizon generateHorizon(boolean isUnitMetric, int limitType) {
        SpeedLimitHorizon speedLimitHorizon = new SpeedLimitHorizon();
        List<SpeedLimitPoint> timeSpeedList = getSpeedLimitPointsByType(messages, limitType);
        speedLimitHorizon.setSpeedPoint1(getByIndex(timeSpeedList, isUnitMetric, 0));
        speedLimitHorizon.setSpeedPoint2(getByIndex(timeSpeedList, isUnitMetric, 1));
        speedLimitHorizon.setSpeedPoint3(getByIndex(timeSpeedList, isUnitMetric, 2));
        speedLimitHorizon.setSpeedPoint4(getByIndex(timeSpeedList, isUnitMetric, 3));
        return speedLimitHorizon;
    }


    private SpeedPoint generateSpeedPoint(SpeedLimitPoint point, boolean isMetric) {
        SpeedPoint speedPoint = new SpeedPoint();
        speedPoint.setDistance(point.getDistance());
        if (point.isNoSpeedData()) {
            speedPoint.setSpeedLimit(NO_DEFINE_VALUE);
        } else if (point.isNoSpeedLimit()){
            speedPoint.setSpeedLimit(NOT_LIMIT_SPEED);
        }else{
            speedPoint.setSpeedLimit(point.getSpeedInTypeOf(isMetric));
        }
        return speedPoint;
    }

    private SpeedPoint getByIndex(List<SpeedLimitPoint> list, boolean isUnitMetric, int index) {
        if (list == null || list.isEmpty()) {
            SpeedPoint point = new SpeedPoint();
            point.setDistance(NO_DEFINE_VALUE);
            point.setSpeedLimit(NO_DEFINE_VALUE);
            return point;
        }
        if (index < list.size()) {
            return generateSpeedPoint(list.get(index),isUnitMetric);
        }
        return null;
    }
*/
    private static SpeedLimitPoint decodeSpeedLimitMessage(AdasMessage message, boolean invalid) {
        if (message.getProvider() == 3 && message.getMessageType() == 7 && message.getType() == AdasMessageType.CUSTOM_SPEED_LIMIT_TYPE) {
            long content = message.getContent();
            int distance = (int) ((content >> 32) & 0xFFFF);
            int speed = (int) (content & 0xFFFF);
            int limitType = (int) ((content >> 24) & 0xFF);
            int speedUnit = (int) ((content >> 16) & 0xFF);
            int index = (int) ((content >> 48) & 0xFF);
            if (invalid){
                distance = 4096;
                speed = 0xFFFF;
            }
            return new SpeedLimitPoint(speed, distance, speedUnit, limitType, index, 0);
        }
        return null;
    }

    public static int decodeCountryCode(List<AdasMessage> messages) {
        if (messages != null) {
            for (AdasMessage message : messages) {
                if (message.getProvider() == 3 && message.getMessageType() == 7 && message.getType() == AdasMessageType.CUSTOM_COUNTRY_CODE) {
                    return (int) message.getContent();
                }
            }
        }
        return -1;
    }

    private long decodeGpsTime(List<AdasMessage> messages) {
        if (messages != null) {
            for (AdasMessage message : messages) {
                if (message.getProvider() == 3 && message.getMessageType() == 7 && message.getType() == AdasMessageType.CUSTOM_GPS_TIME_TYPE) {
                    return message.getContent();
                }
            }
        }
        return -1;
    }

    private boolean isMetric(List<AdasMessage> messageList) {
        for (AdasMessage message : messageList) {
            SpeedLimitPoint point = decodeSpeedLimitMessage(message,false);
            if (point != null) {
                return point.isMetric();
            }
        }
        return true;
    }

    private byte getIndex(List<AdasMessage> messageList) {
        for (AdasMessage message : messageList) {
            SpeedLimitPoint point = decodeSpeedLimitMessage(message,false);
            if (point != null) {
                return (byte) point.getSegmentIndex();
            }
        }
        return -1;
    }

    public static List<SpeedLimitPoint> getSpeedLimitPointsByType(List<AdasMessage> messageList, int type) {
        List<SpeedLimitPoint> list = new ArrayList<>();
        boolean isInvalid = false;
        for (AdasMessage message : messageList) {
            SpeedLimitPoint point = decodeSpeedLimitMessage(message,isInvalid);
            if (point != null && point.getLimitType() == type) {
                list.add(point);
                if (point.getDistance() >= 4096){
                    isInvalid = true;
                }
            }
        }
        Collections.sort(list, new Comparator<SpeedLimitPoint>() {
            @Override
            public int compare(SpeedLimitPoint o1, SpeedLimitPoint o2) {
                return o1.getDistance() - o2.getDistance();
            }
        });
        return list;
    }

}
