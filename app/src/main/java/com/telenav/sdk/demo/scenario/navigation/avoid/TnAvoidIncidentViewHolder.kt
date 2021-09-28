/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation.avoid

import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.drivesession.model.AlongRouteTrafficIncidentInfo
import com.telenav.sdk.examples.databinding.ViewAvoidIncidentBinding

/**
 * TnAvoidIncidentViewHolder class is used for binding  to TnAvoidIncidentRecyclerViewAdapter
 * @author wuchangzhong on 2021/09/15
 */
class TnAvoidIncidentViewHolder(val binding: ViewAvoidIncidentBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(alongRouteTrafficIncidentInfo: AlongRouteTrafficIncidentInfo) {
        binding.alongRouteTrafficIncidentInfo = alongRouteTrafficIncidentInfo
    }
}
