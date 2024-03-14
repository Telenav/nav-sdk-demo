package com.telenav.sdk.examples.scenario.search.fragments.searchComponents.searchEngine

import android.content.Context
import android.location.Location
import android.os.Bundle
import com.telenav.map.api.Annotation
import com.telenav.map.api.search.PoiSearchEntity
import com.telenav.map.api.search.SearchEngine
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.examples.util.BitmapUtils
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType
import com.telenav.sdk.entity.model.base.GeoPoint
import com.telenav.sdk.entity.model.base.Polygon
import com.telenav.sdk.entity.model.search.CategoryFilter
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.entity.model.search.PolygonGeoFilter
import com.telenav.sdk.entity.model.search.SearchFilters
import com.telenav.sdk.examples.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contains the logic of search POI
 * @author Mykola Ivantsov - (p)
 */
class SearchEngineExample(val context: Context) : SearchEngine {

    var vehicleLocation: Location? = null

    private val geoLocation1: Location by lazy {
        createLocation(48.113520, 11.572267)
    }

    private val geoLocation2: Location by lazy {
        createLocation(48.117520, 11.577267)
    }

    private val savedPlacesResult = listOf(geoLocation1, geoLocation2)

    override fun search(
        displayContent: List<String>,
        searchBox: List<GeoPoint>,
        resultSize: Int,
        language: String
    ): List<PoiSearchEntity>? {
        if (displayContent.contains(SEARCH_SAVED_PLACES)) {
            return savedPlacesResult.mapNotNull { location ->
                wrapperToPoiSearchSavedPlace(location)
            }
        }
        printDebugLog("NavSearchEngine: vehicleLocation = $vehicleLocation")
        vehicleLocation?.let { nonNullVehicleLocation ->
            val polygon = createPolygon(searchBox)
            printDebugLog("created polygon = $polygon")
            val geoFilter = createPolygonGeoFilter(polygon)
            printDebugLog("created geoFilter = $geoFilter")
            val categoryFilter = createCategoryFilter(displayContent)
            printDebugLog("created categoryFilter = $categoryFilter")
            val searchFilters = createSearchFilters(categoryFilter, geoFilter)
            printDebugLog("created searchFilters = $searchFilters")
            try {
                _uiState.value = SearchLoading.Loading
                val result = makeSearchRequest(
                    searchFilters,
                    resultSize,
                    nonNullVehicleLocation,
                    createEntityClient()
                )
                printDebugLog("result = $result")
                _uiState.value = SearchLoading.Success
                return result.results?.mapNotNull { entity ->
                    printDebugLog("incoming entity = $entity")
                    val poiSearchEntity = wrapperToPoiSearchEntity(entity)
                    printDebugLog("wrapped to POI search entity = $poiSearchEntity")
                    poiSearchEntity
                }
            } catch (ex: Throwable) {
                printErrorLog("Have some problems!", ex)
                _uiState.value = SearchLoading.Fail(ex.message ?: "")
            }
        }
        return emptyList()
    }

    private fun createPolygon(geoPoint: List<GeoPoint>): Polygon = Polygon
        .builder()
        .setPoints(geoPoint)
        .build()

    private fun createPolygonGeoFilter(polygon: Polygon): PolygonGeoFilter = PolygonGeoFilter
        .builder(polygon)
        .build()

    private fun createCategoryFilter(displayContent: List<String>): CategoryFilter =
        CategoryFilter.builder()
            .setCategories(displayContent.filter {
                it != SEARCH_SAVED_PLACES
            }).build()

    private fun createSearchFilters(
        categoryFilter: CategoryFilter,
        polygonGeoFilter: PolygonGeoFilter
    ): SearchFilters = SearchFilters.builder()
        .setCategoryFilter(categoryFilter)
        .setGeoFilter(polygonGeoFilter)
        .build()

    private fun createEntityClient(): EntityClient = EntityService.getClient()
    private fun makeSearchRequest(
        searchFilters: SearchFilters,
        resultSize: Int,
        vehicleLocation: Location,
        entityClient: EntityClient
    ): EntitySearchResponse = entityClient.searchRequest()
        .setFilters(searchFilters)
        .setLimit(resultSize)
        .setLocation(vehicleLocation.latitude, vehicleLocation.longitude)
        .execute()

    private fun wrapperToPoiSearchEntity(entity: Entity): PoiSearchEntity? {
        val name: String
        val geoLocation: Location
        val navLocation: Location
        val category: String
        when (entity.type) {
            EntityType.ADDRESS -> {
                name = entity.address.formattedAddress
                geoLocation = createLocation(
                    entity.place.address.geoCoordinates.latitude,
                    entity.place.address.geoCoordinates.longitude
                )
                navLocation = createLocation(
                    entity.place.address.navCoordinates.latitude,
                    entity.place.address.navCoordinates.longitude
                )
                category = DEFAULT_CATEGORY
            }
            EntityType.PLACE -> {
                name = entity.place.name
                geoLocation = createLocation(
                    entity.place.address.geoCoordinates.latitude,
                    entity.place.address.geoCoordinates.longitude
                )
                navLocation = createLocation(
                    entity.place.address.navCoordinates.latitude,
                    entity.place.address.navCoordinates.longitude
                )
                category = entity.place.categories.lastOrNull()?.id ?: DEFAULT_CATEGORY
            }
            else -> {
                return null
            }
        }

        val bundle = createBundle(name, geoLocation, navLocation, entity, category)
        //endregion
        return PoiSearchEntity(geoLocation, bundle)
    }

