/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.sdk.examples.R
import com.telenav.sdk.demo.automation.AutomationMainActivity
import com.telenav.sdk.demo.scenario.ScenarioMenuActivity
import kotlinx.android.synthetic.main.fragment_first.*
import java.io.File

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private var mainActivity: MainActivity? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btn_scenario).setOnClickListener {
//            startActivity(Intent(requireActivity(), ScenarioMenuActivity::class.java))
            mainActivity?.initNavSDKAsync {
                startActivity(Intent(requireActivity(), ScenarioMenuActivity::class.java))
            }
        }

        initText()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            mainActivity = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        mainActivity = null
    }

    private fun showSetRegionDialog() {
        if (mainActivity == null) {
            return
        }
        val modelList = mainActivity!!.getRegionInitList()
        val texts = modelList.map {
            it.getDisplayText()
        }.toTypedArray()
        AlertDialog.Builder(requireActivity())
                .setTitle("Switch region. This will delete cached data!")
                .setItems(texts) { _, index ->
                    switchRegionAndFinish(modelList[index])
                }
                .create().show()
    }

    private fun switchRegionAndFinish(model: InitSDKDataModel) {
        if (!TextUtils.isEmpty(model.mapDataPath) && !File(model.mapDataPath).exists()){
            Toast.makeText(requireContext(), "Dir doesn't exist", Toast.LENGTH_SHORT).show()
            return
        }
        RegionCachedHelper.saveSDKDataModel(requireContext(), model)
        mainActivity?.disposeSDK()
    }

    private fun initText() {
        val model = RegionCachedHelper.getSDKDataModel(requireContext())
    }
}
