/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.main

import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.demo.databinding.ViewTurnListItemBinding

class TurnListViewHolder(val binding: ViewTurnListItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(turnListItem: TurnListItem) {
        binding.turnListItem = turnListItem
    }
}
