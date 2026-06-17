package com.telenav.sdk.demo.search

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.telenav.sdk.examples.R

object EvConnectorRowBinder {

    fun bind(container: LinearLayout, connectors: List<EvConnectorItem>) {
        container.removeAllViews()
        if (connectors.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        val inflater = LayoutInflater.from(container.context)
        connectors.forEach { item ->
            val row = inflater.inflate(R.layout.ev_connector_row, container, false)
            row.findViewById<TextView>(R.id.evConnectorSummary).text = item.summaryLine
            row.findViewById<TextView>(R.id.evConnectorAvailability).text =
                formatAvailability(item.available, item.total)
            container.addView(row)
        }
        container.visibility = View.VISIBLE
    }

    private fun formatAvailability(available: Int, total: Int): String {
        if (total > 0) return "$available/$total"
        if (available > 0) return available.toString()
        return ""
    }
}
