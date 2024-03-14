/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.telenav.sdk.drivesession.model.JunctionViewInfo
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentNavTurnbyturnListBinding
import com.telenav.sdk.uikit.turn.TnTurnListRecyclerViewAdapter

/**
 * A simple [Fragment] for maneuver info of turn-by-turn list
 * @author tang.hui on 2021/1/19
 */
class TurnByTurnListFragment : BaseNavFragment<FragmentNavTurnbyturnListBinding>() {

    private lateinit var viewModel: TurnbyturnViewModel

    private val tnTurnListAdapter: TnTurnListRecyclerViewAdapter = TnTurnListRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = TurnbyturnViewModel(tnTurnListAdapter)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNavTurnbyturnListBinding {
        val binding: FragmentNavTurnbyturnListBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_nav_turnbyturn_list,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        tnTurnListAdapter.setOnItemClickListener { _, tnTurnListItem ->
            onTurnListItemClicked(tnTurnListItem)
        }
        return binding
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.contentBasicNavigation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.turnDirectionRecyclerView.adapter = tnTurnListAdapter

        navigationOn.observe(owner = viewLifecycleOwner) {
            viewModel.showNavigationDetails.postValue(it)
            viewModel.handleNavTurnList(navigationSession)
        }
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        viewModel.onNavigationEventUpdated(navEvent)
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
        Log.d(TAG, "onJunctionViewUpdated")
        if (junctionViewInfo.isAvailable) {
            val junctionImageData = junctionViewInfo.image
            if (junctionImageData != null) {
                activity?.runOnUiThread {
                    binding.junctionView.showImage(junctionImageData)
                }
            }
        } else {
            activity?.runOnUiThread {
                binding.junctionView.hideImage()
            }
        }
    }
}