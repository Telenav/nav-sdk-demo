# Telenav Android Navigation SDK Demo

A reference Android application demonstrating how to integrate the **Telenav Android Navigation SDK (TASDK)** with map display, route planning, turn-by-turn navigation, and entity search.

SDK artifacts are distributed via **Alibaba Cloud Maven** and can be consumed with Gradle.

| Item | Value |
|------|-------|
| Application ID | `com.telenav.sdk.demo` |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 33 |
| Current TASDK | `4.26.0-rc.2` |
| NDK flavor | `r15c` |

## Features

This demo showcases the following integration scenarios:

- **SDK initialization** — configure API credentials, cloud endpoint, region, and cache directory
- **Map rendering** — `TnMapView` with traffic, landmarks, buildings, and camera control
- **Route planning** — request routes via long-press on the map or entity search
- **Turn-by-turn navigation** — start/stop navigation with guidance and position events
- **Entity search** — POI search powered by `telenav-entity-cloud`
- **Simulated location** — demo location provider for navigation without real GPS movement
- **Storage permission handling** — scoped storage adaptation including `MANAGE_EXTERNAL_STORAGE` on Android 11+

## Project Structure

```
nav-sdk-demo/
├── app/
│   ├── build.gradle                 # App module & SDK dependencies
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/telenav/sdk/demo/
│       │   ├── SplashActivity.kt    # Permissions & SDK initialization
│       │   ├── MainActivity.kt      # Map, routing & navigation
│       │   ├── SimulationLocationProvider.kt
│       │   ├── search/              # Entity search UI & logic
│       │   └── utils/
│       │       └── StoragePermissionHelper.kt
│       └── res/
├── build.gradle                     # Root build & Maven repositories
├── gradle.properties                # SDK versions & API configuration
└── settings.gradle
```

## Prerequisites

| Requirement | Version / Notes |
|-------------|-----------------|
| Android Studio | Arctic Fox or newer recommended |
| JDK | 8+ |
| Gradle | Wrapper included (`./gradlew`) |
| Device / Emulator | API 26+, GPS & network enabled |
| Maven access | Alibaba Cloud private repository credentials |

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/Telenav/nav-sdk-demo.git
cd nav-sdk-demo
```

### 2. Configure API credentials

Edit `gradle.properties` and set your Telenav API key, secret, region, and cloud endpoint:

```properties
Region="EU"
API_KEY="<your-api-key>"
API_SECRET="<your-api-secret>"
CloudEndPoint="https://apieustg.telenav.com"

# SEA region example (uncomment and disable EU block):
# Region="SEA"
# API_KEY="<your-api-key>"
# API_SECRET="<your-api-secret>"
# CloudEndPoint="https://apiseastg.telenav.com"
```

> Trial API keys may be bundled in the repository for evaluation. They expire periodically — pull the latest code or contact Telenav for renewed credentials.

### 3. Configure Maven repositories

Ensure `build.gradle` includes the Alibaba Cloud Maven repositories with valid credentials. The project uses:

- `https://maven.aliyun.com/repository/public`
- Telenav private release & snapshot repositories on `packages.aliyun.com`

### 4. Build and run

```bash
./gradlew clean :app:assembleDebug
./gradlew :app:installDebug
```

Or open the project in Android Studio and run the `app` module on a connected device.

On first launch, grant **location** and **all-files access** (Android 11+) when prompted.

## SDK Dependency Configuration

SDK versions are centralized in `gradle.properties`:

```properties
ndkVersion=r15c
telenavSdkVersion=4.26.0-rc.2
mapPluginVersion=0.56.0-rc.2
entityVersion=2.4.7
baseVersion=2.1.9
```

Dependencies in `app/build.gradle`:

```gradle
implementation "com.telenav.sdk:telenav-android-mapview-${ndkVersion}:${telenavSdkVersion}"
implementation "com.telenav.sdk:telenav-android-navigation-${ndkVersion}:${telenavSdkVersion}"
implementation "com.telenav.sdk:telenav-android-ehservice-${ndkVersion}:${telenavSdkVersion}"
implementation "com.telenav.sdk:telenav-android-ngx-${ndkVersion}:${mapPluginVersion}"
implementation "com.telenav.sdk:telenav-entity-cloud:${entityVersion}"
implementation "com.telenav.sdk:telenav-sdk-base:${baseVersion}"
```

