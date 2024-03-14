## **Version 3.4.0-LTS4-RC12.1**
2024-10-17
### Breaking Changes
N/A

### New Features
* Expose navigation session id. [ANDROID-10982](https://jira.telenav.com:8443/browse/ANDROID-10982)
* Add new region: ISR. [ANDROID-11014](https://jira.telenav.com:8443/browse/ANDROID-11014)
* Expose AutoZoomLevel config for AutoZoom feature. [ANDROID-10976](https://jira.telenav.com:8443/browse/ANDROID-10976)

### Enhancements
* Cancel POI search in High layer(7-9 zoom level). [ANDROID-10384](https://jira.telenav.com:8443/browse/ANDROID-10384)
* Remove unreasonable stop auto zoom logic. [ANDROID-10959](https://jira.telenav.com:8443/browse/ANDROID-10959)
* Support network status set before SDK init. [ANDROID-11106](https://jira.telenav.com:8443/browse/ANDROID-11106)

### Bug Fixes
* Fix dataArray leak at sendEvent in DataCollector. [ANDROID-11121](https://jira.telenav.com:8443/browse/ANDROID-11121)

## **Version 3.4.0-LTS4-RC11.2**
2024-09-11
### Breaking Changes
* VehicleInfoProvider accept NonNull parameter. [ANDROID-10641](https://jira.telenav.com:8443/browse/ANDROID-10641)
* loadStyleSheet api changed. [ANDROID-10411](https://jira.telenav.com:8443/browse/ANDROID-10411)
* Add more attributes of on-street-parking data. [ANDROID-10907](https://jira.telenav.com:8443/browse/ANDROID-10907)

### New Features
* Supply ease parking level query in mapcontent. [ANDROID-10604](https://jira.telenav.com:8443/browse/ANDROID-10604)

### Enhancements
* MapView module log uniform. [ANDROID-10753](https://jira.telenav.com:8443/browse/ANDROID-10753)
* ManeuverInfo street name will not get from route. [ANDROID-10768](https://jira.telenav.com:8443/browse/ANDROID-10768)
* Clear the taskQueue when surface be destroyed to avoid map delay rendering. [ANDROID-10701](https://jira.telenav.com:8443/browse/ANDROID-10701)
* Expose show FPS API to HMI. [ANDROID-10828](https://jira.telenav.com:8443/browse/ANDROID-10828)

### Bug Fixes
* Fix egl swap buffer error cause 1-2s black screen when back to foreground. [ANDROID-10702](https://jira.telenav.com:8443/browse/ANDROID-10702)
* Add valid check for charging_duration. [ANDROID-10824](https://jira.telenav.com:8443/browse/ANDROID-10824)

## **Version 3.4.0-LTS4-RC10.1**
2024-08-12
### Breaking Changes
N/A

### New Features
* Expose session id and guidance stage. [ANDROID-10514](https://jira.telenav.com:8443/browse/ANDROID-10514)

### Enhancements
* Update targetSdkVersion to 33(Android13). [ANDROID-10414](https://jira.telenav.com:8443/browse/ANDROID-10174)
* Expose edgeIndex and edgePointIndex in NavigationEvent. [ANDROID-10435](https://jira.telenav.com:8443/browse/ANDROID-10435)
* OnStreetParkingLot data model improvement. [ANDROID-10609](https://jira.telenav.com:8443/browse/ANDROID-10609)
* DataCollector add one more LAST_TRIP_WITH_CHALLENGE. [TASDK-42317](https://jira.telenav.com:8443/browse/TASDK-42317)

### Bug Fixes
* Route arrival times not plus the charging duration. [ANDROID-10276](https://jira.telenav.com:8443/browse/ANDROID-10276)
* Fix onLongPress unexpected triggered. [ANDROID-10355](https://jira.telenav.com:8443/browse/ANDROID-10355)
* Fix the TBT info not updated correctly after accept the better route proposal. [ANDROID-10626](https://jira.telenav.com:8443/browse/ANDROID-10626)

## **Version 3.4.0-LTS4-RC9.2**
2024-07-16
### Breaking Changes
* Change the branch and towards name from item to list in ManeuverInfo. [ANDROID-9299](https://jira.telenav.com:8443/browse/ANDROID-9299)
* Add onTimedRestrictionEventUpdated to NavigationEventListener. [ANDROID-10477](https://jira.telenav.com:8443/browse/ANDROID-10477)

### New Features
* Expose setMaxOnboardRouteCount. [ANDROID-10169](https://jira.telenav.com:8443/browse/ANDROID-10169)
* Supply on-street parking query in MapContent. [ANDROID-10353](https://jira.telenav.com:8443/browse/ANDROID-10353)
* Support rendering on-street parking. [ANDROID-10290](https://jira.telenav.com:8443/browse/ANDROID-10290)
* Provide proposal when low arrival battery is detected. [ANDROID-9720](https://jira.telenav.com:8443/browse/ANDROID-9720)

### Enhancements
* Update targetSdkVersion to 31(Android12). [ANDROID-10174](https://jira.telenav.com:8443/browse/ANDROID-10174)
* Use more high precision to decode Polyline. [ANDROID-9799](https://jira.telenav.com:8443/browse/ANDROID-9799)
* Remove the '10 meter' restriction for auto zoom in AGV mode. [ANDROID-10240](https://jira.telenav.com:8443/browse/ANDROID-10240)
* TASDK verbose log improvement. [ANDROID-10119](https://jira.telenav.com:8443/browse/ANDROID-10119)

### Bug Fixes
N/A

## **Version 3.4.0-LTS4-RC8.1**
2024-06-21
### Breaking Changes
* Change setVehicleCategory to internal method. [ANDROID-10098](https://jira.telenav.com:8443/browse/ANDROID-10098)

### New Features
N/A

### Enhancements
N/A

### Bug Fixes
* Fix search task status synchronization issue. [ANDROID-10126](https://jira.telenav.com:8443/browse/ANDROID-10126)
* Fix RouteUpdateFailureContext.route is incorrect. [ANDROID-10251](https://jira.telenav.com:8443/browse/ANDROID-10251)

## **Version 3.4.0-LTS4-RC7.1**
2024-06-03
### Breaking Changes
N/A

### New Features
* Provide truck restriction reason and location for Commercial Vehicles. [ANDROID-9877](https://jira.telenav.com:8443/browse/ANDROID-9877)
* Provide Better Route Service for Commercial Vehicles. [ANDROID-9819](https://jira.telenav.com:8443/browse/ANDROID-9819)

### Enhancements
* Add new region TUR for Turkey. [ANDROID-9709](https://jira.telenav.com:8443/browse/ANDROID-9709)
* Replace byte buffer with byte array to receive native message. [ANDROID-10050](https://jira.telenav.com:8443/browse/ANDROID-10050)

### Bug Fixes
* Fix the problem of concurrent operation of routes. [ANDROID-10018](https://jira.telenav.com:8443/browse/ANDROID-10018)

## **Version 3.4.0-LTS4-RC6.1**
2024-05-09
### Breaking Changes
* New vehicle profile support for truck. [ANDROID-9549](https://jira.telenav.com:8443/browse/ANDROID-9549)

### New Features
* Expose local arrival times of each waypoint on route. [ANDROID-9363](https://jira.telenav.com:8443/browse/ANDROID-9363)

### Enhancements
* Data collector update reward feature interaction and interaction events enum. [TASDK-40561](https://jira.telenav.com:8443/browse/TASDK-40561)

### Bug Fixes
* Fix crash reported by Firebase. [ANDROID-9527](https://jira.telenav.com:8443/browse/ANDROID-9527) [ANDROID-9538](https://jira.telenav.com:8443/browse/ANDROID-9538)


## **Version 3.4.0-LTS4-RC5.1**
2024-04-03
### Breaking Changes
N/A

### New Features
* Expose log handler to client. [ANDROID-9544](https://jira.telenav.com:8443/browse/ANDROID-9544)

### Enhancements
* Expose coroutine dispatcher for search poi. [ANDROID-9657](https://jira.telenav.com:8443/browse/ANDROID-9657)
* tasdk log improvement. [ANDROID-9279](https://jira.telenav.com:8443/browse/ANDROID-9279)

### Bug Fixes
* Fix route not remove after cancel trip. [ANDROID-9514](https://jira.telenav.com:8443/browse/ANDROID-9514)


## **Version 3.4.0-LTS4-RC4.3**
2024-03-15
### Breaking Changes
N/A

### New Features
N/A

### Enhancements
* DataCollector enlarge network usage budget. [ANDROID-9488](https://jira.telenav.com:8443/browse/ANDROID-9488)

### Bug Fixes
* Convert traffic flow speed unit from km/h to m/s. [ANDROID-9485](https://jira.telenav.com:8443/browse/ANDROID-9485)
* Fix a native crash in the intersection view. [ANDROID-9467](https://jira.telenav.com:8443/browse/ANDROID-9467)

## **Version 3.4.0-LTS4-RC4.1**
2024-03-08
### Breaking Changes
N/A

### New Features
* Support broker server address set by sdk options. [ANDROID-9326](https://jira.telenav.com:8443/browse/ANDROID-9326)
* DataCollector schema update. [TASDK-39930](https://jira.telenav.com:8443/browse/TASDK-39930)
* Expose lane guidance step info. [ANDROID-9426](https://jira.telenav.com:8443/browse/ANDROID-9426)

### Enhancements
* DataCollector remove trip score rating limitation. [TASDK-40067](https://jira.telenav.com:8443/browse/TASDK-40067)

### Bug Fixes
* Fix LocationListener.onStatusChanged incompatible. [ANDROID-9357](https://jira.telenav.com:8443/browse/ANDROID-9357)
* Fixed map render mode inconsistent with atlas engine. [ANDROID-8167](https://jira.telenav.com:8443/browse/ANDROID-8167)
* Fixed TASDK init failed. [ANDROID-9286](https://jira.telenav.com:8443/browse/ANDROID-9286)
* Avoid switching threads in SettingChangeListener. [ANDROID-8687](https://jira.telenav.com:8443/browse/ANDROID-8687)

## **Version 3.4.0-LTS4-RC3.3**
2024-02-26
### Breaking Changes
N/A

### New Features
N/A

### Enhancements
N/A

### Bug Fixes
* Fix some issues that intersection view not render correctly. [ANDROID-9326](https://jira.telenav.com:8443/browse/ANDROID-9326)

## **Version 3.4.0-LTS4-RC3.2**
2024-02-22
### Breaking Changes
N/A

### New Features
N/A

### Enhancements
N/A

### Bug Fixes
* Fix the issue of intersection view not render. [ANDROID-9326](https://jira.telenav.com:8443/browse/ANDROID-9326)


## **Version 3.4.0-LTS4-RC3.1**
2024-02-05
### Breaking Changes
N/A

### New Features
* Support load data plugin when 'extractNativeLibs' sets to false. [ANDROID-9201](https://jira.telenav.com:8443/browse/ANDROID-9201)

### Enhancements
* Support control the position of the scale bar. [ANDROID-9079](https://jira.telenav.com:8443/browse/ANDROID-9079)
* Improve map display log output. [ANDROID-9319](https://jira.telenav.com:8443/browse/ANDROID-9319)
* Improve the order of triggering arrival and depart callback. [ANDROID-9338](https://jira.telenav.com:8443/browse/ANDROID-9338)

### Bug Fixes
* Fix the issue of search poi could not be removed in a multi-threaded environment. [ANDROID-9239](https://jira.telenav.com:8443/browse/ANDROID-9239)

## **Version 3.4.0-LTS4-RC2.2**
2024-01-24
### Breaking Changes
N/A

### New Features
N/A

### Enhancements
N/A

### Bug Fixes
* Fixed an issue in map content that lane geometry generation may cause crash when processing lanes with empty boundaries. [TASDK-39573](https://jira.telenav.com:8443/browse/TASDK-39573)

## **Version 3.4.0-LTS4-RC2.1**
2024-01-22
### Breaking Changes
* Update v4 trip score event,remove score type. [TASDK-39247](https://jira.telenav.com:8443/browse/TASDK-39247)
* Expose depart waypoint signal. [ANDROID-9025](https://jira.telenav.com:8443/browse/ANDROID-9025)

### New Features
* Pass firmware_version into c++ for network usage event. [ANDROID-8962](https://jira.telenav.com:8443/browse/ANDROID-8962)
* Expose One Data Package setting to NavSDKOptions. [ANDROID-9075](https://jira.telenav.com:8443/browse/ANDROID-9075)
* Update action in promotion event. [ANDROID-9165](https://jira.telenav.com:8443/browse/ANDROID-9165)

### Enhancements
* Correct the logic for trip id with trip start/end. [ANDROID-9152](https://jira.telenav.com:8443/browse/ANDROID-9152)

### Bug Fixes
N/A

## **Version 3.4.0-LTS4-RC1.1**
2023-12-27
### Breaking Changes
* Replace TrafficIncidentResults with TrafficIncident. [ANDROID-8958](https://jira.telenav.com:8443/browse/ANDROID-8958)
* Rename RESUME_EV_ROUTE to RESUME_EV_TRIP_PLAN in RouteUpdateReason, Add new reason: UPDATE_EV_TRIP_PLAN. [ANDROID-8738](https://jira.telenav.com:8443/browse/ANDROID-8738)

### New Features
* Supply safety meta data. [ANDROID-8748](https://jira.telenav.com:8443/browse/ANDROID-8748)
* Update schema for V4 event PromotionTriggerValues. [ANDROID-9011](https://jira.telenav.com:8443/browse/ANDROID-9011)
* Expose chargingDuration in ArrivalBatteryUpdateInfo. [ANDROID-9050](https://jira.telenav.com:8443/browse/ANDROID-9050)

### Enhancements
* Remove max onboard route count config. [ANDROID-8921](https://jira.telenav.com:8443/browse/ANDROID-8921)

### Bug Fixes
N/A

## **Version 3.4.0-RC1**
2023-11-30
### Breaking Changes
* Expose new map status when surface size changed. [ANDROID-8520](https://jira.telenav.com:8443/browse/ANDROID-8520)
* Move user position from Alert to PE. [ANDROID-8685](https://jira.telenav.com:8443/browse/ANDROID-8685)

### New Features
* Add new better route updating reason for resume EV trip planner. [ANDROID-8267](https://jira.telenav.com:8443/browse/ANDROID-8267)
* Expose traffic incident and flow in AlertItem. [ANDROID-8805](https://jira.telenav.com:8443/browse/ANDROID-8805)
* Expose new API to control the traffic light count prompt. [ANDROID-8571](https://jira.telenav.com:8443/browse/ANDROID-8571)

### Enhancements
* Add ‘evMaxChargingPower’ to EnergyProfile. [ANDROID-8513](https://jira.telenav.com:8443/browse/ANDROID-8513)
* Move the DriveSession listener notify method to a new single thread. [ANDROID-7643](https://jira.telenav.com:8443/browse/ANDROID-7643)
* Add new API in VehicleInfoProvider to improve the behavior of cvp arrival logic. [ANDROID-8755](https://jira.telenav.com:8443/browse/ANDROID-8755)
* Increasing the threshold to 400ms between gesture: 'click' and 'long press'. [ANDROID-8016](https://jira.telenav.com:8443/browse/ANDROID-8016)

### Bug Fixes
* Fix the bug that the callback of task runAsync method is never triggered. [ANDROID-8758](https://jira.telenav.com:8443/browse/ANDROID-8758)
* Fix no road name and icon show in map. [ANDROID-8769](https://jira.telenav.com:8443/browse/ANDROID-8769)

## **Version 3.3.0-RC1**
2023-11-08

### Breaking Changes
* Expose hybrid routing and EV trip planning in drive session. [ANDROID-8695](https://jira.telenav.com:8443/browse/ANDROID-8695)

### New Features
* Expose API to change the dynamic native library path. [ANDROID-8608](https://jira.telenav.com:8443/browse/ANDROID-8608)
* Expose max zoom limit API in Atlas. [ANDROID-8682](https://jira.telenav.com:8443/browse/ANDROID-8682)

### Enhancements
N/A

### Bug Fixes
* Fix AutoZoomDebugInfo crash in multi threading environment. [ANDROID-8710](https://jira.telenav.com:8443/browse/ANDROID-8710)

## **Version 3.2.0-RC2**
2023-10-20

### Breaking Changes
* Change the signpost branch/towards name from list to item. [ANDROID-8494](https://jira.telenav.com:8443/browse/ANDROID-8494)
* Do not throw the exception if the createRerouteTask returns null. [ANDROID-8488](https://jira.telenav.com:8443/browse/ANDROID-8488)
* Provide onboard route for EV when there's no network connection. [ANDROID-8266](https://jira.telenav.com:8443/browse/ANDROID-8266)

### New Features
* Expose parking lot attribute of current road. [ANDROID-8300](https://jira.telenav.com:8443/browse/ANDROID-8300)
* Add new field "promptType" in AudioInstruction. [ANDROID-8512](https://jira.telenav.com:8443/browse/ANDROID-8512)
* Add new field timeZoneInfo and arrivalBattery to TravelEstimation. [ANDROID-7989](https://jira.telenav.com:8443/browse/ANDROID-7989)
* Add Intersection approaching alert and audio. [ANDROID-8522](https://jira.telenav.com:8443/browse/ANDROID-8522)
* Add a new API to enable/disable simplify audio third guidance. [ANDROID-8511](https://jira.telenav.com:8443/browse/ANDROID-8511)

### Enhancements
* Add vehicle live speed into consideration in CVP self-propelling. [ANDROID-8069](https://jira.telenav.com:8443/browse/ANDROID-8069)
* Support setting route count for ev request. [ANDROID-8464](https://jira.telenav.com:8443/browse/ANDROID-8464)
* Make some low-frequency and critical path log info level on android layer. [ANDROID-8550](https://jira.telenav.com:8443/browse/ANDROID-8550)

### Bug Fixes
* Fix touch up event did not callback when moving a short distance on map. [ANDROID-8214](https://jira.telenav.com:8443/browse/ANDROID-8214)
* Handle surface unexpected be destroyed when application in foreground scenario. [ANDROID-8278](https://jira.telenav.com:8443/browse/ANDROID-8278)

## **Version 3.1.0-RC1**
2023-09-19

### Breaking Changes
* Move setRegion method from NavSDKOptions to SDKOptions. [ANDROID-7940](https://jira.telenav.com:8443/browse/ANDROID-7940)
* Change destination and waypoints during navigation. [ANDROID-7791](https://jira.telenav.com:8443/browse/ANDROID-7791)
* Remove field in feedback info which should not been there. [ANDROID-8031](https://jira.telenav.com:8443/browse/ANDROID-8031)
* Still return route when traffic block. [ANDROID-8024](https://jira.telenav.com:8443/browse/ANDROID-8024)
* 'showRegionForRoutes' api improvement. [ANDROID-8060](https://jira.telenav.com:8443/browse/ANDROID-8060)
* Provide proposal accept result to user. [ANDROID-8120](https://jira.telenav.com:8443/browse/ANDROID-8120)
* Public annotation api improvement. [ANDROID-7798](https://jira.telenav.com:8443/browse/ANDROID-7798)
* Remove school sign alert type and sharp turn type. [ANDROID-8270](https://jira.telenav.com:8443/browse/ANDROID-8270)
* Remove deprecated fields in MMFeedbackInfo. [ANDROID-8280](https://jira.telenav.com:8443/browse/ANDROID-8280)
* Move old EV error code into Route warning message. [ANDROID-8307](https://jira.telenav.com:8443/browse/ANDROID-8307)

### New Features
* Add Tightened speed limit alert. [ANDROID-7966](https://jira.telenav.com:8443/browse/ANDROID-7966)
* Expose SafetyScore in the route. [ANDROID-7993](https://jira.telenav.com:8443/browse/ANDROID-7993)
* Integrate Intersection Guidance Info From Drivesession. [ANDROID-8184](https://jira.telenav.com:8443/browse/ANDROID-8184)
* Support rendering intersection view. [ANDROID-8229](https://jira.telenav.com:8443/browse/ANDROID-8184)

### Enhancements
* AutoZoom Config UI improvements. [ANDROID-7534](https://jira.telenav.com:8443/browse/ANDROID-7534)
* Inverted texture coordinates from original bottom-left to top-left. [ANDROID-7462](https://jira.telenav.com:8443/browse/ANDROID-7462)
* SingleClick callback sequence improvement. [ANDROID-7755](https://jira.telenav.com:8443/browse/ANDROID-7755)

### Bug Fixes
* Fix the timed speed limit not updated bug. [ANDROID-7949](https://jira.telenav.com:8443/browse/ANDROID-7949)
* Stop sending navigation event after stopNavigation. [ANDROID-7874](https://jira.telenav.com:8443/browse/ANDROID-7874)
* Handle popup mapView scenario lifecycle. [ANDROID-7906](https://jira.telenav.com:8443/browse/ANDROID-7906)
* Fix mapview container memory leak. [ANDROID-8053](https://jira.telenav.com:8443/browse/ANDROID-8053)
* Fix onNavigationRouteUpdating not been triggered bug. [ANDROID-8085](https://jira.telenav.com:8443/browse/ANDROID-8085)
* Fix Korean country code map error. [ANDROID-8216](https://jira.telenav.com:8443/browse/ANDROID-8216)
* Fix return ADAS message twice. [ANDROID-8269](https://jira.telenav.com:8443/browse/ANDROID-8269)

## **Version 3.0.0-RC2**
2023-06-16

### Breaking Changes
N/A

### New Features
N/A

### Enhancements
N/A

### Bug Fixes
* Fix UNDEFINED sub status after a successful download. [TASDK-34007](https://jira.telenav.com:8443/browse/TASDK-34007)


## **Version 3.0.0-RC1**
2023-06-13

### Breaking Changes
* Add turn by turn information updated callback. [ANDROID-7629](https://jira.telenav.com:8443/browse/ANDROID-7629)
* Remove deprecated method: onNavigationRouteUpdated. [ANDROID-7669](https://jira.telenav.com:8443/browse/ANDROID-7669)
* Expose charging station property. [ANDROID-7197](https://jira.telenav.com:8443/browse/ANDROID-7197)
* Still return route with traffic block for DRG. [ANDROID-7871](https://jira.telenav.com:8443/browse/ANDROID-7871)
* Provide sub-region OTA update feature. [ANDROID-6750](https://jira.telenav.com:8443/browse/ANDROID-6750)
* Datacollector supports uploading events control in the runtime according to SDKRuntime. [ANDROID-6527](https://jira.telenav.com:8443/browse/ANDROID-6527)

### New Features
* Expose red light speed camera audioType enum. [ANDROID-7675](https://jira.telenav.com:8443/browse/ANDROID-7675)
* Expose JunctionView status. [ANDROID-7737](https://jira.telenav.com:8443/browse/ANDROID-7737)

### Enhancements
* Set the maximum number of routes calculated by onboard map data. [ANDROID-6768](https://jira.telenav.com:8443/browse/ANDROID-6768)
* Handle short maneuverLength situation to make the autozoom more smooth. [ANDROID-7603](https://jira.telenav.com:8443/browse/ANDROID-7603)

### Bug Fixes
* Fix autozoom transition to 2D mode when in highway. [ANDROID-7120](https://jira.telenav.com:8443/browse/ANDROID-7120)
* Fix the invalid route issue and audio decode issue. [ANDROID-6276](https://jira.telenav.com:8443/browse/ANDROID-6276)
* Fix Arabic audio text error. [ANDROID-7814](https://jira.telenav.com:8443/browse/ANDROID-7814)
* Fix zoom map mode went to 2D accidentally. [ANDROID-7818](https://jira.telenav.com:8443/browse/ANDROID-7818)

## **Version 2.10.0-lts3-rc8.1**
2023-03-23

### Breaking Changes
N/A

### New Features
* Support outer polygon to represent EV range. [ANDROID-7207](https://jira.telenav.com:8443/browse/ANDROID-7207)
* Enable stakeholders to configure AutoZoom LADs configuration at runtime. [ANDROID-6715](https://jira.telenav.com:8443/browse/ANDROID-6715)

### Enhancements
* Memory performance improvements in the native layer.

### Bug Fixes
* Fix the bug that map not rendered due to EGL_BAD_SURFACE. [ANDROID-7386](https://jira.telenav.com:8443/browse/ANDROID-7386)
* Fix the bug that the better route accept fail. [ANDROID-7448](https://jira.telenav.com:8443/browse/ANDROID-7448)


## **Version 2.10.0-lts3-rc7.3**
2023-03-10

### Breaking Changes
N/A

### New Features
N/A

### Enhancements
N/A

### Bug Fixes
* CVP missing when shapes removed. [ANDROID-7375](https://jira.telenav.com:8443/browse/ANDROID-7375)
* Fix a performance issue in native layer.

## **Version 2.10.0-lts3-rc7.2**
2023-03-06

### Breaking Changes
N/A

### New Features
* Add new API 'showRegionForModelInstance' in CameraController to snap to polygon. [ANDROID-6845](https://jira.telenav.com:8443/browse/ANDROID-6645)
* Apply avoid options in EV routing when auto planning is set to off. [ANDROID-7168](https://jira.telenav.com:8443/browse/ANDROID-7168)

### Enhancements
* Set the value of 'ChargingPlan' in 'TravelPoint' nullable. [ANDROID-7149](https://jira.telenav.com:8443/browse/ANDROID-7149)
* Notify touch event handlers about Up events in case of single tap. [ANDROID-7164](https://jira.telenav.com:8443/browse/ANDROID-7164)
* Autozoom updates. [ANDROID-6843](https://jira.telenav.com:8443/browse/ANDROID-6843)

### Bug Fixes
* Fix the bug that the savedTime in BetterRouteProposal is null. [ANDROID-7260](https://jira.telenav.com:8443/browse/ANDROID-7260)
* Fix the bug that the zoneInfo in school zone alert is null. [ANDROID-7081](https://jira.telenav.com:8443/browse/ANDROID-7081)
* Change the unit of utcTime from second to millisecond. [ANDROID-6162](https://jira.telenav.com:8443/browse/ANDROID-6162)
* Equals method in GeoLocation always return false. [ANDROID-7292](https://jira.telenav.com:8443/browse/ANDROID-7292)


## **Version 2.10.0-lts3-rc6.1**
2023-02-08

### Breaking Changes
* Add navigation coordinate in the GeoLocation. [ANDROID-7105](https://jira.telenav.com:8443/browse/ANDROID-7105)

### New Features
* Add new API 'setFrameThrottlingEnabled' in MapView to improve CPU consumption figures. [ANDROID-6627](https://jira.telenav.com:8443/browse/ANDROID-6627)

### Enhancements
* Keep original cloud route traffic on the rest of the Route. [ANDROID-6154](https://jira.telenav.com:8443/browse/ANDROID-6154)
* Add engine initialization failure handling and provide user feedback. [ANDROID-3867](https://jira.telenav.com:8443/browse/ANDROID-3867)
* Provide direction error code when the charging station cannot fully planned. [ANDROID-7070](https://jira.telenav.com:8443/browse/ANDROID-7070)
* Exposed Obstructed Region Apis. [ANDROID-7088](https://jira.telenav.com:8443/browse/ANDROID-7088)
* Deprecate 'onNavigationRouteUpdated'. [ANDROID-7177](https://jira.telenav.com:8443/browse/ANDROID-7177)
* Memory performance improvements. [ANDROID-6589](https://jira.telenav.com:8443/browse/ANDROID-6589)

### Bug Fixes
* Fix POI not removed sometimes after day/night mode changed. [ANDROID-7042](https://jira.telenav.com:8443/browse/ANDROID-7042)


## **Version 2.10.0-lts3-rc5.1**
2023-01-12

### Breaking Changes
* Move reading client passed analytics object from systemOptions to sdkOptions. [ANDROID-6981](https://jira.telenav.com:8443/browse/ANDROID-6981)
* Expose routing progress from DRG. [ANDROID-6955](https://jira.telenav.com:8443/browse/ANDROID-6955)
* Add better route for station unavailable reason and expose api settings for triggering the event. [ANDROID-7032](https://jira.telenav.com:8443/browse/ANDROID-7032)
* Expose road class in the ManeuverInfo. [ANDROID-6932](https://jira.telenav.com:8443/browse/ANDROID-6932)
* Integrate new avoidStep and avoidIncident feature. [ANDROID-6051](https://jira.telenav.com:8443/browse/ANDROID-6051)

### New Features
* Add double click callbacks. [ANDROID-6307](https://jira.telenav.com:8443/browse/ANDROID-6307)

### Enhancements
* Remove hardcoded name of CVP. [ANDROID-6176](https://jira.telenav.com:8443/browse/ANDROID-6176)
* Simple touch listener should not be invoked if annotation listener was already invoked.[ANDROID-6776](https://jira.telenav.com:8443/browse/ANDROID-6776)
* GetNearest API improvement to avoid ANR .[ANDROID-6831](https://jira.telenav.com:8443/browse/ANDROID-6831)


## **Version 2.10.0-lts3-rc4.1**
2022-12-15

### Breaking Changes
* Deprecate old APIs for alert prompt controlling while adding new APIs. [ANDROID-6933](https://jira.telenav.com:8443/browse/ANDROID-6933)

### New Features
* Support SA, ISC, PAK region. [ANDROID-6119](https://jira.telenav.com:8443/browse/ANDROID-6119)
* Support EV isochrone. [ANDROID-6690](https://jira.telenav.com:8443/browse/ANDROID-6690)

### Enhancements
* Double-tapping the screen with one finger should zoom in, not select a route. [ANDROID-6684](https://jira.telenav.com:8443/browse/ANDROID-6684)
* Format GLTask thread name. [ANDROID-6818](https://jira.telenav.com:8443/browse/ANDROID-6818)
* Expose enable/disable free flow traffic API. [ANDROID-6781](https://jira.telenav.com:8443/browse/ANDROID-6781)
* Add direction to edge id. [ANDROID-6721](https://jira.telenav.com:8443/browse/ANDROID-6721)

### Bug Fixes
* Fix crash when receiving map version update. [ANDROID-6913](https://jira.telenav.com:8443/browse/ANDROID-6913)


## **Version 2.10.0-lts3-rc3.3**
2022-11-29

### Breaking Changes
N/A

### New Features
N/A

### Enhancements
N/A

### Bug Fixes
* Fixed an issue in alert service that missing country code info when call renderIcon in drive session will cause crash. [ANDROID-6860](https://jira.telenav.com:8443/browse/ANDROID-6860)
* Fixed an issue in graph service that native memory increases quickly causing crash. [ANDROID-6280](https://jira.telenav.com:8443/browse/ANDROID-6280)

### Dependencies:
* Update NavCore to 0.14.0-lts3-rc3.2


## **Version 2.10.0-lts3-rc3.2**
2022-11-23

### Breaking Changes
* Bump Kotlin version to 1.6.10. [ANDROID-6683](https://jira.telenav.com:8443/browse/ANDROID-6683)

### New Features
* Merge set network API to LTS3. [ANDROID-6767](https://jira.telenav.com:8443/browse/ANDROID-6767)
* Expose AutoZoom on/off API. [ANDROID-6696](https://jira.telenav.com:8443/browse/ANDROID-6696)
* Add new event type: RewardFeatureInteraction (DataCollector). [TASDK-32869](https://jira.telenav.com:8443/browse/TASDK-32869)

### Enhancements
* Override UserGraphic construct to avoid bitmap copy. [ANDROID-6576](https://jira.telenav.com:8443/browse/ANDROID-6576)
* Annotations shows too slowly with about 6 seconds delay. [ANDROID-6125](https://jira.telenav.com:8443/browse/ANDROID-6125)
* Add new API to support zoom in/out with transition time. [ANDROID-6631](https://jira.telenav.com:8443/browse/ANDROID-6631)
* Remove obsolete parts of GetDefaultAnnotationParams. [ANDROID-6716](https://jira.telenav.com:8443/browse/ANDROID-6716)
* Check streaming download path permission at init stage. [ANDROID-6712](https://jira.telenav.com:8443/browse/ANDROID-6712)

### Bug Fixes
* ShowRoutesAndPointsInRegion Could Not Show Points on Region. [ANDROID-6470](https://jira.telenav.com:8443/browse/ANDROID-6470)
* The underlying POI ICON be clicked when user try to click the RGC bubble. [ANDROID-5482](https://jira.telenav.com:8443/browse/ANDROID-5482)
* Map black screen shows when switch from background to foreground. [ANDROID-6432](https://jira.telenav.com:8443/browse/ANDROID-6432)
* Fix the bug that calling abstract method in DefaultAndroidLocationProvider. [ANDROID-6774](https://jira.telenav.com:8443/browse/ANDROID-6774)
* Fix 2D mode stuck on highway. [ANDROID-6696](https://jira.telenav.com:8443/browse/ANDROID-6696)

### Dependencies:
* Update NavCore to 0.14.0-lts3-rc3.1
* Update AdmClient to 0.10.1-lts3-rc3.1
* Update Entity to v0.11.0-lts3-rc3.1
* Update DataCollector to v0.10.0-lts3-rc3.1
* Update Foundation to v4.0.0


## **Version 2.10.0-lts3-rc2.1**
2022-11-02

### Breaking Changes
N/A

### New Features
N/A

### Enhancements
* MapView instances reference count and CVP position updates per view. [ANDROID-6422](https://jira.telenav.com:8443/browse/ANDROID-6422)

### Bug Fixes
* Drg callback is not triggered. [ANDROID-6536](https://jira.telenav.com:8443/browse/ANDROID-6536)
* Fix crash on MapView exit. [ANDROID-6442](https://jira.telenav.com:8443/browse/ANDROID-6442)

### Dependencies:
* Update NavCore to 0.14.0-lts3-rc2.1
* Update AdmClient to 0.10.1-lts3-rc2.1
* Update Entity to v0.11.0-lts3-rc2.1
* Update DataCollector to v0.10.0-lts3-rc2.1
* Update Foundation to v3.0.1


## **Version 2.10.0-lts3-rc1.4**
2022-10-27

### Breaking Changes
Downgrade kotlin to v1.5.32

### New Features
N/A

### Enhancements
N/A

### Bug Fixes
N/A

### Dependencies:
* Update NavCore to 0.14.0-lts3-rc1.2

## **Version 2.10.0-lts3-rc1.1**
2022-10-17

### Breaking Changes
* Add lane guidance info in both AGV and IGV modes. [ANDROID-5293](https://jira.telenav.com:8443/browse/ANDROID-5293)
* Add language tag on Audio result. [ANDROID-6352](https://jira.telenav.com:8443/browse/ANDROID-6352)
* Traffic display improvement. [ANDROID-6196](https://jira.telenav.com:8443/browse/ANDROID-6196)

### New Features
N/A

### Enhancements
* Expose API to enable/disable alert detection. [ANDROID-5293](https://jira.telenav.com:8443/browse/ANDROID-5293)
* Implement MapView instances reference count. [ANDROID-6422](https://jira.telenav.com:8443/browse/ANDROID-6422)
* Throw exceptions when start navigation fail. [ANDROID-6127](https://jira.telenav.com:8443/browse/ANDROID-6127)

### Bug Fixes
* Construction icon is displaying as road closure on map. [ANDROID-5150](https://jira.telenav.com:8443/browse/ANDROID-5150)
* RGC pin become huge sometimes. [ANDROID-6064](https://jira.telenav.com:8443/browse/ANDROID-6064)
* Concurrent exception is thrown out sometimes when calling api SearchController().displayPOI(). [ANDROID-5939](https://jira.telenav.com:8443/browse/ANDROID-5939)

### Dependencies:
* Update NavCore to 0.14.0-lts3-rc1.1
* Update AdmClient to 0.10.1-lts3-rc1.1
* Update Entity to v0.11.0-lts3-rc1.1
* Update DataCollector to v0.10.0-lts3-rc1.1

## **Version 2.10.0-rc.1**
2022-09-20

### Breaking Changes
* New API is added for HMI to query shield icon bitmap with context. [ANDROID-6084](https://jira.telenav.com:8443/browse/ANDROID-6084)
* Location provider changes. [ANDROID-6189](https://jira.telenav.com:8443/browse/ANDROID-6189)
* Unify the data model for Waypoint definition. [ANDROID-6060](https://jira.telenav.com:8443/browse/ANDROID-6060)

### New Features
N/A

### Enhancements
* First time launch no GPS default location experience improvement. [ANDROID-5905](https://jira.telenav.com:8443/browse/ANDROID-5905)
* Unified tasdk-android thread name. [ANDROID-6256](https://jira.telenav.com:8443/browse/ANDROID-6256)

### Bug Fixes
* SharedPreferences not available until after user is unlocked. [ANDROID-6173](https://jira.telenav.com:8443/browse/ANDROID-6173)
* Set location in MapViewInitConfig does not take effect when no GPS. [ANDROID-6276](https://jira.telenav.com:8443/browse/ANDROID-6276)
* Fix follow vehicle mode switch. [ANDROID-6081](https://jira.telenav.com:8443/browse/ANDROID-6081)

### Dependencies:
* Update NavCore to v0.14.0-rc.1
* Update AdmClient to v0.10.0-rc.1
* Update Foundation to v2.3.1
* Update Entity to v0.11.0-rc.1
* Update DataCollector to v0.10.0-rc.1
* Update SDK-Base to v2.0.4

## **Version 2.9.0**
2022-09-16

## Finalize [ANDROID-6089](https://jira.telenav.com:8443/browse/ANDROID-6089)

## **Version 2.9.0-rc.1**
2022-08-30

### Breaking Changes
N/A

### New Features
* DRG for EV planner and unreachable charging station event. [ANDROID-5433](https://jira.telenav.com:8443/browse/ANDROID-4839) [ANDROID-5232](https://jira.telenav.com:8443/browse/ANDROID-5232)
* Support HDMap switch. [ANDROID-4431](https://jira.telenav.com:8443/browse/ANDROID-4431)

### Enhancements
* Delete default analytics implementation. [ANDROID-5912](https://jira.telenav.com:8443/browse/ANDROID-5912)
* Downgrade targetSdkVersion from 31 to 30. [ANDROID-6002](https://jira.telenav.com:8443/browse/ANDROID-6002)
* Support jni log level setting. [ANDROID-6104](https://jira.telenav.com:8443/browse/ANDROID-6104)
* Use HybridDirectionClient instead of EvtripClient for EV route task. [ANDROID-6038](https://jira.telenav.com:8443/browse/ANDROID-6038)
* Improve annotations update API. [ANDROID-3830](https://jira.telenav.com:8443/browse/ANDROID-3830)

### Bug Fixes
* Rotate screen during navigation，delay displaying the current recenter view. [ANDROID-5219](https://jira.telenav.com:8443/browse/ANDROID-5219)
* School zone missed. [ANDROID-5953](https://jira.telenav.com:8443/browse/ANDROID-5953)
* Compass can not be enabled by getFeaturesController().compass().setEnabled(). [ANDROID-5944](https://jira.telenav.com:8443/browse/ANDROID-5944)
* Fix simulation speed and maneuver list issue.[ANDROID-5989](https://jira.telenav.com:8443/browse/ANDROID-5944) [ANDROID-5997](https://jira.telenav.com:8443/browse/ANDROID-5997)

### Dependencies:
* Update NavCore to v0.13.0-rc.2
* Update Foundation to v2.2.0-rc.1
* Update Entity to v0.10.0-rc.1
* Update DataCollector to v0.9.0-rc.1

## **Version 2.8.0**
2022-08-22

## Finalize [ANDROID-5919](https://jira.telenav.com:8443/browse/ANDROID-5919)

## **Version 2.8.0-rc.1**
2022-08-09

### Breaking Changes
* Add TnAAOSMapView. [ANDROID-5089](https://jira.telenav.com:8443/browse/ANDROID-5089)
* Integrate new EV Direction related interfaces. [ANDROID-5220](https://jira.telenav.com:8443/browse/ANDROID-5220)
* Integrate road segment reference. [ANDROID-5689](https://jira.telenav.com:8443/browse/ANDROID-5689)
* Drivesession refactor. [ANDROID-5570](https://jira.telenav.com:8443/browse/ANDROID-5570)

### New Features
* Add UpdateAnnotationState to AnnotationController API. [ANDROID-4839](https://jira.telenav.com:8443/browse/ANDROID-4839)
* Add country code in route step. [ANDROID-5547](https://jira.telenav.com:8443/browse/ANDROID-5547)
* Add road type in the RoadEdge. [ANDROID-5546](https://jira.telenav.com:8443/browse/ANDROID-5546)
* Set a custom thread name to backgroundExecutorService of SDKImplement. [ANDROID-5761](https://jira.telenav.com:8443/browse/ANDROID-5761)
* Support different log level can be set by topic. [ANDROID-5792](https://jira.telenav.com:8443/browse/ANDROID-5792)
* Add map version update listener. [ANDROID-4495](https://jira.telenav.com:8443/browse/ANDROID-4495)
* Search rightly charge station. [ANDROID-5438](https://jira.telenav.com:8443/browse/ANDROID-5438)
* Support analytics through navSDK options. [ANDROID-5844](https://jira.telenav.com:8443/browse/ANDROID-5844)

### Bug Fixes
* Fix exception thrown on GLWorkerThread. [ANDROID-5602](https://jira.telenav.com:8443/browse/ANDROID-5602)
* Throw exception when cloudRouting disabled in cloud only mode. [ANDROID-5296](https://jira.telenav.com:8443/browse/ANDROID-5296)
* Duplicate data in annotation touch listener. [ANDROID-5593](https://jira.telenav.com:8443/browse/ANDROID-5693)
* Previous rgc button can't disappear when press another one. [ANDROID-5734](https://jira.telenav.com:8443/browse/ANDROID-5734)
* cvp autozoom fix. [ANDROID-5344](https://jira.telenav.com:8443/browse/ANDROID-5344)

## **Version 2.7.0**
2022-08-09

## Finalize [ANDROID-5663](https://jira.telenav.com:8443/browse/ANDROID-5663)

## **Version 2.7.0-rc.2**
2022-07-29

### Bug Fixes
* Parameters of vehicleInfoProvider can be nullable. [ANDROID-5799](https://jira.telenav.com:8443/browse/ANDROID-5799)

### Dependencies:
* Update NavCore to v0.11.0

## **Version 2.7.0-rc.1**
2022-07-19

### Breaking Changes
* Expose interface to frontend to independently set cluster style. [ANDROID-5282](https://jira.telenav.com:8443/browse/ANDROID-4978)
* Use VehicleInfoProvider instead of VehicleProfile. [ANDROID-5282](https://jira.telenav.com:8443/browse/ANDROID-4978)
* TnMapView refactoring. [ANDROID-5088](https://jira.telenav.com:8443/browse/ANDROID-5088)

### New Features
* Add hva and traffic light type. [ANDROID-5475](https://jira.telenav.com:8443/browse/ANDROID-5475)
* Notify the current render mode when tilt gesture ends. [ANDROID-1945](https://jira.telenav.com:8443/browse/ANDROID-1945)
* Reset user set declination when Map Orientation is changed. [ANDROID-4725](https://jira.telenav.com:8443/browse/ANDROID-4725)
* Expose a new interface to reveal update size estimation (OTA). [ANDROID-5120](https://jira.telenav.com:8443/browse/ANDROID-5120)

### Bug Fixes
* Map display incompletely with red color base map. [ANDROID-5141](https://jira.telenav.com:8443/browse/ANDROID-5141)
* Create route annotation failed with bubble types that are not simple or default. [ANDROID-5454](https://jira.telenav.com:8443/browse/ANDROID-5454)
* Throw exception when routing with CLOUD_ONLY mode while cloud routing is disabled. [ANDROID-5296](https://jira.telenav.com:8443/browse/ANDROID-5296)

### Dependencies:
* Update NavCore to v0.11.0-rc.1
* Update Entity to v0.8.0-rc.1

## **Version 2.6.0-rc.2**
2022-07-05

### Bug Fixes
* Crash was got when running testPositionEventListenerTimeCallbackInDemonNav. [ANDROID-5459](https://jira.telenav.com:8443/browse/ANDROID-5459)

### Dependencies:
* Update NavCore to v0.9.2

## **Version 2.6.0-rc.1**
2022-06-28

### New Features
* Add API to disable prompt navigation state. [ANDROID-4978](https://jira.telenav.com:8443/browse/ANDROID-4978)
* Expose configuration file root path to client. [ANDROID-5062](https://jira.telenav.com:8443/browse/ANDROID-5062)
* Use elapsed real time if no drTime is set by client. [ANDROID-5241](https://jira.telenav.com:8443/browse/ANDROID-5241)
* Add isPreferred and applicableDrivingDirection fields in the LaneInfo. [ANDROID-4361](https://jira.telenav.com:8443/browse/ANDROID-4361)
* Add TnBaseMapView. [ANDROID-5087](https://jira.telenav.com:8443/browse/ANDROID-5087)
* Expose interface to frontend to independently set cluster style. [ANDROID-5282](https://jira.telenav.com:8443/browse/ANDROID-5282)
* Optimize "start navigation" method by moving method to work thread. [ANDROID-5354](https://jira.telenav.com:8443/browse/ANDROID-5354)
* Support different verbosity level in audio guidance. [ANDROID-5335](https://jira.telenav.com:8443/browse/ANDROID-5335)
* Expose prompt style to AudioInstruction. [ANDROID-5377](https://jira.telenav.com:8443/browse/ANDROID-5377)
* Provide an interface to adjust the threshold of waypoint arrival distance. [ANDROID-4851](https://jira.telenav.com:8443/browse/ANDROID-4851)

### Bug Fixes
* Crash when loop startNavigation and stopNavigation. [ANDROID-5300](https://jira.telenav.com:8443/browse/ANDROID-5300)
* Set bearing API was not functional. [ANDROID-4795](https://jira.telenav.com:8443/browse/ANDROID-4795)

### Dependencies:
* Update NavCore to v0.9.1

## **Version 2.5.0-rc.2**
2022-06-15

### Bug Fixes
* No traffic display on base map under pure streaming mode. [ANDROID-5254](https://jira.telenav.com:8443/browse/ANDROID-5254)

### Dependencies:
* Update NavCore to v0.9.0-rc3

## **Version 2.5.0-rc.1**
2022-06-08

### New Features
* Make the cvp a tappable annotation. [ANDROID-4699](https://jira.telenav.com:8443/browse/ANDROID-4699)
* "POITouchListener" should not contain annotation element. [ANDROID-4923](https://jira.telenav.com:8443/browse/ANDROID-4923)
* Implement autozoom on route steps API. [ANDROID-4105](https://jira.telenav.com:8443/browse/ANDROID-4105)
* System instance can be created with vehicle profile update. [ANDROID-4927](https://jira.telenav.com:8443/browse/ANDROID-4927)
* Correct the name and usages of the get features mask method in MapViewReadyListener. [ANDROID-4788](https://jira.telenav.com:8443/browse/ANDROID-4788)
* TnMapView initialize api improvement. [ANDROID-4762](https://jira.telenav.com:8443/browse/ANDROID-4762)
* Expose MM location information through location bundle. [ANDROID-5177](https://jira.telenav.com:8443/browse/ANDROID-5177)

### Bug Fixes
* No implementation found for SystemJni.updateVehicle. [ANDROID-4983](https://jira.telenav.com:8443/browse/ANDROID-4983)
* Null pointer exception by removing the redundant configuration in SDKImplement. [ANDROID-4997](https://jira.telenav.com:8443/browse/ANDROID-4997)
* Catch CancellationException in JniMessageHub. [ANDROID-4903](https://jira.telenav.com:8443/browse/ANDROID-4903)
* Disable audio failed. [ANDROID-4800](https://jira.telenav.com:8443/browse/ANDROID-4800)
* getVehicleFollowMode in cameraController always return null. [ANDROID-4797](https://jira.telenav.com:8443/browse/ANDROID-4797)
* JV display error. [ANDROID-5211](https://jira.telenav.com:8443/browse/ANDROID-5211)

### Dependencies:
* Update NavCore to v0.9.0-rc2

## **Version 2.4.0**
2022-05-24

### Dependencies:
* Update NavCore to v0.8.0-rc3
* Update AdmClient to v0.5.2
* Update DataCollector to v0.6.0
* Update EntityService to v0.6.0

## **Version 2.4.0-rc1**
2022-05-12

### New Features
* Send available satellite count into Position Engine. [ANDROID-4873](https://jira.telenav.com:8443/browse/ANDROID-4873)
* Expose shield icon API. [ANDROID-3812](https://jira.telenav.com:8443/browse/ANDROID-3812)
* Support for waypoint overview. [ANDROID-4395](https://jira.telenav.com:8443/browse/ANDROID-4395)
* Disable strip C++ lib. [ANDROID-4912](https://jira.telenav.com:8443/browse/ANDROID-4912)

### Bug Fixes
* Remaining battery level is not smart bubble and it overlaps with ete smart bubble. [ANDROID-4292](https://jira.telenav.com:8443/browse/ANDROID-4292)
* Fix missing symbols of adm client jni. [ANDROID-4822](https://jira.telenav.com:8443/browse/ANDROID-4822)
* Do the config unless sdk initialized. [ANDROID-4914](https://jira.telenav.com:8443/browse/ANDROID-4914)

### Dependencies:
* Update NavCore to v0.8.0-rc.1
* Update DataCollector to v0.6.0-rc.3
* Update EntityService to v0.6.0-rc.1

## **Version 2.3.0**
2022-05-05

### Dependencies:
* Update NavCore to v0.7.0
* Update EntityService to v0.5.2

## **Version 2.3.0-rc1**
2022-04-25

### New Features
* Add a timeoutForOldEmbeddedDataVersion configuration parameters in the HybridClientConfig. [ANDROID-4508](https://jira.telenav.com:8443/browse/ANDROID-4508)
* Support creating poisearchentity without custom graphics. [ANDROID-4066](https://jira.telenav.com:8443/browse/ANDROID-4066)
* Add analytics framework. [TASDK-27651](https://jira.telenav.com:8443/browse/TASDK-27651)

### Bug Fixes
* The location bundle won't be created if navigationEventListener is not added. [ANDROID-4647](https://jira.telenav.com:8443/browse/ANDROID-4647)
* Fix wayPoint signature mismatch. [ANDROID-4676](https://jira.telenav.com:8443/browse/ANDROID-4676)
* Catch JniMessageHub fetch task ArrayIndexOutOfBoundsException. [ANDROID-4629](https://jira.telenav.com:8443/browse/ANDROID-4629)

### Dependencies:
* Update NavCore to v0.7.0-rc.1
* Update Foundation to v1.7.0
* Update EntityService to v0.5.2-rc.1

## **Version 2.2.0-rc1**
2022-04-12

### Breaking Changes
* Auto zoom improvements. [ANDROID-4344](https://jira.telenav.com:8443/browse/ANDROID-4344)
* Disable show POI function by displayPOI with an empty list failed when numbers of POIs is large. [ANDROID-4073](https://jira.telenav.com:8443/browse/ANDROID-4073)

### New Features
* Optimization of signpost name. [ANDROID-3937](https://jira.telenav.com:8443/browse/ANDROID-3937)
* Add api to support traffic control. [ANDROID-4481](https://jira.telenav.com:8443/browse/ANDROID-4481)

### Bug Fixes
* Matched Location bundle is null before nav statues update. [ANDROID-4396](https://jira.telenav.com:8443/browse/ANDROID-4396)

### Dependencies:
* Update Base to v2.0.1
* Update NavCore to v0.6.0-rc.1
* Update AdmClient to v0.5.1
* Update EntityService to v0.5.1


## **Version 2.1.0**
2022-03-23

### Dependencies:
* Update NavCore to v0.5.0


## **Version 2.1.0-rc1**
2022-03-11

### Breaking Changes
* Optimize the urgent-x traffic data structure. [ANDROID-4237](https://jira.telenav.com:8443/browse/ANDROID-4237)

### New Features
* Enable ADAS feature. [ANDROID-4236](https://jira.telenav.com:8443/browse/ANDROID-4236)

### Bug Fixes
* u-turn have double arrow. [ANDROID-4159](https://jira.telenav.com:8443/browse/ANDROID-4159)

### Dependencies:
* Update NavCore to v0.5.0-rc.2


## **Version 2.0.1**
2022-03-08

### Dependencies:
* Update NavCore to v0.4.0


## **Version 2.0.0-rc.2**
2022-02-18

### Breaking Changes
* The parameter of UrgentEventListener.onUrgentEventUpdated() method is changed to TrafficIncidentResults list.


## **Version 1.4.19.5**
2022-02-07

* MapView:
  * Expose real reach API's in Android layer
  * Expose API for tappable map data POI's in java layer
* Direction:
  * Real reach polygon feature
* DriveSession:
  * DriveSession can be initialized with services disable
* Misc.
  * Provide a set of APIs to disable/enable all cloud services dynamically
  * General bugfixes

## **Version 1.4.18.3**
2022-01-07

* MapView:
  * Auto Zoom - Animation speed improvement
* DriveSession:
  * DriveSessionException formatting
* Misc.
  * Provide a set of APIs to disable/enable all cloud services when initialization
  * Support Android11+ storage permission requesting
  * General bugfixes

## **Version 1.4.15.6**
2021-11-08

* MapView:
  * Rewrite the AutoZoom which introduced the json file to control revolved status
  * Use physical dpi (xdpi/ydpi) instead of logical dpi (densityDpi)
* DriveSession:
  * Support exposing more error events from better route feature
* Misc.
  * Atlas configuration independence
  * General bugfixes

## **Version 1.4.14.2**
2021-10-13

* MapView:
  * Change the type of traffic incident to V2
  * Optimize the order of touched annotations
  * Implement route characteristic 'out of range'
  * Support cluster map view
  * Support getting eating route tail point
* Misc.
  * Support ANZ region
  * General bugfixes

## **Version 1.4.13.1**
2021-09-21

* MapView:
  * Support clear POI cache data
  * Support flat terrain
* DriveSession:
  * Add time attribute to location in PositionEventListener's callback
  * Support avoid step and incident
* Misc.
  * Change the default log level from INFO to WARNING
  * Support the client to set the Region when initializing the SDK
  * General bugfixes

## **Version 1.4.12.1**
2021-08-30

* MapView:
  * Optimize the map engine's display quality of route
* DriveSession:
  * Support MMFeedbackInfo
  * Expose getTimedRestrictionEdges and saveTime field when route is updated by DRG
  * Expose better route fail reason
  * Return an empty streetName if there's no road name
* Misc.
  * Keep TaLog's config when SDK init
  * General bugfixes and performance improvements

## **Version 1.4.11.7**
2021-09-03

* MapView:
  * Optimize the MapView initialize time
  * Optimize the CPU usage by GL Thread
  * NorthUp FollowVehicleMode support keeping north up after rotating
  * Support Free-Drive AutoZoom
  * Support displaying and hiding annotations under annotation layer
  * Support limit Max & Min zoom level
* Direction:
  * Replace use_traffic_info of RoutePreferences with avoid_traffic_congestion
* DriveSession:
  * Support adjusting saved time percentage dynamically
* Misc.
  * General bugfixes and CPU performance improvements

## **Version 1.4.10.1**
2021-07-20

* MapView:
  * Support enable/disable AutoZoom in both HeadingUp and NorthUp FollowVehicleMode
  * Remove enhanced FollowVehicleMode
  * Optimize annotation's setIconX and setIconY method
  * Support grouping annotation
  * Optimize AutoZoom with new API
  * Support to set FPS
  * Map style updates
* Direction:
  * Support waypoint optimize task
* DriveSession:
  * Provide better route context info in better route notification / candidate
  * Provide nearby and along route Urgent/X-Urgent event along route notification
  * Support DR feedback
* Misc.
  * Support URLs setting
  * General bugfixes

## **Version 1.4.9.6**
2021-07-01

* MapView:
  * Support show POI on map
  * Support u-turn arrow style
  * Support double-tap zoom in at the touch position
  * Support annotation update with new texture，except congestion bubble and route annotation
* Direction:
  * Provide travel points along the route, including origin, waypoints and final destination.
  * Improve routing performance for EU region
* DriveSession:
  * Support update AudioLocale
  * Expose traffic setting: SETTING_KEY_NAME_TRAFFIC_FETCH_RANGE
  * Provide along route traffic incident
  * Enable feature where am I
* Misc.
  * Expose MAP_DATA_STREAMING_SPACE_LIMIT config
  * General bugfixes

## **Version 1.4.8.2**
2021-06-08

* MapView:
  * Map style updates
  * Forbid the up and move event to trigger the route
  * Support two new traffic incident filters:traffic-incident-urgency-level and traffic-incident-blocking
* Direction:
  * Provide hybrid client configuration for timeout threshold of waiting cloud routing
  * [EV] Support Eco Curve setting
  * Support optional android location to construct GeoLocation
  * Support real reach feature in direction side.
* DriveSession:
  * Refactor AudioType in the AudioInstruction
  * Enrich ManeuverInfo
  * Expose way to specify "Info", "1st", "2nd" & "3rd" guidance
  * Traffic bar improvement
* Misc.
  * General bugfixes

## **Version 1.4.7.1**
2021-05-17

* MapView:
  * Use default map style
  * Support both default screen and off screen snapshot
* Direction:
  * Upgrade V2 route response and models
* Misc.
  * Start broker service
  * Support getting map data version and SDK version
  * General bugfixes

## **Version 1.4.5.31**
2021-04-16

* MapView:
  * Map style updates
  * Update no culling annotation style
  * ADILine support end point
  * AutoZoom only supported in FollowVehicleMode.Enhanced
  * Support 2D/3D render mode
  * Support Sub-View
* DriveSession:
  * Support prefetching data after starting navigation
* Direction:
  * Support hybrid routing mode which will choose suitable onboard / cloud service intellectually
* Misc.
  * General bugfixes and CPU performance improvements

## **Version 1.4.4.14**
2021-03-26

* MapView:
  * Support query follow vehicle mode
  * Improve usability of annotation api
  * Support create annotation with either heavy or light congestion bubble
* DriveSession:
  * Support jump road feature
  * Prompt deviation audio only once when in a continuous deviation phase
  * Prompt rerouting audio when a new route is produced through deviation
* Misc.
  * Support set unit for METRIC or IMPERIAL
  * General bugfixes and performance improvements

## **Version 1.4.2.12**
2021-01-29

* MapView:
  * Add OnTouch support for map elements
  * AutoZoom improvements for free drive and active navigation
  * Add font size configuration support
  * Traffic on route Support
  * Map style updates
* DriveSession:
  * Phoneme support for audio guidance
  * Request along route traffic info
  * Support prompting cloud congestion traffic in audio
* Direction:
  * Onboard routing support improvements
* Misc.
  * General bugfixes and performance improvements

## **Version 1.4.1.7**
2021-01-15

* MapView:
  * Annotation API to set and get ExtraInfo
  * Use night mode as default color theme
  * MapViewListener support, provide onMapFrameUpdate callback
  * LayoutController support, client can set Vertical&Horizontal offset for the layout
  * Support worldToViewPort & viewportToWorld conversion
* DriveSession:
  * JunctionView improvements
  * Guidance of multiple alerts
    * SpeedLimit
    * Running red light
    * Congestion
* Direction:
  * Add api to obtain route's UUID
  * Add RoadCalibrator for jump button support
* System improvements:
  * Init & dispose SDK instance from worker thread instead of main thread
* Misc.
  * Add build version for Javadoc
  * General bugfixes and performance improvements

## **Version 0.6.72.24**
2020-12-25

* MapView:
  * Additional user interaction improvements
  * Viewport offset support
  * TSS loading support
  * Route eating Support
* DriveSession:
  * Custom location provider support
  * Dynamic ReRouting support
* Direction:
  * Update response data model to support additional attributes
* System improvements:
  * Cloud environment setup
  * Java 1.7 compatible support
  * Streaming mode
* Misc.
  * General bugfixes and performance improvements

## **Version 0.6.71.2**
2020-12-04

* MapView:
  * Vehicle controller support
  * Route controller and annotation controller improvements
* DriveSession:
  * Rich navigation event support
  * Alert support
* Direction:
  * Update response data model to support additional attributes
* Misc.
  * General bugfixes and performance improvements

## **Version 0.6.70.1**
* MapView:
  * Bug fixes
* DriveSession:
  * EU Region support
  * AudioGuidance support
  * TraveledDistance and TraveledTime support
* Map:
  * Bug fixes
* Documentation:
  * Javadoc for SDK components

## **Version 0.6.70**
* MapView:
  * Camera controller support
  * Camera bearing angle
  * Central position of camera
  * Different follow vehicle mode support
  * Free mode support
  * Zoom level control
  * Route controller support
  * Annotation controller support
* DriveSession:
  * Adas event listener support
* Documentation:
  * Javadoc for SDK components

