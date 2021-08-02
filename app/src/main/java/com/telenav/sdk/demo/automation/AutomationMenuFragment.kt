/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.automation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.menu_recycleview.*

/**
 * @author tang.hui
 * This fragment is a menu of automation ui test
 */
class AutomationMenuFragment : Fragment() {

    private lateinit var menuList: List<MenuData>
    private lateinit var menuAdapter: MenuAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.menu_recycleview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMenuData()
        menuAdapter = MenuAdapter(recycleView, menuList)
        recycleView.adapter = menuAdapter
        recycleView.layoutManager = LinearLayoutManager(activity)
        recycleView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    }

    private fun initMenuData() {
        menuList = listOf(
                MenuData("Pure Navigation Test") {
                    findNavController().navigate(R.id.action_automationMenuFragment_to_noMapViewFragment)
                },

                MenuData("RequestRoute") {
                    findNavController().navigate(R.id.action_menuFragment_to_requestRouteFragment)
                }
        )
    }

    private class MenuAdapter(
            val recyclerView: RecyclerView,
            val menuList: List<MenuData>
    ) : RecyclerView.Adapter<MenuViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder =
                MenuViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_scenario_menu_item,
                        parent, false)).apply {
                    this.root.setOnClickListener(onClickListener)
                }

        override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
            holder.tvTitle.text = menuList[position].text
            holder.tvIndex.text = (position + 1).toString()
        }

        override fun getItemCount(): Int = menuList.size

        val onClickListener = View.OnClickListener {
            val pos = recyclerView.getChildAdapterPosition(it)
            if (pos >= 0 && pos < menuList.size) {
                menuList[pos].onClick()
            }
        }

    }

    class MenuViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val root: View = root.findViewById(R.id.root)
        val tvTitle: TextView = root.findViewById(R.id.tv_title)
        val tvIndex: TextView = root.findViewById(R.id.tv_index)
    }

    private data class MenuData(val text: String, val onClick: () -> Unit)
}