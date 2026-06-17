package com.telenav.sdk.demo.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.databinding.WordSuggestionChipBinding

class WordSuggestionAdapter(
    private val onItemClick: (WordSuggestionItem) -> Unit
) : RecyclerView.Adapter<WordSuggestionAdapter.ViewHolder>() {

    private val items = mutableListOf<WordSuggestionItem>()

    fun submitList(newItems: List<WordSuggestionItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = WordSuggestionChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(
        private val binding: WordSuggestionChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WordSuggestionItem) {
            binding.wordSuggestionText.text = item.displayText
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}
