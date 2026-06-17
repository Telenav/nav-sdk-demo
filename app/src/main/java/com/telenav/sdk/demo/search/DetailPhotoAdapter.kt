package com.telenav.sdk.demo.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.telenav.sdk.examples.R
import kotlinx.coroutines.CoroutineScope

class DetailPhotoAdapter(
    private val scope: CoroutineScope
) : RecyclerView.Adapter<DetailPhotoAdapter.PhotoViewHolder>() {

    private val photoUrls = mutableListOf<String>()

    fun submitUrls(urls: List<String>) {
        photoUrls.clear()
        photoUrls.addAll(urls)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.detail_photo_item, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photoUrls[position])
    }

    override fun getItemCount(): Int = photoUrls.size

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.detailPhotoImage)

        fun bind(url: String) {
            imageView.setImageDrawable(null)
            RemoteImageLoader.load(imageView, url, scope)
        }
    }
}