    private fun wrapperToPoiSearchSavedPlace(location: Location): PoiSearchEntity {
        val graphic: Annotation.UserGraphic =
            Annotation.UserGraphic(
                BitmapUtils.getBitmapFromVectorDrawable(
                    context,
                    R.drawable.ic_test_icon_day
                )!!
            )

        val bundle = createBundle(location)
        return PoiSearchEntity(location, graphic, bundle)
    }

    private fun createBundle(location: Location): Bundle = Bundle().apply {
        this.putString(BUNDLE_NAME, BUNDLE_VALUE_SAVED_PLACE)
        this.putString(BUNDLE_CATEGORY, SEARCH_SAVED_PLACES)
        this.putParcelable(BUNDLE_GEO_LOCATION, location)
    }

    private fun createBundle(
        name: String,
        geoLocation: Location,
        navLocation: Location,
        entity: Entity,
        category: String
    ): Bundle = Bundle().apply {
        this.putString(BUNDLE_NAME, name)
        this.putParcelable(BUNDLE_GEO_LOCATION, geoLocation)
        this.putParcelable(BUNDLE_NAV_LOCATION, navLocation)
        this.putDouble(BUNDLE_DISTANCE, entity.distance)
        //if you have not injected PoiAnnotationFactory TnSearchController will use DefaultPoiAnnotationFactory
        //but for preventing NPE you should include the in your Bundle object the next key TnSearchController.STYLE_KEY
        //with the your POI style key that will use by default
//        this.putString(TnSearchController.STYLE_KEY, TnSearchController.DEFAULT_VALUE_STYLE)
        this.putString(BUNDLE_CATEGORY, category)
    }

    private fun printDebugLog(msg: String) {
        TaLog.d(TAG, "msg: $msg | Thread.name: ${Thread.currentThread().name}")
    }

    private fun printErrorLog(msg: String, ex: Throwable) {
        TaLog.e(
            TAG,
            "msg: $msg | ex msg: ${ex.message}| Thread.name: ${Thread.currentThread().name}",
            ex
        )
    }

    private fun createLocation(latitude: Double, longitude: Double): Location {
        return Location("MOCK").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
    }

    companion object {
        //region General categories (can be different for each project)
        const val SEARCH_FOOD = "2040"

        //region subcategories of food
        const val SPECIALTY_FOOD = "274"

        //endregion
        const val SEARCH_ELECTRIC_CHARGE_STATION = "771"
        const val SEARCH_PARKING = "600"

        //region subcategories of parking
        const val PARKING_LOT = "611"

        //endregion
        const val SEARCH_AUTOMOTIVE = "1000"

        //region subcategories of Automotive
        const val E10 = "771"

        //endregion
        const val SEARCH_SHOPPING = "4090"

        //region subcategories of shopping
        const val DISCOUNT_STORE = "933"

        //endregion
        const val SEARCH_TRAVEL = "5800"
        const val SEARCH_TRANSIT = "2200"
        const val SEARCH_ACCOMODATIONS = "5850"
        const val SEARCH_ENTERTAINMENT = "1620"
        const val SEARCH_ATTRACTIONS = "782"
        const val SEARCH_SPORTS_AND_ACTIVITIES = "5430"
        const val SEARCH_EMERGENCY_AND_HEALTH = "2750"
        const val SEARCH_COMMUNITY_AND_GOVERNMENT = "86"
        const val SEARCH_FINANCIAL = "5870"
        const val SEARCH_SERVICES = "5080"

        //endregion
        const val SEARCH_SAVED_PLACES = "SavedPlaces"

        const val BUNDLE_NAME = "name"
        const val BUNDLE_GEO_LOCATION = "geoLocation"
        const val BUNDLE_NAV_LOCATION = "navLocation"
        const val BUNDLE_DISTANCE = "distance"
        const val BUNDLE_CATEGORY = "category"
        const val BUNDLE_VALUE_SAVED_PLACE = "saved places search"
        const val DEFAULT_CATEGORY = ""

        private const val TAG = "POI_SEARCH"

        // Backing property to avoid state updates from other classes
        private val _uiState = MutableStateFlow<SearchLoading>(SearchLoading.NotInit)

        // The UI collects from this StateFlow to get its state updates
        val uiState: StateFlow<SearchLoading> = _uiState
    }
}

sealed class SearchLoading {
    object NotInit : SearchLoading()
    object Success : SearchLoading()
    class Fail(val msg: String) : SearchLoading()
    object Loading : SearchLoading()
}