/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.demo.R
import com.telenav.sdk.demo.scenario.mapview.MapViewActivity
import com.telenav.sdk.demo.scenario.navigation.NavMainActivity
import com.telenav.sdk.demo.scenario.ota.OtaActivity
import kotlinx.android.synthetic.main.activity_scenario_menu.*
import kotlinx.android.synthetic.main.layout_action_bar.*

class ScenarioMenuActivity : AppCompatActivity() {

    /**
     * add menu and click function here
     */
    private val menuList = listOf(
        MenuData("Navigation") {
            startActivity(Intent(this, NavMainActivity::class.java))
        },
        MenuData("Map view") {
            startActivity(MapViewActivity.getCallingIntent(this))
        },
        MenuData("Ota") {
            startActivity(OtaActivity.getCallingIntent(this))
        },
    )

    private lateinit var menuAdapter: MenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scenario_menu)
        menuAdapter = MenuAdapter(recycleView, menuList)
        recycleView.adapter = menuAdapter
        recycleView.layoutManager = LinearLayoutManager(this)
        recycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        tv_title.text = getString(R.string.title_activity_scenario_menu)
        iv_back.setOnClickListener {
            finish()
        }
    }


    private class MenuAdapter(
        val recyclerView: RecyclerView,
        val menuList: List<MenuData>
    ) : RecyclerView.Adapter<MenuViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder =
            MenuViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.layout_scenario_menu_item,
                    parent, false
                )
            ).apply {
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

    private class MenuViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val root: View = root.findViewById(R.id.root)
        val tvTitle: TextView = root.findViewById(R.id.tv_title)
        val tvIndex: TextView = root.findViewById(R.id.tv_index)
    }

    private data class MenuData(val text: String, val onClick: () -> Unit)
}