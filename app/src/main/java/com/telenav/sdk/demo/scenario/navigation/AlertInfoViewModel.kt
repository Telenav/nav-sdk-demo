/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.demo.R

/**
 * @author tang.hui
 */
class AlertInfoViewModel : ViewModel() {

    val showNavigationDetails = MutableLiveData(false)
    val dataList = mutableListOf<String>("highWay exits")
    val serviceAreaList = mutableListOf<String>("service areas")
    val cameraList = mutableListOf<String>("camera info")
    val adapter = AlertInfoAdapter(dataList)
    val serviceAreaAdapter = AlertInfoAdapter(serviceAreaList)
    val cameraAdapter = AlertInfoAdapter(cameraList)

    val resetLiveData = MutableLiveData(false)

    fun reset(view: View) {
        resetLiveData.postValue(true)
        dataList.clear()
        serviceAreaList.clear()
        cameraList.clear()
        adapter.notifyDataSetChanged()
        serviceAreaAdapter.notifyDataSetChanged()
        cameraAdapter.notifyDataSetChanged()
    }

}

class AlertInfoAdapter(val data: List<String>) : RecyclerView.Adapter<AlertInfoAdapter.CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            CustomViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_alert_info, parent, false))

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        if (position < data.size) {
            holder.textView.text = data[position]
        }
    }

    override fun getItemCount(): Int = data.size

    class CustomViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val textView: TextView = root.findViewById(R.id.textView)
    }
}