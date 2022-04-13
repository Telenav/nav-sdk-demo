/*
 *  Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.demo.scenario.navigation.realReach

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.telenav.sdk.examples.R

/**
 * This fragment is used to input request distance
 * @author wu.changzhong on 2022/3/23
 */
class SetDistanceDialogFragment : DialogFragment() {

    private lateinit var editText :EditText
    private lateinit var viewModel: RealReachViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_set_real_reach_disance_dialog,null)
        editText = view.findViewById(R.id.et)
        viewModel =
                ViewModelProvider(requireActivity(), ViewModelProvider.NewInstanceFactory()).get(
                    RealReachViewModel::class.java
                )
        editText.setText(viewModel.distance.value?.toString())
        val builder = AlertDialog.Builder(activity,R.style.DialogStyle)
                .setTitle("input the distance to calculate the polygon")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Submit"){_,_ ->
                    val text = editText.text.toString().trim()
                    if (!TextUtils.isEmpty(text)) {
                        viewModel.distance.value = (text.toInt())
                    }else{
                        Toast.makeText(activity, "Invalid value", Toast.LENGTH_SHORT).show()
                    }
                }.setView(view)
        return builder.create()
    }

}