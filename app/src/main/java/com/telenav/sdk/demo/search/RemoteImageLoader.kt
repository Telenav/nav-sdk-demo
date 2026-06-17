package com.telenav.sdk.demo.search

import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

object RemoteImageLoader {

    private val cache = LruCache<String, android.graphics.Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    )

    fun load(imageView: ImageView, url: String, scope: CoroutineScope) {
        val cached = cache.get(url)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }
        imageView.tag = url
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(url).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            } ?: return@launch
            cache.put(url, bitmap)
            if (imageView.tag == url) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }
}
