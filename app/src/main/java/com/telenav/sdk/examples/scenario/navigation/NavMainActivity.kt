/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation

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
import com.telenav.sdk.examples.scenario.findchargestation.FindChargeStationFragment
import com.telenav.sdk.examples.scenario.navigation.aduio.AudioGuidanceFragment
import com.telenav.sdk.examples.scenario.navigation.avoid.AvoidIncidentFragment
import com.telenav.sdk.examples.scenario.navigation.avoid.AvoidStepFragment
import com.telenav.sdk.examples.scenario.navigation.evtripplan.EvTripPlanFragment
import com.telenav.sdk.examples.scenario.navigation.realReach.RealReachFragment
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ActivityNavMainBinding

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
        MenuData("ev trip plan") {
            showFragment(EvTripPlanFragment())
        },
        MenuData("find charge station") {
            showFragment(FindChargeStationFragment())
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
        MenuData("nav draw poi") {
            showFragment(NavDrawPoIFragment())
        },
        MenuData("avoid incident") {
            showFragment(AvoidIncidentFragment())
        },
        MenuData("avoid step") {
            showFragment(AvoidStepFragment())
        },
        MenuData("AudioGuidance") {
            showFragment(AudioGuidanceFragment())
        },
        MenuData("RealReach") {
            showFragment(RealReachFragment())
        },
    )

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
            setReorderingAllowed(true)
            addToBackStack(null)
        }
    }

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var binding: ActivityNavMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        menuAdapter = MenuAdapter(binding.recycleView, menuList)
        binding.recycleView.adapter = menuAdapter
        binding.recycleView.layoutManager = LinearLayoutManager(this)
        binding.recycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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