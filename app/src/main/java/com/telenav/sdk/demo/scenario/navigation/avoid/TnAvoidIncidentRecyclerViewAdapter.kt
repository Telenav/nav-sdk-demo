/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation.avoid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.databinding.ViewAvoidIncidentBinding
import com.telenav.sdk.map.model.AlongRouteTrafficIncidentInfo

/**
 * AvoidIncidentRecyclerViewAdapter
 * @author wuchangzhong on 2021/09/15
 */

class TnAvoidIncidentRecyclerViewAdapter : RecyclerView.Adapter<TnAvoidIncidentViewHolder>() {

    var alongRouteTrafficIncidentInfoList = ArrayList<AlongRouteTrafficIncidentInfo>()
    private var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TnAvoidIncidentViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val avoidIncidentViewBinding: ViewAvoidIncidentBinding = ViewAvoidIncidentBinding.inflate(layoutInflater, parent, false);
        return TnAvoidIncidentViewHolder(avoidIncidentViewBinding)
    }

    override fun getItemCount(): Int {
        return alongRouteTrafficIncidentInfoList.size
    }

    override fun onBindViewHolder(holderTn: TnAvoidIncidentViewHolder, position: Int) {
        holderTn.bind(alongRouteTrafficIncidentInfoList[position])
        holderTn.binding.root.setOnClickListener {
            onItemClickListener?.onItemClick(holderTn.binding.root, alongRouteTrafficIncidentInfoList[position])
        }
    }

    fun setupData(listTn: List<AlongRouteTrafficIncidentInfo>?) {
        alongRouteTrafficIncidentInfoList.clear()
        if (listTn != null) {
            alongRouteTrafficIncidentInfoList.addAll(listTn)
        }
        notifyDataSetChanged()
    }

    @JvmName("setOnItemClickListener1")
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, alongRouteTrafficIncidentInfo: AlongRouteTrafficIncidentInfo)
    }

    fun clear() {
        alongRouteTrafficIncidentInfoList.clear()
        notifyDataSetChanged()
    }
}