## **Version 1.4.42.2**
2023-02-05

* Misc.
  * General bugfixes

## **Version 1.4.41.4**
2023-10-23

* Misc.
  * General bugfixes
  
## **Version 1.4.40.2**
2023-09-05

* Misc.
  * General bugfixes and performance improvements
  * Update native third-party libraries

## **Version 1.4.39.2**
2023-07-26

* Misc.
  * General bugfixes
  
## **Version 1.4.38.2**
2023-06-26

* Misc.
  * General bugfixes

## **Version 1.4.37.1**
2023-05-22

* Misc.
  * General bugfixes
  
## **Version 1.4.36.10**
2023-05-05

* Misc.
  * General bugfixes

## **Version 1.4.35.1**
2023-03-17

* Misc.
  * General bugfixes

## **Version 1.4.34.1**
2023-02-10

* MapView:
  * Map engine CPU consumption improvement in the idle state
* Misc.
  * General bugfixes

## **Version 1.4.33.7**
2023-01-10

* MapView:
  * GetNearest API improvement to avoid ANR
* Misc.
  * General bugfixes

## **Version 1.4.32.1**
2022-12-15

* MapView:
  * Expose AutoZoom on/off API
  * Fix AutoZoom controller did not handle ramps properly
  * Inject LifecycleOwner to TnMapView
* Misc.
  * Correct log print in BrokerServerHelper
  * General bugfixes

## **Version 1.4.31.4**
2022-11-27

* MapView:
  * Fix: Map element could not update to corresponding language
  * Override UserGraphic construct to avoid bitmap copy
  * Support front passenger screen
* Misc.
  * Support native log level setting by topic
  * Traffic display improvement
  * General bugfixes

## **Version 1.4.30.1**
2022-10-23

* DriveSession:
  * Update phoneme language information in TTS output
* Misc.
  * General bugfixes

## **Version 1.4.29.1**
2022-09-25

* MapView:
  * CVP icon could not be shown at the first time without GPS
* DriveSession:
  * Add Shield icons for MEA region
* Misc.
  * New region RN ( ISC) support
  * Support jni log level setting
  * Expose the api of setting log of ADAS
  * General bugfixes and performance improvements


## **Version 1.4.27.3**
2022-08-26

* DriveSession:
  * Disable shield icon to be rendered by default
  * Add sorted signpost names in maneuverInfo
  * Support Toll booth audio prompt switch
* Misc.
  * General bugfixes and performance improvements


## **Version 1.4.26.6**
2022-07-28

* MapView:
  * Support map element POI on map clickable
  * Notify the current render mode when tilt gesture ends
* DriveSession:
  * Add HighVigilanceArea alert type
  * Destination distance threshold adjustment
* Misc.
  * General bugfixes


## **Version 1.4.25.8**
2022-06-30

* MapView:
  * Expose interface to set cluster style independently
  * Make the CVP a tappable annotation
* DriveSession:
  * Add API to disable/enable navigation status to be prompted by audio
  * Support different verbosity level in audio guidance
* Direction:
  * Add road type/road subtype in route response model
  * Support config the numbers of onboard route when network disconnected
* Misc.
  * General bugfixes


## **Version 1.4.24.1**
2022-05-19

* DriveSession:
  * Send available satellite count into Position Engine
  * Exposing shield icon from navigation maneuver info
* Direction:
  * Add controlled access information in route response
* Misc.
  * Provide a method to disconnect all the cloud service
  * General bugfixes

## **Version 1.4.23.1**
2022-04-20

* MapView:
  * Expose api to check mapview is FinishedLoading
* Direction:
  * Add a timeoutForOldEmbeddedDataVersion configuration parameter in the HybridClientConfig
* Misc.
  * General bugfixes

## **Version 1.4.22.3**
2022-03-31

* MapView:
  * Expose api to enable/disable free flow traffic
* DriveSession:
  * Provide traffic light signal in alert service
* Misc.
  * Add map version update listener
  * General bugfixes

## **Version 1.4.21.2**
2022-03-09

* DriveSession:
  * Lane guidance for passing cross in AGV
  * Send satellite number into Position Engine
* Misc.
  * General bugfixes

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
