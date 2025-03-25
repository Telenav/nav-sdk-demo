# Introduction
This is a sample app to demonstrate how to integrate Telenav navigation SDK with basic features.

The SDK package is hosted on Alibaba Cloud, allowing seamless integration via Maven or Gradle.


## Key Demo Resources
| Resource     |     Description                                                    |
|--------------|--------------------------------------------------------------------|
|*Project Name*|nav-sdk-demo                                                        |
|*Summary*     |Basic sample app demonstrating integrate Telenav navigation SDK |
|*README.md*    |This README document                                                 |
|*[app/build.gradle](https://github.com/Telenav/nav-sdk-demo/blob/easy/app/build.gradle)* |Application gradle file. SDK dependency configuration          |
|*[app/src/main/res/layout/activity_main.xml](https://github.com/Telenav/nav-sdk-demo/blob/easy/app/src/main/res/layout/activity_main.xml)* | Main activity layout definition file. Declaring Mapview |
|*[app/src/main/java/com/telenav/sdk/demo/SplashActivity.kt](https://github.com/Telenav/nav-sdk-demo/blob/easy/app/src/main/java/com/telenav/sdk/demo/SplashActivity.kt)* | Navigation SDK initialization |
|*[app/src/main/java/com/telenav/sdk/demo/MainActivity.kt.kt](https://github.com/Telenav/nav-sdk-demo/blob/easy/app/src/main/java/com/telenav/sdk/demo/MainActivity.kt)* | Majority part of logic on showing map, route request, start and stop navigation |

# Getting Started

## Prerequisites
Before running the demo, ensure you have the following:

1. **Development Environment**:

- Android Studio
- Gradle
- Git
- JDK: Version 8 or higher

2. **Device Requirements**:

- Android 6.0 (API Level 23) or higher.
- GPS and internet access enabled on the device.

## Clone or Download Project Source Code
Please make sure you have access of this github repo, run below git commands to download the source code.
```git
git clone git@github.com:Telenav/nav-sdk-demo.git
git checkout easy
```

## API key/secret

To delight all potential users, it's provide a set of trial API key/secret for the EU region for free use.It will expire in six months, after which users must check out the latest code from the repo to obtain a new trial API.

## Config SDK Dependency

To start Navigation SDK integration, add the SDK as a dependency of the project. 

> Currently we're using version [3.4.0-lts4-rc12.1](https://docs.telenav.com/nav-unified/release-notes.html). SDK release history can be found at [Navigation SDK release page](https://docs.telenav.com/nav-unified/release-notes.html).

```Gradle
dependencies {
	implementation "com.telenav.sdk:telenav-android-mapview-rc15:${telenavSdkVersion}"
	implementation "com.telenav.sdk:telenav-android-drivesession-rc15:${telenavSdkVersion}"
	implementation "com.telenav.sdk:telenav-android-ngx-rc15:${mapPluginVersion}"
}
```

## Initialize Navigation SDK 
To initialize Navigation SDK, you need to create an instance of *NavSDKOptions*, please see below code snippet. For more detailed implement, please refer to function *SplashActivity.initNavSDK* from the project source file *SplashActivity.kt*. <br/>Please don't forget to replace *'SDK_KEY'* and *'SDK_SECRET'* with correct API key and secret strings respectively which you need to get from Telenav first. 

> ***In this demo application works in pure streaming mode, thus the cloud endpoint is mandatory. And *sdkCacheDataDir* needs to be a writable folder.***

```kotlin
    val sdkCacheDataDir = "$cacheDir/nav-cached/"           //  specific any writable data folder
    val cloudEndPoint = "https://restapistage.telenav.com"  //  specific correct cloud endpoint
    val apiKey = "SDK_KEY"                                  //  TODO("replace with correct API key")
    val apiSecret = "SDK_SECRET"                            //  TODO("replace with correct API secret")
    val sdkOptions = SDKOptions.builder()
        .setApiKey(apiKey)
        .setApiSecret(apiSecret)
        .setSdkCacheDataDir(sdkCacheDataDir)
        .setCloudEndPoint(cloudEndPoint)
        .setLocale(Locale.EN_US)
        .build()
    
    val navSDKOptions = NavSDKOptions.builder(sdkOptions)
        //  add some other navigation related options here
        //  ...
        .enableTraffic(true)
        .build()
    SDK.getInstance().initialize(this@SplashActivity, navSDKOptions)
```

## Showing Map
- Step 1, Declare Mapview on the target layout.
```Layout
    <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:context="com.telenav.sdk.demo.MainActivity">

        <com.telenav.map.views.TnMapView
            android:id="@+id/map_view"
            android:layout_width="0dp"
            android:layout_height="0dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

       ...

    </androidx.constraintlayout.widget.ConstraintLayout>
```

- Step 2, Initalize MapView after it has been created.
In the demo app, you call function *map_view.initialize* for map view initialization(hereby the instance of *map_view* is the instance declared in layout from *Step 1*): 
```kotlin
    class MainActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            map_view.initialize(savedInstanceState) {
                //  you can add more logic here to control the initial zoom level, set initial camera position etc.
                //  ...
            }
        }
    }
```

For more detailed implementation, please find from the project source file *MainActivity.kt*. Also, please find detail Map View related document from [Show MapView](https://docs.telenav.com/nav/show-map.html)

## Working with DriveSession
Call function *DriveSession.Factory.createInstance* to obtain an instance of DriveSession. The application layer needs to register drive session event listeners to receive different types of notifications. <br/>For more information, please refer the document [Work with DriveSession](https://docs.telenav.com/nav/start-navigation.html). To make the demo application simple, only a few events are processed. Most events are ignored and marked as *TODO*.

# Compilation and Running
Build the demo project, connect target Android device. If everything OK, the demo application will be able to be installed and launched on the target device.
You will see a map after application is launched. The map supports gestures like panning, pinch zooming, tilting etc.
Long press on map display area to set a destination, application will trigger route requesting with that destination.
To start navigation, click the "Start Navigation" button shows on middle-bottom of screen. click this button again will stop navigation and go back to the original state.

# Documentation
To get more help on SDK integration and other features, please refer to below documentation website:
[Vivid Navigation Android](https://docs.telenav.com/overview/nav.html)

## **FAQs**

### **1. How do I update the SDK?**

Update the version number in your `gradle.properties` file:

```
telenavSdkVersion=3.4.0-lts4-rc12.1
mapPluginVersion=0.18.1-lts4-rc12.1
```

### **2. What if dependencies fail to download?**

Ensure your network can access Alibaba Cloud and that the repository URL is configured correctly. If issues persist, contact technical support.

## **Supported Architectures**

- ARMv7
- ARM64 (ARMv8)
- x86/x86_64

## **Contact and Support**

For further assistance, please contact the development team at huitang@telenav.cn.

