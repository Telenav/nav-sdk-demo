package com.telenav.sdk.demo.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.databinding.AutocompleteRowBinding

class AutocompleteAdapter(
    private val onItemClick: (AutocompleteItem) -> Unit
) : RecyclerView.Adapter<AutocompleteAdapter.ViewHolder>() {

    private val items = mutableListOf<AutocompleteItem>()

    fun submitList(newItems: List<AutocompleteItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AutocompleteRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(
        private val binding: AutocompleteRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AutocompleteItem) {
            binding.autocompleteLabel.text = item.label
            SearchProviderIcon.apply(binding.autocompleteIcon, item.fromGoogle)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}
