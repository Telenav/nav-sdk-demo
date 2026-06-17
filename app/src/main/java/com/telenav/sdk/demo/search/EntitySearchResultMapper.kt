package com.telenav.sdk.demo.search

import android.location.Location
import com.telenav.sdk.common.model.Earth.distance
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType
import com.telenav.sdk.entity.model.prediction.Suggestion
import com.telenav.sdk.entity.model.prediction.WordPrediction
import com.telenav.sdk.examples.SearchResultItemDao
import com.telenav.sdk.map.direction.model.Address

object EntitySearchResultMapper {

    private const val METERS_PER_MILE = 1609.344

    fun toWordSuggestionItem(prediction: WordPrediction): WordSuggestionItem? {
        val suffix = prediction.predictWord?.trim().orEmpty()
        if (suffix.isEmpty()) return null
        val prefix = prediction.activeWord?.trim().orEmpty()
        val fullQuery = prefix + suffix
        return WordSuggestionItem(displayText = fullQuery, fullQuery = fullQuery)
    }

    fun toAutocompleteItem(
        suggestion: Suggestion,
        responseFromGoogle: Boolean = false
    ): AutocompleteItem? {
        val label = suggestion.formattedLabel?.trim()?.takeIf { it.isNotEmpty() }
            ?: suggestion.entity?.label?.trim()?.takeIf { it.isNotEmpty() }
            ?: suggestion.entity?.place?.name?.trim()?.takeIf { it.isNotEmpty() }
            ?: suggestion.query?.trim()?.takeIf { isUserFacingQuery(it) }
            ?: return null
        val entityId = suggestion.entity?.id?.trim()?.takeIf { it.isNotEmpty() }
            ?: suggestion.id?.trim()?.takeIf { it.isNotEmpty() }
            ?: ""
        val fromGoogle = responseFromGoogle ||
            SearchProviderIcon.isGoogleId(entityId) ||
            SearchProviderIcon.isGoogleId(suggestion.id) ||
            SearchProviderIcon.isGoogleEntity(suggestion.entity)
        return AutocompleteItem(entityId = entityId, label = label, fromGoogle = fromGoogle)
    }

    /** Google UI-kit may return internal metadata in `query`, e.g. `source=ui;position=1;`. */
    private fun isUserFacingQuery(query: String): Boolean =
        query.isNotEmpty() && !query.startsWith("source=")

    fun toSearchResultItem(
        entity: Entity,
        originLat: Double,
        originLon: Double,
        fromGoogle: Boolean = false
    ): SearchResultItemDao? {
        var name = ""
        val displayLocation = Location("")
        var navLocation: Location? = null
        var geoAddress: Address? = null
        var addressLine = ""

        when (entity.type) {
            EntityType.ADDRESS -> {
                entity.address?.let { address ->
                    name = entity.label?.trim().orEmpty()
                    addressLine = formatAddressLines(address.formattedAddress, address.addressLines)
                    address.geoCoordinates?.let {
                        displayLocation.latitude = it.latitude
                        displayLocation.longitude = it.longitude
                    }
                    address.navCoordinates?.let {
                        navLocation = Location("").apply {
                            latitude = it.latitude
                            longitude = it.longitude
                        }
                    }
                    geoAddress = Address(
                        address.street?.formattedName,
                        address.crossStreet?.formattedName,
                        address.houseNumber
                    )
                }
            }
            EntityType.PLACE -> {
                entity.place?.let { p ->
                    name = p.name?.trim().orEmpty()
                        .ifBlank { entity.label?.trim().orEmpty() }
                    addressLine = formatAddressLines(
                        p.address?.formattedAddress,
                        p.address?.addressLines
                    )
                    p.address?.geoCoordinates?.let { coords ->
                        displayLocation.latitude = coords.latitude
                        displayLocation.longitude = coords.longitude
                    }
                    p.address?.navCoordinates?.let { coords ->
                        navLocation = Location("").apply {
                            latitude = coords.latitude
                            longitude = coords.longitude
                        }
                    }
                    p.address?.let { addr ->
                        geoAddress = Address(
                            addr.street?.formattedName,
                            addr.crossStreet?.formattedName,
                            addr.houseNumber
                        )
                    }
                }
            }
            else -> return null
        }

        if (displayLocation.latitude == 0.0 && displayLocation.longitude == 0.0) {
            return null
        }

        val distMeters = resolveDistanceMeters(
            originLat,
            originLon,
            displayLocation.latitude,
            displayLocation.longitude
        )
        val dist = metersToMiles(distMeters)
        val rating = entity.facets?.rating?.firstOrNull()?.averageRating
        val hoursSummary = OpenHoursDisplay.summarize(entity)
        val detailLine = buildDetailLine(entity)
        val evConnectors = EvConnectorExtractor.connectors(entity)
        val displayText = name.ifBlank { addressLine.lineSequence().firstOrNull().orEmpty() }

        return SearchResultItemDao(
            displayLocation = displayLocation,
            navLocation = navLocation ?: displayLocation,
            address = geoAddress,
            name = name,
            addressLine = addressLine,
            detailLine = detailLine,
            displayText = displayText.ifBlank { "Destination" },
            distance = dist,
            fromGoogle = fromGoogle || SearchProviderIcon.isGoogleEntity(entity),
            entityId = entity.id?.trim().orEmpty(),
            rating = rating,
            category = entity.place?.categories?.firstOrNull()?.name?.trim().orEmpty(),
            priceLevel = EntityFacetExtractor.priceLevel(entity),
            openStatusLabel = hoursSummary?.statusLabel.orEmpty(),
            isOpenNow = hoursSummary?.isOpen,
            closingTimeLabel = hoursSummary?.closingLabel.orEmpty(),
            evConnectors = evConnectors
        )
    }

