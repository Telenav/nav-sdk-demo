package com.telenav.sdk.demo.search

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.telenav.sdk.core.SDKOptions
import com.telenav.searchservice.SearchService
import com.telenav.searchservice.api.GoogleSearchAvailabilityListener
import com.telenav.searchservice.api.GoogleSearchAvailabilityState
import com.telenav.searchservice.api.GoogleSearchUnavailabilityReason
import com.telenav.searchservice.api.NetworkMode
import com.telenav.searchservice.google.GoogleSearchBridge

/**
 * App-level SearchService lifecycle — mirrors search-service-demo MainActivity init/dispose.
 */
object SearchServiceHolder {

    private const val TAG = "SearchServiceHolder"

    @Volatile
    private var initialized = false

    private var lastKnownLatitude: Double? = null
    private var lastKnownLongitude: Double? = null
    private var lastRefreshLatitude: Double? = null
    private var lastRefreshLongitude: Double? = null

    private var networkProvider: AndroidNetworkConnectivityProvider? = null
    private var networkModeObserver: ((Boolean) -> Unit)? = null
    private var lastNetworkConnected: Boolean? = null
    private var googleAvailabilityListener: GoogleSearchAvailabilityListener? = null

    private var initStartElapsedMs: Long = 0L
    private var webViewReadyLogged = false
    private var googleAvailableLogged = false

    private val webViewReadyCallback: (Boolean) -> Unit = { ready ->
        if (ready) {
            logWebViewReadyTiming()
            if (needsAvailabilityRefresh()) {
                refreshGoogleAvailabilityAtLastLocation(force = false)
            }
        }
    }

    fun isInitialized(): Boolean = initialized

    fun isGoogleSearchAvailable(): Boolean = getGoogleSearchAvailabilityState().available

    fun getGoogleSearchAvailabilityState(): GoogleSearchAvailabilityState {
        if (!initialized) return GoogleSearchAvailabilityState.UNKNOWN
        return try {
            SearchService.getGoogleSearchAvailabilityState()
        } catch (e: Exception) {
            Log.w(TAG, "getGoogleSearchAvailabilityState failed", e)
            GoogleSearchAvailabilityState.UNKNOWN
        }
    }

    fun setGoogleAvailabilityListener(listener: GoogleSearchAvailabilityListener?) {
        googleAvailabilityListener = listener
        if (!initialized) return
        SearchService.setGoogleSearchAvailabilityListener(createDelegatingListener())
        notifyGoogleAvailabilityIfNeeded()
    }

    fun initialize(context: Context, sdkOptions: SDKOptions): Boolean {
        if (initialized) return true
        initStartElapsedMs = SystemClock.elapsedRealtime()
        webViewReadyLogged = false
        googleAvailableLogged = false
        return try {
            val appContext = context.applicationContext
            val provider = AndroidNetworkConnectivityProvider(appContext)
            networkProvider = provider
            val options = SearchServiceConfig.buildInitOptions(appContext, sdkOptions)
            val ok = SearchService.initialize(appContext, options)
            if (ok) {
                initialized = true
                val syncInitMs = SystemClock.elapsedRealtime() - initStartElapsedMs
                registerNetworkModeObserver(provider)
                GoogleSearchBridge.registerWebViewReadyCallback(webViewReadyCallback)
                SearchService.setGoogleSearchAvailabilityListener(createDelegatingListener())
                Log.i(TAG, "SearchService initialized, sync init took ${syncInitMs}ms")
                if (GoogleSearchBridge.isWebViewReady()) {
                    logWebViewReadyTiming()
                    refreshGoogleAvailabilityAtLastLocation(force = false)
                }
                logGoogleAvailableTiming(isGoogleSearchAvailable())
                notifyGoogleAvailabilityIfNeeded()
            } else {
                resetInitTiming()
                provider.release()
                networkProvider = null
                Log.e(TAG, "SearchService.initialize returned false")
            }
            ok
        } catch (e: Exception) {
            resetInitTiming()
            Log.e(TAG, "SearchService init failed", e)
            false
        }
    }

