package com.telenav.sdk.demo.search

import android.content.Context
import com.telenav.sdk.examples.R
import com.telenav.searchservice.api.GoogleSearchAvailabilityState
import com.telenav.searchservice.api.GoogleSearchUnavailabilityReason

object GoogleSearchAvailabilityDisplay {

    fun formatStatus(context: Context, state: GoogleSearchAvailabilityState): String {
        if (state.available) {
            return context.getString(R.string.google_search_available)
        }
        val reasonLabel = context.getString(reasonStringRes(state.reason))
        val detail = state.detail?.trim()?.takeIf { it.isNotEmpty() }
        return if (detail != null) {
            context.getString(R.string.google_search_unavailable_with_detail, reasonLabel, detail)
        } else {
            context.getString(R.string.google_search_unavailable, reasonLabel)
        }
    }

    private fun reasonStringRes(reason: GoogleSearchUnavailabilityReason): Int = when (reason) {
        GoogleSearchUnavailabilityReason.AVAILABLE -> R.string.google_unavail_reason_available
        GoogleSearchUnavailabilityReason.NETWORK_DISCONNECTED -> R.string.google_unavail_reason_network_disconnected
        GoogleSearchUnavailabilityReason.GOOGLE_SERVICES_UNREACHABLE ->
            R.string.google_unavail_reason_google_services_unreachable
        GoogleSearchUnavailabilityReason.CONFIG_PROJECTS_JSON_FAILED ->
            R.string.google_unavail_reason_config_projects_json_failed
        GoogleSearchUnavailabilityReason.CONFIG_ASSEMBLE_FAILED ->
            R.string.google_unavail_reason_config_assemble_failed
        GoogleSearchUnavailabilityReason.HTML_LOAD_FAILED ->
            R.string.google_unavail_reason_html_load_failed
        GoogleSearchUnavailabilityReason.WEBVIEW_VERSION_TOO_LOW ->
            R.string.google_unavail_reason_webview_version_too_low
        GoogleSearchUnavailabilityReason.WEBVIEW_NOT_READY ->
            R.string.google_unavail_reason_webview_not_ready
        GoogleSearchUnavailabilityReason.REGION_BANNED ->
            R.string.google_unavail_reason_region_banned
        GoogleSearchUnavailabilityReason.CONTROL_DISABLED ->
            R.string.google_unavail_reason_control_disabled
        GoogleSearchUnavailabilityReason.COUNTRY_UNKNOWN ->
            R.string.google_unavail_reason_country_unknown
        GoogleSearchUnavailabilityReason.GOOGLE_SEARCH_DISABLED ->
            R.string.google_unavail_reason_google_search_disabled
        GoogleSearchUnavailabilityReason.UNKNOWN -> R.string.google_unavail_reason_unknown
    }
}
