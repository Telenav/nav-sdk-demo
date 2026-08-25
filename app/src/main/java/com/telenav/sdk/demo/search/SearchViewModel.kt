package com.telenav.sdk.demo.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.telenav.sdk.entity.model.base.GeoPoint
import com.telenav.sdk.entity.model.base.SortType
import com.telenav.sdk.entity.model.lookup.GetDetailOptions
import com.telenav.sdk.entity.model.search.BBox
import com.telenav.sdk.entity.model.search.BBoxGeoFilter
import com.telenav.sdk.entity.model.search.CategoryFilter
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.entity.model.search.RadiusGeoFilter
import com.telenav.sdk.entity.model.search.SearchFilters
import com.telenav.sdk.entity.model.search.SearchOptions
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.SearchResultItemDao
import com.telenav.searchservice.SearchService
import com.telenav.searchservice.api.SearchResponseCodes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "SearchViewModel"
        private const val INPUT_DEBOUNCE_MS = 350L
        private const val MAX_WORD_SUGGESTIONS = 5
        private const val CATEGORY_RADIUS_METERS = 2_000
    }

    private val _wordSuggestions = MutableLiveData<List<WordSuggestionItem>>(emptyList())
    val wordSuggestions: LiveData<List<WordSuggestionItem>> = _wordSuggestions

    private val _autocompleteItems = MutableLiveData<List<AutocompleteItem>>(emptyList())
    val autocompleteItems: LiveData<List<AutocompleteItem>> = _autocompleteItems

    private val _searchResults = MutableLiveData<List<SearchResultItemDao>>(emptyList())
    val searchResults: LiveData<List<SearchResultItemDao>> = _searchResults

    private val _searchResultsFromGoogle = MutableLiveData(false)
    val searchResultsFromGoogle: LiveData<Boolean> = _searchResultsFromGoogle

    private val _autocompleteFromGoogle = MutableLiveData(false)
    val autocompleteFromGoogle: LiveData<Boolean> = _autocompleteFromGoogle

    /** True after autocomplete completes successfully with zero displayable items. */
    private val _showAutocompleteEmpty = MutableLiveData(false)
    val showAutocompleteEmpty: LiveData<Boolean> = _showAutocompleteEmpty

    /** True after text/category search completes with zero results. */
    private val _showSearchResultsEmpty = MutableLiveData(false)
    val showSearchResultsEmpty: LiveData<Boolean> = _showSearchResultsEmpty

    private val _selectedLocation = MutableLiveData<SearchResultItemDao>()
    val selectedLocation: LiveData<SearchResultItemDao> = _selectedLocation

    private val _detailItem = MutableLiveData<SearchDetailItem?>(null)
    val detailItem: LiveData<SearchDetailItem?> = _detailItem

    private val _showDetailPanel = MutableLiveData(false)
    val showDetailPanel: LiveData<Boolean> = _showDetailPanel

    private val _detailLoading = MutableLiveData(false)
    val detailLoading: LiveData<Boolean> = _detailLoading

    /** Preview route on map when detail panel opens or detail data loads. */
    private val _routePreview = MutableLiveData<SearchResultItemDao>()
    val routePreview: LiveData<SearchResultItemDao> = _routePreview

    /** One-shot trigger: start turn-by-turn navigation from detail panel. */
    private val _navigationStartRequest = MutableLiveData(0L)
    val navigationStartRequest: LiveData<Long> = _navigationStartRequest

    /** One-shot trigger: end turn-by-turn navigation from detail panel. */
    private val _navigationEndRequest = MutableLiveData(0L)
    val navigationEndRequest: LiveData<Long> = _navigationEndRequest

    private val _detailNavigationActive = MutableLiveData(false)
    val detailNavigationActive: LiveData<Boolean> = _detailNavigationActive

    private val _detailDestinationReached = MutableLiveData(false)
    val detailDestinationReached: LiveData<Boolean> = _detailDestinationReached

    /** One-shot trigger: clear route preview on map when detail panel closes. */
    private val _clearRoutePreview = MutableLiveData(0L)
    val clearRoutePreview: LiveData<Long> = _clearRoutePreview

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _autocompleteLoading = MutableLiveData(false)
    val autocompleteLoading: LiveData<Boolean> = _autocompleteLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /** When true, show word suggestion + autocomplete; when false, show search results list only. */
    private val _showSuggestionPanel = MutableLiveData(true)
    val showSuggestionPanel: LiveData<Boolean> = _showSuggestionPanel

    private val _selectedCategoryId = MutableLiveData<String?>(null)
    val selectedCategoryId: LiveData<String?> = _selectedCategoryId

    private var debounceJob: Job? = null
    private var detailJob: Job? = null
    private var queryGeneration = 0
    private var searchLatitude = 0.0
    private var searchLongitude = 0.0
    private var activeCategoryId: String? = null
    private var activeSearchQuery: String? = null

    fun setSearchCenter(latitude: Double, longitude: Double) {
        if (latitude == searchLatitude && longitude == searchLongitude) return
        searchLatitude = latitude
        searchLongitude = longitude
        Log.d(TAG, "search center updated to ($latitude, $longitude)")
    }

    fun reset() {
        cancelDebounce()
        queryGeneration++
        activeCategoryId = null
        activeSearchQuery = null
        _selectedCategoryId.value = null
        _wordSuggestions.value = emptyList()
        _autocompleteItems.value = emptyList()
        _autocompleteFromGoogle.value = false
        _showAutocompleteEmpty.value = false
        _showSearchResultsEmpty.value = false
        _searchResults.value = emptyList()
        _searchResultsFromGoogle.value = false
        _loading.value = false
        _autocompleteLoading.value = false
        _errorMessage.value = null
        _showSuggestionPanel.value = true
        closeDetailPanel()
    }

    fun onQueryChanged(query: String) {
        val trimmed = query.trim()
        cancelDebounce()
        closeDetailPanel()
        clearCategorySelection()
        enterSuggestionMode()
        val generation = ++queryGeneration
        if (trimmed.isEmpty()) {
            _autocompleteLoading.value = false
            _wordSuggestions.value = emptyList()
            _autocompleteItems.value = emptyList()
            _searchResults.value = emptyList()
            _searchResultsFromGoogle.value = false
            _autocompleteFromGoogle.value = false
            _showAutocompleteEmpty.value = false
            _showSearchResultsEmpty.value = false
            return
        }
        _showAutocompleteEmpty.value = false
        _autocompleteLoading.value = true
        debounceJob = viewModelScope.launch {
            delay(INPUT_DEBOUNCE_MS)
            if (generation != queryGeneration) return@launch
            viewModelScope.launch { fetchWordSuggestions(trimmed, generation) }
            viewModelScope.launch { fetchAutocomplete(trimmed, generation) }
        }
    }

    fun onWordSuggestionSelected(query: String) {
        val trimmed = query.trim()
        Log.i(TAG, "wordSuggestion selected: $trimmed")
        if (trimmed.isEmpty()) return
        cancelDebounce()
        val generation = ++queryGeneration
        enterSuggestionMode()
        _autocompleteItems.value = emptyList()
        _showAutocompleteEmpty.value = false
        _autocompleteLoading.value = true
        viewModelScope.launch {
            fetchAutocomplete(trimmed, generation)
        }
    }

    fun onAutocompleteClicked(item: AutocompleteItem) {
        Log.i(TAG, "autocomplete clicked: ${item.entityId} ${item.label}")
        if (item.entityId.isBlank()) {
            _errorMessage.value = "Missing entity id for detail search"
            return
        }
        openDetailPreview(
            entityId = item.entityId,
            previewName = item.label,
            fromGoogle = item.fromGoogle
        )
        viewModelScope.launch {
            fetchDetailForPanel(item.entityId)
        }
    }

    fun onTextSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _errorMessage.value = "Enter a search query"
            return
        }
        activeSearchQuery = trimmed
        clearCategorySelection()
        beginFullSearch()
        viewModelScope.launch {
            runTextSearch(trimmed)
        }
    }

    fun onRectSearch(bottomLeft: GeoPoint, topRight: GeoPoint) {
        val categoryId = activeCategoryId
        val query = activeSearchQuery?.trim().orEmpty()
        if (categoryId == null && query.isEmpty()) {
            _errorMessage.value = getApplication<Application>().getString(R.string.rect_search_no_context)
            return
        }
        beginFullSearch()
        viewModelScope.launch {
            runRectSearch(query, categoryId, bottomLeft, topRight)
        }
    }

    fun onCategoryButtonClicked(categoryId: String) {
        if (activeCategoryId == categoryId) {
            clearCategorySelection()
            clearCategorySearchResults()
            return
        }
        activeCategoryId = categoryId
        activeSearchQuery = null
        _selectedCategoryId.value = categoryId
        beginFullSearch()
        viewModelScope.launch {
            runCategorySearch(categoryId)
        }
    }

    fun onSearchResultClicked(item: SearchResultItemDao) {
        Log.i(TAG, "search result clicked: ${item.displayText}")
        _detailItem.value = EntitySearchResultMapper.toSearchDetailItemFromListItem(item)
        _showDetailPanel.value = true
        requestRoutePreview(item)
        if (item.entityId.isNotBlank()) {
            _detailLoading.value = true
            detailJob?.cancel()
            detailJob = viewModelScope.launch {
                fetchDetailForPanel(item.entityId)
            }
        } else {
            _detailLoading.value = false
        }
    }

    fun closeDetailPanel(clearRoutePreview: Boolean = true) {
        detailJob?.cancel()
        detailJob = null
        _showDetailPanel.value = false
        _detailLoading.value = false
        _detailNavigationActive.value = false
        _detailDestinationReached.value = false
        if (clearRoutePreview) {
            _clearRoutePreview.value = System.currentTimeMillis()
        }
    }

    fun onDetailNavigateClicked() {
        if (_detailDestinationReached.value == true) {
            closeDetailPanel()
            return
        }
        if (_detailNavigationActive.value == true) {
            _navigationEndRequest.value = System.currentTimeMillis()
        } else {
            startNavigationFromDetail()
        }
    }

    fun onNavigationDestinationReached() {
        _detailNavigationActive.postValue(false)
        _detailDestinationReached.postValue(true)
    }

    fun startNavigationFromDetail() {
        _detailDestinationReached.value = false
        val item = _detailItem.value?.navigationItem ?: return
        requestRoutePreview(item)
        _navigationStartRequest.value = System.currentTimeMillis()
    }

    fun setDetailNavigationActive(active: Boolean) {
        _detailNavigationActive.postValue(active)
    }

    private fun requestRoutePreview(item: SearchResultItemDao) {
        if (!hasValidCoords(item)) return
        _routePreview.value = item
    }

    private fun hasValidCoords(item: SearchResultItemDao): Boolean =
        kotlin.math.abs(item.displayLocation.latitude) > 1e-4 ||
            kotlin.math.abs(item.displayLocation.longitude) > 1e-4

    private fun openDetailPreview(entityId: String, previewName: String, fromGoogle: Boolean) {
        _detailItem.value = SearchDetailItem(
            entityId = entityId,
            name = previewName,
            addressLine = "",
            distanceMiles = 0.0,
            fromGoogle = fromGoogle,
            category = "",
            phone = "",
            hours = "",
            rating = null,
            ratingCount = null,
            priceLevel = "",
            openStatusLabel = "",
            isOpenNow = null,
            closingTimeLabel = "",
            hoursSchedule = "",
            photoUrls = emptyList(),
            reviews = emptyList(),
            navigationItem = SearchResultItemDao(
                displayLocation = android.location.Location("preview"),
                navLocation = null,
                address = null,
                name = previewName,
                addressLine = "",
                detailLine = "",
                displayText = previewName,
                distance = 0.0,
                fromGoogle = fromGoogle,
                entityId = entityId
            )
        )
        _showDetailPanel.value = true
        _detailLoading.value = true
        detailJob?.cancel()
    }

    private fun enterSuggestionMode() {
        _showSuggestionPanel.value = true
        _searchResults.value = emptyList()
    }

    private fun clearCategorySelection() {
        if (activeCategoryId == null) return
        activeCategoryId = null
        _selectedCategoryId.value = null
    }

    private fun clearCategorySearchResults() {
        cancelDebounce()
        queryGeneration++
        closeDetailPanel()
        _searchResults.value = emptyList()
        _searchResultsFromGoogle.value = false
        _autocompleteFromGoogle.value = false
        _showAutocompleteEmpty.value = false
        _showSearchResultsEmpty.value = false
        _loading.value = false
        _autocompleteLoading.value = false
        _showSuggestionPanel.value = true
        _wordSuggestions.value = emptyList()
        _autocompleteItems.value = emptyList()
    }

    private fun cancelDebounce() {
        debounceJob?.cancel()
        debounceJob = null
    }

    private fun beginFullSearch() {
        cancelDebounce()
        closeDetailPanel()
        queryGeneration++
        _autocompleteLoading.value = false
        _wordSuggestions.value = emptyList()
        _autocompleteItems.value = emptyList()
        _autocompleteFromGoogle.value = false
        _showAutocompleteEmpty.value = false
        _showSearchResultsEmpty.value = false
        _searchResults.value = emptyList()
        _searchResultsFromGoogle.value = false
        _showSuggestionPanel.value = false
        _loading.value = true
    }

    private fun isStaleGeneration(generation: Int): Boolean = generation != queryGeneration

    private fun reportSearchError(operation: String, e: Throwable) {
        if (e is CancellationException || e.cause is CancellationException) return
        Log.e(TAG, "$operation failed", e)
        _errorMessage.postValue("$operation failed: ${e.message}")
    }

    private suspend fun fetchWordSuggestions(query: String, generation: Int) {
        if (isStaleGeneration(generation) || !isSearchServiceReady()) return
        try {
            val response = withContext(Dispatchers.IO) {
                SearchService.getClient().wordPredictionRequest()
                    .setQuery(query)
                    .setLocation(GeoPoint(searchLatitude, searchLongitude))
                    .build()
                    .execute()
            }
            if (isStaleGeneration(generation)) return
            logResponse("wordSuggestion", response.code, response.provider, response.operation)
            if (!SearchResponseCodes.isSuccess(response.code)) {
                _wordSuggestions.postValue(emptyList())
                return
            }
            val predictions = response.wordPredictionOrNull()?.results.orEmpty()
            _wordSuggestions.postValue(
                predictions.mapNotNull { EntitySearchResultMapper.toWordSuggestionItem(it) }
                    .take(MAX_WORD_SUGGESTIONS)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isStaleGeneration(generation)) {
                reportSearchError("Word suggestion", e)
            }
        }
    }

    private suspend fun fetchAutocomplete(query: String, generation: Int) {
        if (isStaleGeneration(generation)) return
        if (!isSearchServiceReady()) {
            finishAutocompleteLoading(generation)
            return
        }
        try {
            val response = withContext(Dispatchers.IO) {
                SearchService.getClient().suggestionPredictionRequest()
                    .setQuery(query)
                    .setLocation(GeoPoint(searchLatitude, searchLongitude))
                    .build()
                    .execute()
            }
            if (isStaleGeneration(generation)) return
            logResponse("autocomplete", response.code, response.provider, response.operation)
            if (!SearchResponseCodes.isSuccess(response.code)) {
                _autocompleteItems.postValue(emptyList())
                _autocompleteFromGoogle.postValue(false)
                _showAutocompleteEmpty.postValue(false)
                return
            }
            val suggestions = response.autocompleteOrNull()?.results.orEmpty().filterNotNull()
            val responseFromGoogle = response.provider.equals("google", ignoreCase = true)
            val items = suggestions.mapNotNull {
                EntitySearchResultMapper.toAutocompleteItem(it, responseFromGoogle)
            }
            Log.i(
                TAG,
                "autocomplete at ($searchLatitude, $searchLongitude) mapped " +
                    "${items.size}/${suggestions.size} for \"$query\" provider=${response.provider}"
            )
            _autocompleteItems.postValue(items)
            _autocompleteFromGoogle.postValue(responseFromGoogle && items.isNotEmpty())
            _showAutocompleteEmpty.postValue(items.isEmpty())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isStaleGeneration(generation)) {
                reportSearchError("Autocomplete", e)
            }
        } finally {
            finishAutocompleteLoading(generation)
        }
    }

    private fun finishAutocompleteLoading(generation: Int) {
        if (generation == queryGeneration) {
            _autocompleteLoading.postValue(false)
        }
    }

    private suspend fun runTextSearch(query: String) {
        if (!isSearchServiceReady()) {
            _loading.value = false
            return
        }
        try {
            val response = withContext(Dispatchers.IO) {
                SearchService.getClient().searchRequest()
                    .setQuery(query)
                    .setLocation(GeoPoint(searchLatitude, searchLongitude))
                    .setLimit(20)
                    .setSort(SortType.BEST_MATCH)
                    .setSearchOptions(
                        SearchOptions.builder().setShowAddressLines(true).build()
                    )
                    .build()
                    .execute()
            }
            logResponse("textSearch", response.code, response.provider, response.operation)
            presentEntitySearchResponse(
                response.entitySearchOrNull(),
                "textSearch",
                response.provider
            )
        } catch (e: Exception) {
            Log.e(TAG, "textSearch failed", e)
            _errorMessage.value = "Text search failed: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    private suspend fun runRectSearch(
        query: String,
        categoryId: String?,
        bottomLeft: GeoPoint,
        topRight: GeoPoint,
    ) {
        if (!isSearchServiceReady()) {
            _loading.value = false
            return
        }
        try {
            val bBox = BBox.builder()
                .setBottomLeft(bottomLeft)
                .setTopRight(topRight)
                .build()
            val geoFilter = BBoxGeoFilter.builder(bBox).build()
            val requestBuilder = SearchService.getClient().searchRequest()
                .setLocation(GeoPoint(searchLatitude, searchLongitude))
                .setLimit(20)
                .setSearchOptions(
                    SearchOptions.builder().setShowAddressLines(true).build()
                )
            val operation: String
            if (categoryId != null) {
                val categoryFilter = CategoryFilter.builder()
                    .setCategories(listOf(categoryId))
                    .build()
                val filters = SearchFilters.builder()
                    .setCategoryFilter(categoryFilter)
                    .setGeoFilter(geoFilter)
                    .build()
                requestBuilder.setFilters(filters).setSort(SortType.DISTANCE)
                operation = "categoryBoundingBoxSearch"
            } else {
                val filters = SearchFilters.builder()
                    .setGeoFilter(geoFilter)
                    .build()
                requestBuilder.setQuery(query).setFilters(filters).setSort(SortType.BEST_MATCH)
                operation = "boundingBoxSearch"
            }
            val response = withContext(Dispatchers.IO) {
                requestBuilder.build().execute()
            }
            logResponse(operation, response.code, response.provider, response.operation)
            presentEntitySearchResponse(
                response.entitySearchOrNull(),
                operation,
                response.provider
            )
        } catch (e: Exception) {
            Log.e(TAG, "rectSearch failed", e)
            _errorMessage.value = "Rect search failed: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    private suspend fun runCategorySearch(categoryId: String) {
        if (!isSearchServiceReady()) {
            _loading.value = false
            return
        }
        try {
            val categoryFilter = CategoryFilter.builder()
                .setCategories(listOf(categoryId))
                .build()
            val geoFilter = RadiusGeoFilter.builder(CATEGORY_RADIUS_METERS).build()
            val filters = SearchFilters.builder()
                .setCategoryFilter(categoryFilter)
                .setGeoFilter(geoFilter)
                .build()
            val response = withContext(Dispatchers.IO) {
                SearchService.getClient().searchRequest()
                    .setLocation(GeoPoint(searchLatitude, searchLongitude))
                    .setFilters(filters)
                    .setLimit(20)
                    .setSort(SortType.DISTANCE)
                    .setSearchOptions(
                        SearchOptions.builder().setShowAddressLines(true).build()
                    )
                    .build()
                    .execute()
            }
            logResponse("categorySearch", response.code, response.provider, response.operation)
            presentEntitySearchResponse(
                response.entitySearchOrNull(),
                "categorySearch",
                response.provider
            )
        } catch (e: Exception) {
            Log.e(TAG, "categorySearch failed", e)
            _errorMessage.value = "Category search failed: ${e.message}"
        } finally {
            _loading.value = false
        }
    }

    private suspend fun fetchDetailForPanel(entityId: String) {
        if (!isSearchServiceReady()) {
            _detailLoading.value = false
            return
        }
        Log.d(TAG, "detail search at ($searchLatitude, $searchLongitude) entityId=$entityId")
        try {
            val detailOptions = GetDetailOptions.builder()
                .setDetailLevel(GetDetailOptions.EntityDetailLevel.FULL)
                .setShowAddressLines(true)
                .build()
            val response = withContext(Dispatchers.IO) {
                SearchService.getClient().getDetailRequest()
                    .setEntityIds(listOf(entityId))
                    .setDetailOptions(detailOptions)
                    .setLocation(searchLatitude, searchLongitude)
                    .build()
                    .execute()
            }
            logResponse("detail", response.code, response.provider, response.operation)
            presentDetailResponse(
                response.entitySearchOrNull(),
                response.provider
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "detail search failed", e)
            _errorMessage.value = "Detail search failed: ${e.message}"
        } finally {
            _detailLoading.value = false
        }
    }

    private fun presentDetailResponse(
        response: EntitySearchResponse?,
        provider: String
    ) {
        if (_showDetailPanel.value != true) return
        val entity = response?.results?.firstOrNull()
        if (entity == null) {
            _errorMessage.value = "No detail found"
            return
        }
        val fromGoogle = provider.equals("google", ignoreCase = true)
        val detail = EntitySearchResultMapper.toSearchDetailItem(
            entity,
            searchLatitude,
            searchLongitude,
            fromGoogle
        )
        if (detail != null) {
            _detailItem.value = detail
            requestRoutePreview(detail.navigationItem)
            Log.i(
                TAG,
                "detail panel: photos=${detail.photoUrls.size} reviews=${detail.reviews.size} " +
                    "fuelPrices=${detail.fuelPrices.size} provider=$provider"
            )
            if (detail.fuelPrices.isNotEmpty()) {
                detail.fuelPrices.forEach { price ->
                    Log.i(TAG, "detail fuel price: ${price.displayLine}")
                }
            }
        } else {
            _errorMessage.value = "Could not load place details"
        }
    }

    private fun presentEntitySearchResponse(
        response: EntitySearchResponse?,
        operation: String,
        provider: String
    ) {
        val results = response?.results.orEmpty()
        val fromGoogle = provider.equals("google", ignoreCase = true)
        Log.i(TAG, "$operation: ${results.size} results provider=$provider")
        if (results.isEmpty()) {
            _searchResults.value = emptyList()
            _searchResultsFromGoogle.value = fromGoogle
            _showSearchResultsEmpty.value = true
            _showSuggestionPanel.value = false
            _wordSuggestions.value = emptyList()
            _autocompleteItems.value = emptyList()
            _autocompleteFromGoogle.value = false
            _showAutocompleteEmpty.value = false
            return
        }
        _searchResults.value = results.mapNotNull { entity ->
            EntitySearchResultMapper.toSearchResultItem(
                entity,
                searchLatitude,
                searchLongitude,
                fromGoogle
            )
        }.also { items ->
            val googleCount = items.count { it.fromGoogle }
            Log.i(TAG, "$operation: mapped ${items.size} items, google=$googleCount provider=$provider")
        }
        _searchResultsFromGoogle.value = fromGoogle
        _showSearchResultsEmpty.value = false
        _showSuggestionPanel.value = false
        _wordSuggestions.value = emptyList()
        _autocompleteItems.value = emptyList()
        _autocompleteFromGoogle.value = false
    }

    private fun logResponse(label: String, code: Int, provider: String, operation: String) {
        Log.i(TAG, "$label: code=$code provider=$provider operation=$operation")
    }

    private fun isSearchServiceReady(): Boolean {
        if (!SearchServiceHolder.isInitialized()) {
            _errorMessage.postValue(
                getApplication<Application>().getString(R.string.search_service_not_ready)
            )
            return false
        }
        return try {
            SearchService.getClient()
            true
        } catch (e: IllegalStateException) {
            _errorMessage.postValue(
                getApplication<Application>().getString(R.string.search_service_not_ready)
            )
            false
        }
    }
}
