package com.telenav.sdk.demo.scenario.navigation.realReach

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup
/*
 *  Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.telenav.sdk.demo.scenario.navigation.realReach.RealReachViewModel
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.model.RequestMode

/**
 * This fragment is used to select request mode
 * @author wu.changzhong on 2022/3/23
 */
class SelectRequestModeDialogFragment : DialogFragment() ,RadioGroup.OnCheckedChangeListener{

    private val viewModel: RealReachViewModel by activityViewModels()

    var requestMode: RequestMode = RequestMode.CLOUD_ONLY
    var hybrid: RadioButton? = null
    var cloud: RadioButton? = null
    var onbard: RadioButton? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_request_mode,null)
        var rgSource = view.findViewById<RadioGroup>(R.id.rg_source)
        hybrid = view.findViewById<RadioButton>(R.id.rb_hybrid)
        cloud = view.findViewById<RadioButton>(R.id.rb_cloud)
        onbard =view.findViewById<RadioButton>(R.id.rb_onbard)

        rgSource.setOnCheckedChangeListener(this)
        val builder = AlertDialog.Builder(activity,R.style.DialogStyle)
                .setTitle("select request mode")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Submit"){_,_ ->
                    viewModel.requestMode.value = requestMode
                }.setView(view)
        setRequestMode()
        return builder.create()
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            R.id.rb_hybrid -> requestMode = RequestMode.HYBRID
            R.id.rb_cloud -> requestMode = RequestMode.CLOUD_ONLY
            R.id.rb_onbard -> requestMode = RequestMode.EMBEDDED_ONLY
        }
    }

   @JvmName("setRequestMode1")
   fun setRequestMode(){
       when (viewModel.requestMode.value?.ordinal) {
           RequestMode.HYBRID.ordinal -> {
                hybrid?.isChecked = true
                cloud?.isChecked = false
                onbard?.isChecked = false
           }
           RequestMode.CLOUD_ONLY.ordinal -> {
               hybrid?.isChecked = false
               cloud?.isChecked = true
               onbard?.isChecked = false
           }
           RequestMode.EMBEDDED_ONLY.ordinal -> {
               hybrid?.isChecked = false
               cloud?.isChecked = false
               onbard?.isChecked = true
           }
       }
   }
}