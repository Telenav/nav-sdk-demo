package com.telenav.sdk.demo.search

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.telenav.sdk.examples.R

class SearchDetailPanelBinder(
    private val overlayRoot: View,
    private val scope: LifecycleCoroutineScope,
    private val onClose: () -> Unit,
    private val onNavigate: () -> Unit
) {
    private val closeButton: ImageButton = overlayRoot.findViewById(R.id.detailCloseButton)
    private val providerIcon: ImageView = overlayRoot.findViewById(R.id.detailProviderIcon)
    private val nameView: TextView = overlayRoot.findViewById(R.id.detailName)
    private val loadingBar: ProgressBar = overlayRoot.findViewById(R.id.detailLoadingBar)
    private val ratingRow: View = overlayRoot.findViewById(R.id.detailRatingRow)
    private val ratingScoreView: TextView = overlayRoot.findViewById(R.id.detailRatingScore)
    private val ratingStarsView: TextView = overlayRoot.findViewById(R.id.detailRatingStars)
    private val ratingCountView: TextView = overlayRoot.findViewById(R.id.detailRatingCount)
    private val categoryLineView: TextView = overlayRoot.findViewById(R.id.detailCategoryLine)
    private val openHoursRow: View = overlayRoot.findViewById(R.id.detailOpenHoursRow)
    private val openStatusView: TextView = overlayRoot.findViewById(R.id.detailOpenStatus)
    private val closingTimeView: TextView = overlayRoot.findViewById(R.id.detailClosingTime)
    private val evConnectorsContainer: LinearLayout =
        overlayRoot.findViewById(R.id.detailEvConnectorsContainer)
    private val tabLayout: TabLayout = overlayRoot.findViewById(R.id.detailTabLayout)
    private val overviewPanel: View = overlayRoot.findViewById(R.id.detailOverviewPanel)
    private val reviewsPanel: View = overlayRoot.findViewById(R.id.detailReviewsPanel)
    private val photoRecyclerView: RecyclerView = overlayRoot.findViewById(R.id.detailPhotoRecyclerView)
    private val overviewInfoContainer: LinearLayout =
        overlayRoot.findViewById(R.id.detailOverviewInfoContainer)
    private val reviewsEmptyView: TextView = overlayRoot.findViewById(R.id.detailReviewsEmpty)
    private val reviewsContainer: LinearLayout = overlayRoot.findViewById(R.id.detailReviewsContainer)
    private val navigateButton: MaterialButton = overlayRoot.findViewById(R.id.detailNavigateButton)
    private val destinationReachedMessage: TextView =
        overlayRoot.findViewById(R.id.detailDestinationReachedMessage)

    private val photoAdapter = DetailPhotoAdapter(scope)
    private val inflater = LayoutInflater.from(overlayRoot.context)

    init {
        photoRecyclerView.layoutManager =
            LinearLayoutManager(overlayRoot.context, LinearLayoutManager.HORIZONTAL, false)
        photoRecyclerView.adapter = photoAdapter
        closeButton.setOnClickListener { onClose() }
        navigateButton.setOnClickListener { onNavigate() }
        setupTabs()
    }

    private fun setupTabs() {
        if (tabLayout.tabCount > 0) return
        tabLayout.addTab(tabLayout.newTab().setText(R.string.search_detail_overview))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.search_detail_reviews))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showTab(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun showTab(index: Int) {
        overviewPanel.visibility = if (index == TAB_OVERVIEW) View.VISIBLE else View.GONE
        reviewsPanel.visibility = if (index == TAB_REVIEWS) View.VISIBLE else View.GONE
    }

    fun setVisible(visible: Boolean) {
        overlayRoot.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun bindLoading(loading: Boolean) {
        loadingBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    fun bindNavigationActive(isActive: Boolean) {
        bindDetailActionState(isActive, destinationReached = false)
    }

    fun bindDestinationReached(reached: Boolean) {
        bindDetailActionState(navigationActive = false, destinationReached = reached)
    }

    private fun bindDetailActionState(navigationActive: Boolean, destinationReached: Boolean) {
        when {
            destinationReached -> {
                destinationReachedMessage.visibility = View.VISIBLE
                navigateButton.setText(R.string.search_detail_dismiss)
                navigateButton.isEnabled = true
                navigateButton.alpha = 1f
            }
            navigationActive -> {
                destinationReachedMessage.visibility = View.GONE
                navigateButton.setText(R.string.end_navigation)
            }
            else -> {
                destinationReachedMessage.visibility = View.GONE
                navigateButton.setText(R.string.start_navigation)
            }
        }
    }

    fun bindDetail(item: SearchDetailItem?) {
        if (item == null) return
        tabLayout.getTabAt(TAB_OVERVIEW)?.select()
        showTab(TAB_OVERVIEW)

        providerIcon.setImageResource(
            if (item.fromGoogle) R.drawable.ic_google_maps_pin else R.drawable.ic_tn_place_pin
        )
        nameView.text = item.name

        if (item.rating != null && item.rating > 0) {
            ratingScoreView.text = RatingDisplay.formatScore(item.rating)
            RatingDisplay.applyStars(ratingStarsView, item.rating)
            val count = item.ratingCount
            if (count != null && count > 0) {
                ratingCountView.text = "($count)"
                ratingCountView.visibility = View.VISIBLE
            } else {
                ratingCountView.visibility = View.GONE
            }
            ratingRow.visibility = View.VISIBLE
        } else {
            ratingRow.visibility = View.GONE
        }

        bindCategoryAndPriceRow(item)
        bindOpenHoursRow(item)
        EvConnectorRowBinder.bind(evConnectorsContainer, item.evConnectors)

        if (item.photoUrls.isNotEmpty()) {
            photoAdapter.submitUrls(item.photoUrls)
            photoRecyclerView.visibility = View.VISIBLE
        } else {
            photoAdapter.submitUrls(emptyList())
            photoRecyclerView.visibility = View.GONE
        }

        bindOverviewInfo(item)

        reviewsContainer.removeAllViews()
        if (item.reviews.isNotEmpty()) {
            reviewsEmptyView.visibility = View.GONE
            item.reviews.take(10).forEach { review ->
                val row = inflater.inflate(R.layout.detail_review_row, reviewsContainer, false)
                row.findViewById<TextView>(R.id.reviewAuthor).text = review.author
                val reviewRatingView = row.findViewById<TextView>(R.id.reviewRating)
                if (review.rating > 0) {
                    RatingDisplay.applyStars(reviewRatingView, review.rating)
                    reviewRatingView.visibility = View.VISIBLE
                } else {
                    reviewRatingView.visibility = View.GONE
                }
                row.findViewById<TextView>(R.id.reviewTime).text = review.timeLabel
                row.findViewById<TextView>(R.id.reviewText).text = review.text
                reviewsContainer.addView(row)
            }
        } else {
            reviewsEmptyView.visibility = View.VISIBLE
        }

        val hasCoords = item.navigationItem.displayLocation.latitude != 0.0 ||
            item.navigationItem.displayLocation.longitude != 0.0
        if (destinationReachedMessage.visibility != View.VISIBLE) {
            navigateButton.isEnabled = hasCoords
            navigateButton.alpha = if (hasCoords) 1f else 0.5f
        }
    }

    private fun bindOverviewInfo(item: SearchDetailItem) {
        overviewInfoContainer.removeAllViews()
        val context = overlayRoot.context

        if (item.distanceMiles > 0) {
            addInfoRow(
                context.getString(R.string.search_detail_label_distance),
                context.getString(R.string.search_detail_distance_mi, item.distanceMiles)
            )
        }
        if (item.addressLine.isNotBlank()) {
            addInfoRow(
                context.getString(R.string.search_detail_label_address),
                item.addressLine
            )
        }
        if (item.phone.isNotBlank()) {
            addInfoRow(context.getString(R.string.search_detail_label_phone), item.phone)
        }
        if (item.fuelPrices.isNotEmpty()) {
            addInfoRow(
                context.getString(R.string.search_detail_label_fuel_prices),
                item.fuelPrices.joinToString("\n") { it.displayLine }
            )
        }
        val schedule = item.hoursSchedule.ifBlank { item.hours }
        if (schedule.isNotBlank() &&
            item.openStatusLabel.isBlank() &&
            item.closingTimeLabel.isBlank()
        ) {
            addInfoRow(context.getString(R.string.search_detail_label_hours), schedule)
        } else if (schedule.isNotBlank() && schedule.contains("\n")) {
            addInfoRow(context.getString(R.string.search_detail_label_hours), schedule)
        }
    }

    private fun bindCategoryAndPriceRow(item: SearchDetailItem) {
        val parts = buildList {
            if (item.category.isNotBlank()) add(item.category)
            if (item.priceLevel.isNotBlank()) add(item.priceLevel)
        }
        if (parts.isNotEmpty()) {
            categoryLineView.text = parts.joinToString(" · ")
            categoryLineView.visibility = View.VISIBLE
        } else {
            categoryLineView.visibility = View.GONE
        }
    }

    private fun bindOpenHoursRow(item: SearchDetailItem) {
        val context = overlayRoot.context
        var hasOpenHours = false

        if (item.openStatusLabel.isNotBlank()) {
            openStatusView.text = item.openStatusLabel
            val statusColor = when (item.isOpenNow) {
                true -> R.color.search_detail_open
                false -> R.color.search_detail_closed
                null -> R.color.search_overlay_text_secondary
            }
            openStatusView.setTextColor(ContextCompat.getColor(context, statusColor))
            openStatusView.visibility = View.VISIBLE
            hasOpenHours = true
        } else {
            openStatusView.visibility = View.GONE
        }

        if (item.closingTimeLabel.isNotBlank()) {
            closingTimeView.text = "· ${item.closingTimeLabel}"
            closingTimeView.visibility = View.VISIBLE
            hasOpenHours = true
        } else {
            closingTimeView.visibility = View.GONE
        }

        openHoursRow.visibility = if (hasOpenHours) View.VISIBLE else View.GONE
    }

    private fun addInfoRow(label: String, value: String) {
        val row = inflater.inflate(R.layout.detail_info_row, overviewInfoContainer, false)
        row.findViewById<TextView>(R.id.infoLabel).text = label
        row.findViewById<TextView>(R.id.infoValue).text = value
        overviewInfoContainer.addView(row)
    }

    companion object {
        private const val TAB_OVERVIEW = 0
        private const val TAB_REVIEWS = 1
    }
}
