/*
 * Copyright © 2025 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.search

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.telenav.sdk.examples.databinding.FragmentSearchResultBinding


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SearchResultFragment : Fragment() {
    private var _binding: FragmentSearchResultBinding? = null
    private val binding get() = _binding!!

    private var adapter: SearchAdapter = SearchAdapter()
    private val searchLocationViewModel: SharedSearchLocationViewModel by activityViewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    var lati: Double = 0.0
    var lngi: Double = 0.0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        arguments?.let { arg ->
            lati = arg.getDouble("lat")
            lngi = arg.getDouble("lng")
        }
        _binding = FragmentSearchResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchViewModel.setAdapter(adapter)
        setupSearchObserver(lati, lngi)
        binding.searchRecyclerView.adapter = adapter
        binding.searchRecyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        binding.searchRecyclerView.addItemDecoration(
            DividerItemDecoration(
                activity,
                LinearLayoutManager.VERTICAL
            )
        )
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupSearchObserver(latitude: Double, longitude: Double) {
        // Search text observation
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchText =  binding.searchEditText.text.toString()
                if (searchText.isNotEmpty()) {
                    searchViewModel.entitySearch(searchText, latitude, longitude)
                }
                if (context != null) {
                    context?.let { con ->
                        {
                            val imm = con.getSystemService(Activity.INPUT_METHOD_SERVICE)
                                    as InputMethodManager
                            imm.apply {
                                toggleSoftInput(
                                    InputMethodManager.SHOW_FORCED,
                                    InputMethodManager.HIDE_IMPLICIT_ONLY
                                )
                            }
                        }
                    }
                }

            }
            true
        }

        searchViewModel.mutableSelectedLocation.observe(viewLifecycleOwner) {
            searchLocationViewModel.mutableSelectedLocation.postValue(it)
            parentFragmentManager.popBackStack()
        }

    }
}
