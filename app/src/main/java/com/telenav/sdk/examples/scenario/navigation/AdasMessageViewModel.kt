/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.R

/**
 * @author zhai.xiang
 */
class AdasMessageViewModel : ViewModel() {

    val showNavigationDetails = MutableLiveData(false)
    val alertList = mutableListOf<String>("Alert item")
    val adasList = mutableListOf<String>("adas message")
    val alertAdapter = AlertInfoAdapter(alertList)
    val adasAdapter = AdasMessageAdapter(adasList)
}

class AdasMessageAdapter(val data: List<String>) : RecyclerView.Adapter<AdasMessageAdapter.CustomViewHolder>() {

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