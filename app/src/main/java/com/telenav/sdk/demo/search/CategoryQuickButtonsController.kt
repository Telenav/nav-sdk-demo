package com.telenav.sdk.demo.search

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.telenav.sdk.examples.R

/**
 * Quick category filter chips on the map. Tap to search; tap again to deselect and clear results.
 * When EV charging (771) is selected, shows multi-select connector types and min-power filter.
 */
class CategoryQuickButtonsController(
    private val root: View,
    private val viewModel: SearchViewModel,
    private val lifecycleOwner: LifecycleOwner
) {
    private val container: LinearLayout = root.findViewById(R.id.categoryQuickButtonsContainer)
    private val evFiltersPanel: View = root.findViewById(R.id.evCategoryFiltersPanel)
    private val connectorTypeContainer: LinearLayout = root.findViewById(R.id.evConnectorTypeContainer)
    private val powerSpinner: Spinner = root.findViewById(R.id.evMinPowerSpinner)
    private val buttons = mutableListOf<Button>()
    private val connectorTypeButtons = mutableListOf<Button>()
    private var connectorTypeIds: Array<String> = emptyArray()
    private var categoryIds: Array<String> = emptyArray()
    private var suppressFilterCallbacks = false

    var refreshSearchCenter: (() -> Unit)? = null

    fun bind() {
        val context = root.context
        val labels = context.resources.getStringArray(R.array.search_category_labels)
        categoryIds = context.resources.getStringArray(R.array.search_category_ids)
        container.removeAllViews()
        buttons.clear()

        val margin = context.resources.getDimensionPixelSize(R.dimen.dimens_5dp)
        val paddingH = context.resources.getDimensionPixelSize(R.dimen.dimens_10dp)
        val paddingV = context.resources.getDimensionPixelSize(R.dimen.dimens_5dp)

        labels.forEachIndexed { index, label ->
            val button = Button(context).apply {
                text = label
                isAllCaps = false
                textSize = 13f
                isSelected = false
                backgroundTintList = null
                setBackgroundResource(R.drawable.category_quick_button_bg)
                setTextColor(
                    ContextCompat.getColorStateList(context, R.color.category_quick_button_text)
                )
                setPadding(paddingH, paddingV, paddingH, paddingV)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = margin
                }
                setOnClickListener {
                    refreshSearchCenter?.invoke()
                    viewModel.onCategoryButtonClicked(categoryIds[index])
                }
            }
            container.addView(button)
            buttons.add(button)
        }

        setupEvFilters()

        viewModel.selectedCategoryId.observe(lifecycleOwner) { selectedId ->
            buttons.forEachIndexed { index, button ->
                button.isSelected = categoryIds[index] == selectedId
            }
        }
        viewModel.showEvChargingFilters.observe(lifecycleOwner) { show ->
            evFiltersPanel.visibility = if (show == true) View.VISIBLE else View.GONE
        }
    }

    private fun setupEvFilters() {
        val context = root.context
        connectorTypeIds = context.resources.getStringArray(R.array.ev_filter_connector_ids)
        val connectorLabels = context.resources.getStringArray(R.array.ev_filter_connector_labels)
        val powerLabels = context.resources.getStringArray(R.array.ev_filter_power_labels)

        connectorTypeContainer.removeAllViews()
        connectorTypeButtons.clear()

        val margin = context.resources.getDimensionPixelSize(R.dimen.dimens_5dp)
        val paddingH = context.resources.getDimensionPixelSize(R.dimen.dimens_10dp)
        val paddingV = context.resources.getDimensionPixelSize(R.dimen.dimens_5dp)

        connectorLabels.forEachIndexed { index, label ->
            val typeId = connectorTypeIds.getOrNull(index) ?: return@forEachIndexed
            val button = Button(context).apply {
                text = label
                isAllCaps = false
                textSize = 12f
                isSelected = false
                backgroundTintList = null
                setBackgroundResource(R.drawable.category_quick_button_bg)
                setTextColor(
                    ContextCompat.getColorStateList(context, R.color.category_quick_button_text)
                )
                setPadding(paddingH, paddingV, paddingH, paddingV)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = margin
                }
                setOnClickListener {
                    if (suppressFilterCallbacks) return@setOnClickListener
                    toggleConnectorType(typeId)
                }
            }
            connectorTypeContainer.addView(button)
            connectorTypeButtons.add(button)
        }

        val powerAdapter = ArrayAdapter(
            context,
            R.layout.spinner_item_light,
            powerLabels
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_light)
        }
        powerSpinner.adapter = powerAdapter
        powerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (suppressFilterCallbacks) return
                publishEvFilterFromUi()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        viewModel.evChargingFilter.observe(lifecycleOwner) { filter ->
            syncUiToFilter(filter)
        }
    }

    private fun toggleConnectorType(typeId: String) {
        val current = viewModel.evChargingFilter.value ?: EvChargingSearchFilter()
        val selected = current.connectorTypeIds.toMutableSet()
        if (selected.contains(typeId)) {
            selected.remove(typeId)
        } else {
            selected.add(typeId)
        }
        refreshSearchCenter?.invoke()
        viewModel.onEvChargingFilterChanged(
            current.copy(connectorTypeIds = selected.toList())
        )
    }

    private fun publishEvFilterFromUi() {
        if (viewModel.showEvChargingFilters.value != true) return
        val context = root.context
        val powerValues = context.resources.getStringArray(R.array.ev_filter_power_min_kw)
        val powerIndex = powerSpinner.selectedItemPosition.coerceIn(0, powerValues.size - 1)
        val minPower = powerValues[powerIndex].trim().toDoubleOrNull()
        val current = viewModel.evChargingFilter.value ?: EvChargingSearchFilter()
        refreshSearchCenter?.invoke()
        viewModel.onEvChargingFilterChanged(current.copy(minPowerKw = minPower))
    }

    private fun syncUiToFilter(filter: EvChargingSearchFilter) {
        val context = root.context
        val powerValues = context.resources.getStringArray(R.array.ev_filter_power_min_kw)
        val selectedIds = filter.connectorTypeIds.toSet()
        suppressFilterCallbacks = true
        connectorTypeIds.forEachIndexed { index, typeId ->
            connectorTypeButtons.getOrNull(index)?.isSelected = selectedIds.contains(typeId)
        }
        val powerIndex = powerValues.indexOfFirst { value ->
            val kw = value.trim().toDoubleOrNull()
            kw == filter.minPowerKw || (kw == null && filter.minPowerKw == null)
        }.takeIf { it >= 0 } ?: 0
        if (powerSpinner.selectedItemPosition != powerIndex) {
            powerSpinner.setSelection(powerIndex)
        }
        suppressFilterCallbacks = false
    }
}
