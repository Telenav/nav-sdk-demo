/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.demo.databinding.ViewTurnListItemBinding

class TurnListRecyclerViewAdapter : RecyclerView.Adapter<TurnListViewHolder>() {

    var turnList = ArrayList<TurnListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TurnListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewTurnListItemBinding: ViewTurnListItemBinding = ViewTurnListItemBinding.inflate(layoutInflater, parent, false);
        return TurnListViewHolder(viewTurnListItemBinding)
    }

    override fun getItemCount(): Int {
       return turnList.size
    }

    override fun onBindViewHolder(holder: TurnListViewHolder, position: Int) {
        val turnListItem = turnList[position]
        holder.bind(turnList[position]);
    }

    fun setupData(list: List<TurnListItem>) {
        turnList.clear()
        turnList.addAll(list)
        notifyDataSetChanged()
    }
}