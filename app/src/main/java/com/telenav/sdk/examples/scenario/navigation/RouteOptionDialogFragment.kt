/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.R

/**
 * show route option dialog
 * @author tang.hui
 */
class RouteOptionDialogFragment : DialogFragment() {
    private var list: List<String> = mutableListOf()
    private lateinit var onClick: (Int) -> Unit

    companion object {
        fun newInstance(data: List<String>, onClick: (Int) -> Unit): RouteOptionDialogFragment {
            val fragment = RouteOptionDialogFragment()
            fragment.onClick = onClick
            fragment.list = data
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_route_request_option, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val lp = dialog?.window?.attributes
        lp?.width = ViewGroup.LayoutParams.MATCH_PARENT
        lp?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        lp?.gravity = Gravity.BOTTOM
        lp?.windowAnimations = R.style.DialogAnimation
        lp?.dimAmount = 0.0F
        dialog?.window?.attributes = lp
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recycleView = view.findViewById<RecyclerView>(R.id.recycleView)
        recycleView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recycleView.adapter = OptionAdapter(list, onClick)
    }

    private class OptionAdapter(val data: List<String>, val onClick: (Int) -> Unit) : RecyclerView.Adapter<OptionViewHolder>() {
        var pickPosition = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder =
                OptionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_route_response_option,
                        parent, false))

        override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
            holder.tv_route_no.text = "Route " + (position + 1)
            holder.tv_eta.text = data[position]
            holder.tv_route_no.isSelected = pickPosition == position
            holder.tv_eta.isSelected = pickPosition == position
            holder.itemView.setOnClickListener {
                pickPosition = position
                onClick(position)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int = data.size

    }

    private class OptionViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val tv_route_no: TextView = root.findViewById(R.id.tv_route_no)
        val tv_eta: TextView = root.findViewById(R.id.tv_eta)
    }

}