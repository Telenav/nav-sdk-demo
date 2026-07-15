package com.telenav.sdk.demo.search

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.telenav.sdk.examples.R

/**
 * Quick category filter chips on the map. Tap to search; tap again to deselect and clear results.
 */
class CategoryQuickButtonsController(
    private val root: View,
    private val viewModel: SearchViewModel,
    private val lifecycleOwner: LifecycleOwner
) {
    private val container: LinearLayout = root.findViewById(R.id.categoryQuickButtonsContainer)
    private val buttons = mutableListOf<Button>()
    private var categoryIds: Array<String> = emptyArray()

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

        viewModel.selectedCategoryId.observe(lifecycleOwner) { selectedId ->
            buttons.forEachIndexed { index, button ->
                button.isSelected = categoryIds[index] == selectedId
            }
        }
    }
}
