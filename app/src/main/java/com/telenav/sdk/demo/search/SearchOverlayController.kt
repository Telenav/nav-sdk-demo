package com.telenav.sdk.demo.search

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.R

/**
 * Floating search overlay: search bar + dropdown panel (same width). Category chips are separate.
 */
class SearchOverlayController(
    private val root: View,
    private val viewModel: SearchViewModel,
    private val lifecycleOwner: LifecycleOwner
) {
    private val searchEditText: EditText = root.findViewById(R.id.searchEditText)
    private val googleSearchBadge: ImageView = root.findViewById(R.id.googleSearchBadge)
    private val searchPanelCard: View = root.findViewById(R.id.searchPanelCard)
    private val wordSuggestionRecyclerView: RecyclerView =
        root.findViewById(R.id.wordSuggestionRecyclerView)
    private val searchResultsPanel: LinearLayout = root.findViewById(R.id.searchResultsPanel)
    private val searchResultsProviderLabel: View = root.findViewById(R.id.searchResultsProviderLabel)
    private val autocompleteProviderLabel: View = root.findViewById(R.id.autocompleteProviderLabel)
    private val autocompleteRecyclerView: RecyclerView =
        root.findViewById(R.id.autocompleteRecyclerView)
    private val autocompleteEmptyView: View = root.findViewById(R.id.autocompleteEmptyView)
    private val searchRecyclerView: RecyclerView = root.findViewById(R.id.searchRecyclerView)
    private val searchEmptyView: View = root.findViewById(R.id.searchEmptyView)
    private val searchLoadingPanel: View = root.findViewById(R.id.searchLoadingPanel)
    private val listDetailOverlay: View = root.findViewById(R.id.searchDetailOverlay)
    private val suggestionDetailOverlay: View = root.findViewById(R.id.suggestionDetailOverlay)

    private lateinit var listDetailBinder: SearchDetailPanelBinder
    private lateinit var suggestionDetailBinder: SearchDetailPanelBinder

    private val wordSuggestionAdapter = WordSuggestionAdapter { item ->
        suppressQueryChange = true
        searchEditText.setText(item.fullQuery)
        searchEditText.setSelection(item.fullQuery.length)
        suppressQueryChange = false
        withFreshSearchCenter { viewModel.onWordSuggestionSelected(item.fullQuery) }
    }
    private val autocompleteAdapter = AutocompleteAdapter { item ->
        withFreshSearchCenter { viewModel.onAutocompleteClicked(item) }
    }
    private val searchAdapter = SearchAdapter()

    private var suppressQueryChange = false

    /** Sync vehicle location into [SearchViewModel] before each search request. */
    var refreshSearchCenter: (() -> Unit)? = null

    fun bind() {
        listDetailBinder = SearchDetailPanelBinder(
            listDetailOverlay,
            lifecycleOwner.lifecycleScope,
            { viewModel.closeDetailPanel() },
            { viewModel.onDetailNavigateClicked() }
        )
        suggestionDetailBinder = SearchDetailPanelBinder(
            suggestionDetailOverlay,
            lifecycleOwner.lifecycleScope,
            { viewModel.closeDetailPanel() },
            { viewModel.onDetailNavigateClicked() }
        )
        viewModel.reset()
        clearUiState()
        setupRecyclerViews()
        setupSearchInput()
        observeGoogleAvailability()
        observeViewModel()
    }

    private fun clearUiState() {
        suppressQueryChange = true
        searchEditText.text.clear()
        suppressQueryChange = false
        wordSuggestionAdapter.submitList(emptyList())
        autocompleteAdapter.submitList(emptyList())
        searchAdapter.setSearchData(emptyList())
        autocompleteRecyclerView.visibility = View.GONE
        searchResultsPanel.visibility = View.GONE
        searchResultsProviderLabel.visibility = View.GONE
        autocompleteProviderLabel.visibility = View.GONE
        autocompleteEmptyView.visibility = View.GONE
        searchEmptyView.visibility = View.GONE
        wordSuggestionRecyclerView.visibility = View.GONE
        searchLoadingPanel.visibility = View.GONE
        searchPanelCard.visibility = View.GONE
        listDetailBinder.setVisible(false)
        suggestionDetailBinder.setVisible(false)
    }

    private fun setupRecyclerViews() {
        wordSuggestionRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = wordSuggestionAdapter
        }
        autocompleteRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = autocompleteAdapter
            addItemDecoration(createListDividerDecoration())
        }
        searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
            addItemDecoration(createListDividerDecoration())
        }
        searchAdapter.setOnClickListener(object : OnClickedLayoutListener {
            override fun onClickLayout(itemDao: com.telenav.sdk.examples.SearchResultItemDao) {
                viewModel.onSearchResultClicked(itemDao)
            }
        })
    }

    private fun observeGoogleAvailability() {
        val listener = com.telenav.searchservice.api.GoogleSearchAvailabilityListener { available ->
            updateGoogleSearchBadge(available)
        }
        SearchServiceHolder.setGoogleAvailabilityListener(listener)
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                SearchServiceHolder.setGoogleAvailabilityListener(null)
            }
        })
        updateGoogleSearchBadge(SearchServiceHolder.isGoogleSearchAvailable())
    }

    private fun updateGoogleSearchBadge(available: Boolean) {
        googleSearchBadge.visibility = if (available) View.VISIBLE else View.GONE
    }

    private fun setupSearchInput() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!suppressQueryChange) {
                    withFreshSearchCenter {
                        viewModel.onQueryChanged(s?.toString().orEmpty())
                    }
                }
            }
        })
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                withFreshSearchCenter {
                    viewModel.onTextSearch(searchEditText.text.toString())
                }
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        viewModel.wordSuggestions.observe(lifecycleOwner) { items ->
            wordSuggestionAdapter.submitList(items)
            updateWordSuggestionVisibility(items.isNotEmpty())
        }
        viewModel.autocompleteItems.observe(lifecycleOwner) { items ->
            autocompleteAdapter.submitList(items)
            updateAutocompleteUi()
        }
        viewModel.autocompleteFromGoogle.observe(lifecycleOwner) {
            updateAutocompleteGoogleLabel()
        }
        viewModel.showAutocompleteEmpty.observe(lifecycleOwner) {
            updateAutocompleteUi()
        }
        viewModel.searchResults.observe(lifecycleOwner) { items ->
            searchAdapter.setSearchData(items)
            updateGoogleResultsLabel()
            updateSearchEmptyState()
        }
        viewModel.searchResultsFromGoogle.observe(lifecycleOwner) {
            updateGoogleResultsLabel()
        }
        viewModel.showSearchResultsEmpty.observe(lifecycleOwner) {
            updateSearchEmptyState()
        }
        viewModel.showSuggestionPanel.observe(lifecycleOwner) { showSuggestions ->
            updateDetailPanelVisibility(viewModel.showDetailPanel.value == true)
            val showResults = showSuggestions != true || viewModel.loading.value == true
            searchResultsPanel.visibility = if (showResults) View.VISIBLE else View.GONE
            val show = showSuggestions == true
            if (!show) {
                wordSuggestionRecyclerView.visibility = View.GONE
                autocompleteRecyclerView.visibility = View.GONE
                autocompleteProviderLabel.visibility = View.GONE
                autocompleteEmptyView.visibility = View.GONE
            } else {
                updateWordSuggestionVisibility(wordSuggestionAdapter.itemCount > 0)
                updateAutocompleteUi()
            }
            updateSearchEmptyState()
            updateSearchPanelVisibility()
        }
        viewModel.loading.observe(lifecycleOwner) {
            updateLoadingUi()
            updateSearchEmptyState()
        }
        viewModel.autocompleteLoading.observe(lifecycleOwner) {
            updateLoadingUi()
            updateAutocompleteUi()
        }
        viewModel.errorMessage.observe(lifecycleOwner) { message ->
            message?.let {
                Toast.makeText(root.context, it, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.showDetailPanel.observe(lifecycleOwner) { show ->
            updateDetailPanelVisibility(show == true)
            updateAutocompleteUi()
        }
        viewModel.detailItem.observe(lifecycleOwner) { item ->
            listDetailBinder.bindDetail(item)
            suggestionDetailBinder.bindDetail(item)
            updateDetailActionState()
        }
        viewModel.detailLoading.observe(lifecycleOwner) { loading ->
            listDetailBinder.bindLoading(loading == true)
            suggestionDetailBinder.bindLoading(loading == true)
        }
        viewModel.detailNavigationActive.observe(lifecycleOwner) { active ->
            if (viewModel.detailDestinationReached.value == true) return@observe
            listDetailBinder.bindNavigationActive(active == true)
            suggestionDetailBinder.bindNavigationActive(active == true)
        }
        viewModel.detailDestinationReached.observe(lifecycleOwner) { reached ->
            listDetailBinder.bindDestinationReached(reached == true)
            suggestionDetailBinder.bindDestinationReached(reached == true)
        }
    }

    private fun updateDetailActionState() {
        val reached = viewModel.detailDestinationReached.value == true
        val navigating = viewModel.detailNavigationActive.value == true
        if (reached) {
            listDetailBinder.bindDestinationReached(true)
            suggestionDetailBinder.bindDestinationReached(true)
        } else {
            listDetailBinder.bindNavigationActive(navigating)
            suggestionDetailBinder.bindNavigationActive(navigating)
        }
    }

    private fun updateDetailPanelVisibility(show: Boolean) {
        val inSuggestionMode = viewModel.showSuggestionPanel.value == true
        listDetailBinder.setVisible(show && !inSuggestionMode)
        suggestionDetailBinder.setVisible(show && inSuggestionMode)
        updateGoogleResultsLabel()
        updateSearchPanelVisibility()
    }

    private fun updateGoogleResultsLabel() {
        val inResultsMode = viewModel.showSuggestionPanel.value != true
        val detailOpen = viewModel.showDetailPanel.value == true
        val fromGoogle = viewModel.searchResultsFromGoogle.value == true
        val hasResults = searchAdapter.itemCount > 0
        searchResultsProviderLabel.visibility =
            if (inResultsMode && !detailOpen && fromGoogle && hasResults) View.VISIBLE else View.GONE
    }

    private fun updateAutocompleteGoogleLabel() {
        val inSuggestionMode = viewModel.showSuggestionPanel.value == true
        val fromGoogle = viewModel.autocompleteFromGoogle.value == true
        val loading = viewModel.autocompleteLoading.value == true
        val detailOpen = viewModel.showDetailPanel.value == true && inSuggestionMode
        val hasItems = autocompleteAdapter.itemCount > 0
        autocompleteProviderLabel.visibility =
            if (inSuggestionMode && fromGoogle && hasItems && !loading && !detailOpen) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun updateAutocompleteUi() {
        val inSuggestionMode = viewModel.showSuggestionPanel.value == true
        val loading = viewModel.autocompleteLoading.value == true
        val detailOpen = viewModel.showDetailPanel.value == true && inSuggestionMode
        val showEmpty = inSuggestionMode && !loading && !detailOpen &&
            viewModel.showAutocompleteEmpty.value == true
        val hasItems = autocompleteAdapter.itemCount > 0

        autocompleteEmptyView.visibility = if (showEmpty) View.VISIBLE else View.GONE
        autocompleteRecyclerView.visibility =
            if (inSuggestionMode && !loading && !detailOpen && hasItems) View.VISIBLE else View.GONE
        updateAutocompleteGoogleLabel()
        updateSearchPanelVisibility()
    }

    private fun updateSearchEmptyState() {
        val inResultsMode = viewModel.showSuggestionPanel.value != true
        val loading = viewModel.loading.value == true
        val detailOpen = viewModel.showDetailPanel.value == true
        val showEmpty = inResultsMode && !loading && !detailOpen &&
            viewModel.showSearchResultsEmpty.value == true
        val hasItems = searchAdapter.itemCount > 0
        searchEmptyView.visibility = if (showEmpty) View.VISIBLE else View.GONE
        searchRecyclerView.visibility = when {
            showEmpty || (loading && inResultsMode) -> View.GONE
            hasItems -> View.VISIBLE
            else -> View.GONE
        }
        updateSearchPanelVisibility()
    }

    private fun createListDividerDecoration(): DividerItemDecoration {
        val decoration = DividerItemDecoration(
            root.context,
            LinearLayoutManager.VERTICAL
        )
        val divider = GradientDrawable().apply {
            setColor(ContextCompat.getColor(root.context, R.color.search_overlay_divider))
            setSize(-1, 1)
        }
        decoration.setDrawable(divider)
        return decoration
    }

    private fun updateWordSuggestionVisibility(hasItems: Boolean) {
        val inSuggestionMode = viewModel.showSuggestionPanel.value == true
        wordSuggestionRecyclerView.visibility =
            if (inSuggestionMode && hasItems) View.VISIBLE else View.GONE
        updateSearchPanelVisibility()
    }

    private fun withFreshSearchCenter(block: () -> Unit) {
        refreshSearchCenter?.invoke()
        updateGoogleSearchBadge(SearchServiceHolder.isGoogleSearchAvailable())
        block()
    }

    private fun updateLoadingUi() {
        val fullSearchLoading = viewModel.loading.value == true
        val autocompleteLoading = viewModel.autocompleteLoading.value == true
        val showLoading = fullSearchLoading || autocompleteLoading
        searchLoadingPanel.visibility = if (showLoading) View.VISIBLE else View.GONE
        if (fullSearchLoading) {
            searchResultsPanel.visibility = View.VISIBLE
        }
        updateSearchEmptyState()
        updateAutocompleteUi()
        updateSearchPanelVisibility()
    }

    private fun updateSearchPanelVisibility() {
        val showPanel = searchLoadingPanel.visibility == View.VISIBLE ||
            wordSuggestionRecyclerView.visibility == View.VISIBLE ||
            autocompleteProviderLabel.visibility == View.VISIBLE ||
            autocompleteRecyclerView.visibility == View.VISIBLE ||
            autocompleteEmptyView.visibility == View.VISIBLE ||
            searchResultsPanel.visibility == View.VISIBLE
        searchPanelCard.visibility = if (showPanel) View.VISIBLE else View.GONE
    }

    private fun hideKeyboard() {
        val imm = root.context.getSystemService(Activity.INPUT_METHOD_SERVICE)
            as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }
}