    fun release() {
        if (!initialized) return
        GoogleSearchBridge.unregisterWebViewReadyCallback(webViewReadyCallback)
        SearchService.setGoogleSearchAvailabilityListener(null)
        googleAvailabilityListener = null
        unregisterNetworkModeObserver()
        SearchService.dispose()
        networkProvider?.release()
        networkProvider = null
        initialized = false
        lastKnownLatitude = null
        lastKnownLongitude = null
        lastRefreshLatitude = null
        lastRefreshLongitude = null
        lastNetworkConnected = null
        resetInitTiming()
        Log.i(TAG, "SearchService released")
    }

    /**
     * Sync vehicle position to SearchService. Per-request search APIs still need
     * [SearchViewModel.setSearchCenter]; this refreshes Google availability when location shifts.
     */
    fun updateLocation(latitude: Double, longitude: Double) {
        lastKnownLatitude = latitude
        lastKnownLongitude = longitude
        refreshGoogleAvailabilityAtLastLocation(force = false)
    }

    private fun refreshGoogleAvailabilityAtLastLocation(force: Boolean) {
        val lat = lastKnownLatitude ?: return
        val lon = lastKnownLongitude ?: return
        if (!initialized) return

        val countryMissing = getGoogleSearchAvailabilityState().reason ==
            GoogleSearchUnavailabilityReason.COUNTRY_UNKNOWN

        val lastLat = lastRefreshLatitude
        val lastLon = lastRefreshLongitude
        val movedEnough = lastLat == null || lastLon == null ||
            distanceMeters(lastLat, lastLon, lat, lon) >= 1_000.0

        if (!force && !countryMissing && !movedEnough) {
            return
        }

        lastRefreshLatitude = lat
        lastRefreshLongitude = lon
        try {
            SearchService.refreshGoogleAvailability(lat, lon)
            Log.d(
                TAG,
                "SearchService location refreshed at ($lat, $lon) " +
                    "force=$force countryMissing=$countryMissing"
            )
            notifyGoogleAvailabilityIfNeeded()
        } catch (e: Exception) {
            Log.w(TAG, "SearchService location refresh failed", e)
        }
    }

    private fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun registerNetworkModeObserver(provider: AndroidNetworkConnectivityProvider) {
        val observer: (Boolean) -> Unit = { connected ->
            if (initialized && lastNetworkConnected != connected) {
                lastNetworkConnected = connected
                SearchService.getClient().setNetworkMode(
                    if (connected) NetworkMode.CONNECTED else NetworkMode.DISCONNECTED
                )
                notifyGoogleAvailabilityIfNeeded()
            }
        }
        provider.observeNetworkState(observer)
        networkModeObserver = observer
    }

    private fun unregisterNetworkModeObserver() {
        val provider = networkProvider
        val observer = networkModeObserver
        if (provider != null && observer != null) {
            provider.removeNetworkObserver(observer)
        }
        networkModeObserver = null
        lastNetworkConnected = null
    }

    private fun createDelegatingListener(): GoogleSearchAvailabilityListener {
        return GoogleSearchAvailabilityListener { available, state ->
            logGoogleAvailableTiming(available)
            googleAvailabilityListener?.onGoogleSearchAvailabilityChanged(available, state)
        }
    }

    private fun needsAvailabilityRefresh(): Boolean {
        if (!initialized) return false
        return getGoogleSearchAvailabilityState().reason ==
            GoogleSearchUnavailabilityReason.COUNTRY_UNKNOWN
    }

    private fun notifyGoogleAvailabilityIfNeeded() {
        val state = getGoogleSearchAvailabilityState()
        googleAvailabilityListener?.onGoogleSearchAvailabilityChanged(state.available, state)
    }

    private fun logWebViewReadyTiming() {
        if (webViewReadyLogged || initStartElapsedMs <= 0L) return
        webViewReadyLogged = true
        val elapsedMs = SystemClock.elapsedRealtime() - initStartElapsedMs
        Log.i(TAG, "Google WebView ready in ${elapsedMs}ms since SearchService init started")
    }

    private fun logGoogleAvailableTiming(available: Boolean) {
        if (!available || googleAvailableLogged || initStartElapsedMs <= 0L) return
        googleAvailableLogged = true
        val elapsedMs = SystemClock.elapsedRealtime() - initStartElapsedMs
        Log.i(TAG, "Google search available in ${elapsedMs}ms since SearchService init started")
    }

    private fun resetInitTiming() {
        initStartElapsedMs = 0L
        webViewReadyLogged = false
        googleAvailableLogged = false
    }
}
