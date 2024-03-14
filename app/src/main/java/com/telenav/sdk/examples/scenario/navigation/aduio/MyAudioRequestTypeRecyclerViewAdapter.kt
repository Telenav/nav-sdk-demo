/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation.aduio

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.telenav.sdk.examples.databinding.ViewAudioRequestTypeBinding

class MyAudioRequestTypeRecyclerViewAdapter(val values: MutableList<AudioRequestTypeInfo>) : RecyclerView.Adapter<MyAudioRequestTypeRecyclerViewAdapter.ViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val viewAudioRequestTypeBinding: ViewAudioRequestTypeBinding = ViewAudioRequestTypeBinding.inflate(layoutInflater, parent, false);
        return ViewHolder(viewAudioRequestTypeBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(values[position])
        holder.binding.root.setOnClickListener {
            onItemClickListener?.onItemClick(holder.binding.root, values[position])
        }
    }

    override fun getItemCount(): Int = values.size

    @JvmName("setOnItemClickListener1")
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, audioRequestTypeInfo: AudioRequestTypeInfo)
    }


    inner class ViewHolder(val binding: ViewAudioRequestTypeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(audioRequestTypeInfo: AudioRequestTypeInfo) {
            binding.audioRequestTypeInfo = audioRequestTypeInfo
        }
    }

}