Release history: [TA SDK Android Releases](https://spaces.telenav.com:8443/spaces/map/pages/276092951/TA+SDK+Android+Releases)

## Integration Guide

### Initialize the SDK

SDK initialization is performed in `SplashActivity`. The demo runs in **streaming mode**, which requires:

1. A valid **cloud endpoint**
2. A **writable cache directory** for map data

```kotlin
val sdkCacheDataDir = "$cacheDir/nav-cached/"
val sdkOptions = SDKOptions.builder()
    .setApiKey(BuildConfig.API_KEY)
    .setApiSecret(BuildConfig.API_SECRET)
    .setSdkCacheDataDir(sdkCacheDataDir)
    .setCloudEndPoint(BuildConfig.CloudEndPoint)
    .setLocale(Locale.EN_US)
    .setRegion(BuildConfig.Region)
    .setUserId("AndroidDemoTest")
    .build()

val navSDKOptions = NavSDKOptions.builder(sdkOptions)
    .setMapStreamingSpaceLimit(1024 * 1024 * 1024)
    .build()

SDK.getInstance().initialize(context, navSDKOptions)
EntityService.initialize(sdkOptions)
```

See `SplashActivity.initNavSDK()` for the full implementation.

### Display the map

**Layout** — declare `TnMapView` in your activity layout:

```xml
<com.telenav.map.views.TnMapView
    android:id="@+id/map_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

**Initialize** — call `initialize()` after the view is created:

```kotlin
map_view.initialize(savedInstanceState) {
    // Map is ready — configure camera, features, listeners
}
```

See `MainActivity.configureMapView()` for feature toggles and camera setup.

### Navigation session

```kotlin
val navigationService = NavigationService.Factory.createInstance()
navigationService.eventHub.addNavigationEventListener(listener)
navigationService.eventHub.addPositionEventListener(listener)
SDK.getInstance().injectLocationProvider(locationProvider)
```

Route request, navigation start/stop, and event handling are implemented in `MainActivity.kt`.

## Usage

1. Launch the app and wait for SDK initialization.
2. Interact with the map (pan, pinch-zoom, tilt).
3. **Long-press** on the map to set a destination and request a route.
4. Or tap **Search for a Destination** to find a POI via entity search.
5. Tap **Start Navigation** to begin guidance; tap again to stop.

## Permissions

| Permission | Purpose |
|------------|---------|
| `ACCESS_FINE_LOCATION` | Navigation and map positioning |
| `INTERNET` | Cloud map data & routing services |
| `ACCESS_NETWORK_STATE` | Network connectivity checks |
| `WRITE_EXTERNAL_STORAGE` | Legacy storage (API ≤ 29) |
| `MANAGE_EXTERNAL_STORAGE` | Map cache & SDK data (API 30+) |

Storage permission logic is handled by `StoragePermissionHelper` and requested in `SplashActivity`.

## Troubleshooting

### Dependency resolution fails

- Verify Alibaba Maven repository URL and credentials in `build.gradle`.
- Confirm the SDK version exists in the release repository.
- Run `./gradlew :app:dependencies --configuration debugCompileClasspath` to inspect the dependency tree.

### `packageDebug` / `manifestOutputs is null`

Run a clean build:

```bash
./gradlew clean :app:assembleDebug
```

The project enables `packagingOptions.jniLibs.useLegacyPackaging` to match `android:extractNativeLibs="true"`.

### SDK initialization fails

- Check `API_KEY`, `API_SECRET`, `CloudEndPoint`, and `Region` in `gradle.properties`.
- Ensure the device has network access and required permissions are granted.
- Review logcat output filtered by `TaLog` or `Nav SDK Demo`.

## Supported ABIs

- `armeabi-v7a`
- `arm64-v8a`
- `x86` / `x86_64`

## Documentation

- [Vivid Navigation Android Overview](https://docs.telenav.com/overview/nav.html)
- [Show MapView](https://docs.telenav.com/nav/show-map.html)
- [Start Navigation](https://docs.telenav.com/nav/start-navigation.html)

## License & Support

This project is provided as an integration sample. For API credentials, SDK updates, or technical support, contact the Telenav development team.
