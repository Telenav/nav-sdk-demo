/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.telenav.sdk.demo.R
import com.telenav.sdk.demo.databinding.FragmentNavTurnbyturnListBinding
import com.telenav.sdk.demo.main.TurnListRecyclerViewAdapter
import com.telenav.sdk.drivesession.model.JunctionViewInfo
import com.telenav.sdk.drivesession.model.NavigationEvent
import kotlinx.android.synthetic.main.fragment_nav_turnbyturn_list.*

/**
 * A simple [Fragment] for maneuver info of turn-by-turn list
 * @author tang.hui on 2021/1/19
 */
class TurnbyturnListFragment : BaseNavFragment() {

    private lateinit var csHideJunctionView: ConstraintSet
    private lateinit var csShowJunctionView: ConstraintSet

    private lateinit var viewModel: TurnbyturnViewModel

    private val turnListAdapter: TurnListRecyclerViewAdapter = TurnListRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = TurnbyturnViewModel(turnListAdapter)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentNavTurnbyturnListBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_nav_turnbyturn_list, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initJunctionConstraintSet()
        navigationOn.observe(viewLifecycleOwner) {
            viewModel.showNavigationDetails.postValue(it)
            viewModel.handleNavTurnList(navigationSession)
        }
    }

    private fun initJunctionConstraintSet() {
        turn_direction_recycler_view.adapter = turnListAdapter
        csHideJunctionView = ConstraintSet()
        csShowJunctionView = ConstraintSet()
        csHideJunctionView.clone(layout_junction)
        csShowJunctionView.clone(activity, R.layout.layout_junction_show)
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        viewModel.onNavigationEventUpdated(navEvent)
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
        Log.d(TAG, "onJunctionViewUpdated")
        if (junctionViewInfo.isAvailable()) {
            val junctionImageData = junctionViewInfo.getImageData()
            if (junctionImageData != null) {
                val junctionImage = BitmapFactory.decodeByteArray(junctionImageData, 0, junctionImageData.size)
                activity?.runOnUiThread {
                    showJunctionImage(junctionImage)
                }
            }
        } else {
            activity?.runOnUiThread {
                hideJunctionImage()
            }
        }
    }

    private fun showJunctionImage(bitmap: Bitmap) {
        val param = iv_junction.layoutParams
        param.height = bitmap.height
        param.width = bitmap.width
        iv_junction.layoutParams = param
        iv_junction.setImageBitmap(bitmap)

        val transition = AutoTransition().apply {
            duration = 400
        }
        TransitionManager.beginDelayedTransition(layout_junction, transition)
        csShowJunctionView.applyTo(layout_junction)
    }

    private fun hideJunctionImage() {
        val transition = AutoTransition().apply {
            duration = 400
        }
        TransitionManager.beginDelayedTransition(layout_junction, transition)
        csHideJunctionView.applyTo(layout_junction)
    }

}