    fun toSearchDetailItem(
        entity: Entity,
        originLat: Double,
        originLon: Double,
        fromGoogle: Boolean = false
    ): SearchDetailItem? {
        val navigationItem = toSearchResultItem(entity, originLat, originLon, fromGoogle) ?: return null
        val hoursSummary = OpenHoursDisplay.summarize(entity)
        return SearchDetailItem(
            entityId = entity.id?.trim().orEmpty(),
            name = navigationItem.name.ifBlank { navigationItem.displayText },
            addressLine = navigationItem.addressLine,
            distanceMiles = navigationItem.distance,
            fromGoogle = navigationItem.fromGoogle,
            category = entity.place?.categories?.firstOrNull()?.name?.trim().orEmpty(),
            phone = entity.place?.phoneNumbers?.firstOrNull { it.isNotBlank() }?.trim().orEmpty(),
            hours = hoursSummary?.fullSchedule?.ifBlank {
                entity.facets?.openHours?.displayText?.trim().orEmpty()
            }.orEmpty(),
            rating = EntityFacetExtractor.averageRating(entity),
            ratingCount = EntityFacetExtractor.ratingCount(entity),
            priceLevel = EntityFacetExtractor.priceLevel(entity),
            openStatusLabel = hoursSummary?.statusLabel.orEmpty(),
            isOpenNow = hoursSummary?.isOpen,
            closingTimeLabel = hoursSummary?.closingLabel.orEmpty(),
            hoursSchedule = hoursSummary?.fullSchedule.orEmpty(),
            photoUrls = EntityFacetExtractor.photoUrls(entity),
            reviews = EntityFacetExtractor.reviews(entity),
            evConnectors = navigationItem.evConnectors,
            navigationItem = navigationItem
        )
    }

    fun toSearchDetailItemFromListItem(item: SearchResultItemDao): SearchDetailItem =
        SearchDetailItem(
            entityId = item.entityId,
            name = item.name.ifBlank { item.displayText },
            addressLine = item.addressLine,
            distanceMiles = item.distance,
            fromGoogle = item.fromGoogle,
            category = item.category,
            phone = item.detailLine,
            hours = "",
            rating = item.rating,
            ratingCount = null,
            priceLevel = item.priceLevel,
            openStatusLabel = item.openStatusLabel,
            isOpenNow = item.isOpenNow,
            closingTimeLabel = item.closingTimeLabel,
            hoursSchedule = "",
            photoUrls = emptyList(),
            reviews = emptyList(),
            evConnectors = item.evConnectors,
            navigationItem = item
        )

    /**
     * Straight-line (haversine) distance from the current search center to the POI.
     *
     * We always compute locally from [originLat]/[originLon] and destination coordinates.
     * API [Entity.distance] is not used because Google detail responses do not receive the
     * vehicle location (distance is often 0 or based on a stale center).
     */
    private fun resolveDistanceMeters(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double
    ): Double {
        if (!isPlausibleCoordinate(originLat, originLon) ||
            !isPlausibleCoordinate(destLat, destLon)
        ) {
            return 0.0
        }
        return distance(LatLon(originLat, originLon), LatLon(destLat, destLon))
    }

    private fun isPlausibleCoordinate(latitude: Double, longitude: Double): Boolean =
        kotlin.math.abs(latitude) > 1e-4 || kotlin.math.abs(longitude) > 1e-4

    private fun metersToMiles(meters: Double): Double =
        if (meters <= 0) 0.0 else ("%.2f".format(meters / METERS_PER_MILE)).toDouble()

    private fun formatAddressLines(
        formattedAddress: String?,
        addressLines: List<String?>?
    ): String {
        val lines = addressLines?.mapNotNull { it?.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        if (lines.isNotEmpty()) {
            return lines.joinToString("\n")
        }
        return formattedAddress?.trim().orEmpty()
    }

    private fun buildDetailLine(entity: Entity): String {
        return entity.place?.phoneNumbers?.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }
}
