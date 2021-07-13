package com.telenav.sdk.demo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.databinding.SearchRowBinding

class SearchAdapter : RecyclerView.Adapter<CustomViewHolder>() {
    private val searchResultList = arrayListOf<SearchResultItemDao>()
    private lateinit var layoutListener: OnClickedLayoutListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val searchRowBinding = SearchRowBinding.inflate(layoutInflater, parent, false)
        return CustomViewHolder(searchRowBinding).listen { pos, _ ->
            layoutListener.onClickLayout(searchResultList[pos])
        }
    }

    override fun getItemCount() : Int {
        return searchResultList.size
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.bind(searchResultList[position])
    }

    fun setSearchData(searchResultItemListObj: List<SearchResultItemDao>) {
        searchResultList.clear()
        searchResultList.addAll(searchResultItemListObj.sortedWith(Comparator { o1, o2 -> if (o1.distance - o2.distance >= 0) 1 else -1}))
        notifyDataSetChanged()
    }

    fun setOnClickListener(onClickedLayoutListener: OnClickedLayoutListener) {
        layoutListener = onClickedLayoutListener
    }

}

class CustomViewHolder(private val binding: SearchRowBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(searchResultItemDao: SearchResultItemDao) {
        binding.searchItemDao = searchResultItemDao
    }

}

fun <T : RecyclerView.ViewHolder> T.listen(event: (position: Int, type: Int) -> Unit): T {
    itemView.setOnClickListener {
        event.invoke(adapterPosition, itemViewType)
    }
    return this
}

interface OnClickedLayoutListener {
    fun onClickLayout(location: SearchResultItemDao)
}
