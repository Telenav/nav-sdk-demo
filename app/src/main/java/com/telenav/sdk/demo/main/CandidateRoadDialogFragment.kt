/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.main

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.fragment_candidate_road.*

class CandidateRoadDialogFragment : DialogFragment() {
    private lateinit var adapter: CandidateRoadAdapter
    private lateinit var viewModel : CandidateRoadViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_candidate_road,container,false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val lp = dialog?.window?.attributes
        lp?.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = lp
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity(),ViewModelProvider.NewInstanceFactory()).get(CandidateRoadViewModel::class.java)
        dialog?.window?.setWindowAnimations(R.style.DialogAnimation)
        dialog?.window?.attributes?.gravity = Gravity.BOTTOM

        adapter = CandidateRoadAdapter(rv){
            viewModel.selectedRoad.postValue(it)
            dismiss()
        }
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(activity,LinearLayoutManager.VERTICAL,false)
        rv.addItemDecoration(DividerItemDecoration(activity,LinearLayoutManager.VERTICAL))
        viewModel.candidateRoads.observe(this){
            adapter.setData(it)
        }
    }

}