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
