package com.telenav.sdk.demo.search

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.telenav.sdk.examples.databinding.FragmentSearchResultBinding

class SearchResultFragment : Fragment() {

    companion object {
        private const val ARG_LAT = "lat"
        private const val ARG_LNG = "lng"

        fun newInstance(latitude: Double, longitude: Double): SearchResultFragment {
            return SearchResultFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_LAT, latitude)
                    putDouble(ARG_LNG, longitude)
                }
            }
        }
    }

    private var _binding: FragmentSearchResultBinding? = null
    private val binding get() = _binding!!

    private val searchLocationViewModel: SharedSearchLocationViewModel by activityViewModels()
    private val searchViewModel: SearchViewModel by viewModels()

    private lateinit var wordSuggestionAdapter: WordSuggestionAdapter
    private lateinit var autocompleteAdapter: AutocompleteAdapter
    private lateinit var searchAdapter: SearchAdapter

    private var latitude = 0.0
    private var longitude = 0.0
    private var suppressQueryChange = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        latitude = arguments?.getDouble(ARG_LAT) ?: 0.0
        longitude = arguments?.getDouble(ARG_LNG) ?: 0.0
        _binding = FragmentSearchResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchViewModel.setSearchCenter(latitude, longitude)
        setupAdapters()
        setupRecyclerViews()
        setupSearchInput()
        observeViewModel()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupAdapters() {
        wordSuggestionAdapter = WordSuggestionAdapter { item ->
            suppressQueryChange = true
            binding.searchEditText.setText(item.fullQuery)
            binding.searchEditText.setSelection(item.fullQuery.length)
            suppressQueryChange = false
            searchViewModel.onWordSuggestionSelected(item.fullQuery)
        }
        autocompleteAdapter = AutocompleteAdapter { item ->
            searchViewModel.onAutocompleteClicked(item)
        }
        searchAdapter = SearchAdapter()
        searchAdapter.setOnClickListener(object : OnClickedLayoutListener {
            override fun onClickLayout(itemDao: com.telenav.sdk.examples.SearchResultItemDao) {
                searchViewModel.onSearchResultClicked(itemDao)
            }
        })
    }

    private fun setupRecyclerViews() {
        binding.wordSuggestionRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = wordSuggestionAdapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
            )
        }
        binding.autocompleteRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = autocompleteAdapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
            )
        }
        binding.searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
            )
        }
    }

    private fun setupSearchInput() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!suppressQueryChange) {
                    searchViewModel.onQueryChanged(s?.toString().orEmpty())
                }
            }
        })

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchText = binding.searchEditText.text.toString()
                searchViewModel.onTextSearch(searchText)
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        searchViewModel.wordSuggestions.observe(viewLifecycleOwner) { items ->
            wordSuggestionAdapter.submitList(items)
        }
        searchViewModel.autocompleteItems.observe(viewLifecycleOwner) { items ->
            autocompleteAdapter.submitList(items)
        }
        searchViewModel.searchResults.observe(viewLifecycleOwner) { items ->
            searchAdapter.setSearchData(items)
        }
        searchViewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.searchProgressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        searchViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        searchViewModel.selectedLocation.observe(viewLifecycleOwner) { item ->
            item?.let {
                searchLocationViewModel.mutableSelectedLocation.postValue(it)
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE)
            as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }
}
