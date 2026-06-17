package com.telenav.sdk.demo.search

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.telenav.sdk.examples.R

object SearchResultMetaBinder {

    fun formatCategoryPriceLine(category: String, priceLevel: String): String? {
        val parts = buildList {
            if (category.isNotBlank()) add(category)
            if (priceLevel.isNotBlank()) add(priceLevel)
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    fun bindCategoryPriceLine(
        textView: TextView,
        category: String,
        priceLevel: String
    ) {
        val line = formatCategoryPriceLine(category, priceLevel)
        if (line != null) {
            textView.text = line
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
    }

    fun bindOpenHoursRow(
        row: View,
        openStatusView: TextView,
        closingTimeView: TextView,
        openStatusLabel: String,
        isOpenNow: Boolean?,
        closingTimeLabel: String
    ) {
        val context = row.context
        var visible = false

        if (openStatusLabel.isNotBlank()) {
            openStatusView.text = openStatusLabel
            val statusColor = when (isOpenNow) {
                true -> R.color.search_detail_open
                false -> R.color.search_detail_closed
                null -> R.color.search_overlay_text_secondary
            }
            openStatusView.setTextColor(ContextCompat.getColor(context, statusColor))
            openStatusView.visibility = View.VISIBLE
            visible = true
        } else {
            openStatusView.visibility = View.GONE
        }

        if (closingTimeLabel.isNotBlank()) {
            closingTimeView.text = "· $closingTimeLabel"
            closingTimeView.visibility = View.VISIBLE
            visible = true
        } else {
            closingTimeView.visibility = View.GONE
        }

        row.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
