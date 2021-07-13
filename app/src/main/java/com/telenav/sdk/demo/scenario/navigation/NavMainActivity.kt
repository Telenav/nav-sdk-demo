/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.scenario.navigation.*
import kotlinx.android.synthetic.main.activity_nav_main.*

/**
 * @author tang.hui on 2021/1/11
 */
class NavMainActivity : AppCompatActivity() {

    /**
     * add menu and click function here
     */
    private val menuList = listOf(
            MenuData("request route") {
                showFragment(RequestRouteFragment())
            },
            MenuData("auto refresh route") {
                showFragment(AutoRefreshRouteFragment())
            },
            MenuData("turn by turn list") {
                showFragment(TurnByTurnListFragment())
            },
            MenuData("whereami and location provider") {
                showFragment(WhereamiFragment())
            },
            MenuData("stop point") {
                showFragment(StopPointFragment())
            },
            MenuData("prompt voice") {
                Toast.makeText(applicationContext, "todo", Toast.LENGTH_SHORT).show()
            },
            MenuData("get alter info") {
                showFragment(AlertInfoFragment())
            },
            MenuData("adas messages") {
                showFragment(AdasMessageFragment())
            },
            MenuData("traffic bar") {
                showFragment(TrafficBarFragment())
            },
            MenuData("jump road") {
                showFragment(JumpRoadFragment())
            }
    )

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
            setReorderingAllowed(true)
            addToBackStack(null)
        }
    }

    private lateinit var menuAdapter: MenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        menuAdapter = MenuAdapter(recycleView, menuList)
        recycleView.adapter = menuAdapter
        recycleView.layoutManager = LinearLayoutManager(this)
        recycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.backStackEntryCount!=0) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }
        return super.onSupportNavigateUp()
    }

    private class MenuAdapter(
            val recyclerView: RecyclerView,
            val menuList: List<MenuData>
    ) : RecyclerView.Adapter<MenuViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder =
                MenuViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_scenario_menu_item,
                        parent, false)).apply {
                    this.layoutRoot.setOnClickListener(onClickListener)
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
        val layoutRoot: View = root.findViewById(R.id.root)
        val tvTitle: TextView = root.findViewById(R.id.tv_title)
        val tvIndex: TextView = root.findViewById(R.id.tv_index)
    }

    private data class MenuData(val text: String, val onClick: () -> Unit)
}