package com.telenav.sdk.examples.main

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.fragment_set_vehicle_heading_dialog.view.*
import kotlin.math.roundToInt

/**
 * This fragment is used to set the heading angle of vehicle.
 * @author zhai.xiang on 2021/1/21
 */
class SetVehicleHeadingDialogFragment : DialogFragment() {

    private lateinit var editText :EditText
    private lateinit var viewModel:SetVehicleHeadingViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_set_vehicle_heading_dialog,null)
        editText = view.findViewById(R.id.et)
        viewModel =
                ViewModelProvider(requireActivity(), ViewModelProvider.NewInstanceFactory()).get(
                        SetVehicleHeadingViewModel::class.java
                )
        editText.setText(viewModel.headingAngle.value?.roundToInt().toString())
        val builder = AlertDialog.Builder(activity,R.style.DialogStyle)
                .setTitle("Set vehicle heading angle")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Submit"){_,_ ->
                    val text = editText.text.toString().trim()
                    if (!TextUtils.isEmpty(text)) {
                        viewModel.headingAngle.postValue(text.toFloat())
                    }else{
                        Toast.makeText(activity, "Invalid value", Toast.LENGTH_SHORT).show()
                    }
                }.setView(view)
        return builder.create()
    }

}