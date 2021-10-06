# Introduction
This is a sample app to demonstrate how to integrate Telenav navigation SDK with basic features.

# Compilation and running

## Prerequisites
Make sure you have the following tools installed on your computer.
- Android Studio
- Gradle

## Setting API Key and Secret
Before compiling the code, you must update the code with correct API key and secret. Please get them from Telenav and set them in file "SplashActivity.kt".
```kotlin
const val API_KEY = "API_KEY"
const val API_SECRET = "API_SECRET"
```

## Compilation
Once API key and secret are updated, run "gradlew build" command or use "Import Project" in Android Stuido.

## Running the application
This application includes the following features, map display, route calculation and navigation.
You will see a map after application is launched. The map supports the following gestures, panning, pinch zooming, tilting etc.
Long press on map display area to set a destination, application will trigger route requesting with that destination.
To start navigation, click the "Start Navigation" button shows on middle-bottom of screen. click this button again will stop navigation and go back to the original state.

# Documentation
To get more help on SDK integration and other features, refer to the following documentation website.

[Developer Guides and API references for Telenav APIs and SDKs](https://docs.telenav.com/overview/index.html)
