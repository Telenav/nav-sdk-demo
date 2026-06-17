package com.telenav.sdk.demo.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.SearchResultItemDao
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

    override fun getItemCount(): Int {
        return searchResultList.size
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.bind(searchResultList[position])
    }

    fun setSearchData(searchResultItemListObj: List<SearchResultItemDao>) {
        searchResultList.clear()
        searchResultList.addAll(searchResultItemListObj.sortedWith(Comparator { o1, o2 -> if (o1.distance - o2.distance >= 0) 1 else -1 }))
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
        val title = searchResultItemDao.name.ifBlank { searchResultItemDao.addressLine }
        binding.txtName.text = title
        binding.txtName.visibility = if (title.isNotBlank()) View.VISIBLE else View.GONE

        val showAddress = searchResultItemDao.name.isNotBlank() &&
            searchResultItemDao.addressLine.isNotBlank()
        binding.txtAddress.text = searchResultItemDao.addressLine
        binding.txtAddress.visibility = if (showAddress) View.VISIBLE else View.GONE

        val rating = searchResultItemDao.rating
        if (rating != null && rating > 0) {
            binding.txtRatingScore.text = RatingDisplay.formatScore(rating)
            RatingDisplay.applyStars(binding.txtRatingStars, rating)
            binding.txtRatingRow.visibility = View.VISIBLE
        } else {
            binding.txtRatingRow.visibility = View.GONE
        }

        SearchResultMetaBinder.bindCategoryPriceLine(
            binding.txtCategoryLine,
            searchResultItemDao.category,
            searchResultItemDao.priceLevel
        )
        SearchResultMetaBinder.bindOpenHoursRow(
            binding.txtOpenHoursRow,
            binding.txtOpenStatus,
            binding.txtClosingTime,
            searchResultItemDao.openStatusLabel,
            searchResultItemDao.isOpenNow,
            searchResultItemDao.closingTimeLabel
        )

        EvConnectorRowBinder.bind(binding.evConnectorsContainer, searchResultItemDao.evConnectors)

        binding.txtDetail.text = searchResultItemDao.detailLine
        binding.txtDetail.visibility =
            if (searchResultItemDao.detailLine.isNotBlank()) View.VISIBLE else View.GONE

        if (searchResultItemDao.distance > 0) {
            binding.txtDistance.text = "%.2f mi".format(searchResultItemDao.distance)
            binding.txtDistance.visibility = View.VISIBLE
        } else {
            binding.txtDistance.visibility = View.GONE
        }

        binding.executePendingBindings()
        SearchProviderIcon.apply(binding.imgSearch, searchResultItemDao.fromGoogle)
    }

}

fun <T : RecyclerView.ViewHolder> T.listen(event: (position: Int, type: Int) -> Unit): T {
    itemView.setOnClickListener {
        event.invoke(adapterPosition, itemViewType)
    }
    return this
}

interface OnClickedLayoutListener {
    fun onClickLayout(itemDao: SearchResultItemDao)
}
