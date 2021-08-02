/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.main

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.drivesession.model.CandidateRoadInfo
import com.telenav.sdk.examples.R

class CandidateRoadAdapter(private val recyclerView: RecyclerView,private val onClick : (CandidateRoadInfo)->Unit) : RecyclerView.Adapter<CandidateRoadViewHolder>() {
    var dataList = ArrayList<CandidateRoadInfo>()

    private val onItemClick = View.OnClickListener {
        val index = recyclerView.getChildAdapterPosition(it)
        if (index >= 0 && index < dataList.size){
            onClick(dataList[index])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateRoadViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_candidate_road_item,parent,false)
        view.setOnClickListener(onItemClick)
        return CandidateRoadViewHolder(view)
    }

    override fun getItemCount(): Int {
       return dataList.size
    }

    override fun onBindViewHolder(holder: CandidateRoadViewHolder, position: Int) {
        holder.bind(dataList[position]);
    }

    fun setData(list: List<CandidateRoadInfo>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }
}

class CandidateRoadViewHolder(root: View) :
        RecyclerView.ViewHolder(root) {
    private val tvName: TextView = root.findViewById(R.id.tv_name)

    fun bind(road: CandidateRoadInfo) {
        if (TextUtils.isEmpty(road.roadName)){
            tvName.text = "No Name"
        }else {
            tvName.text = road.roadName
        }
    }
}