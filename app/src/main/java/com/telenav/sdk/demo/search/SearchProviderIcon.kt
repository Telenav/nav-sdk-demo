package com.telenav.sdk.demo.search

import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.examples.R

object SearchProviderIcon {

    fun isGoogleId(id: String?): Boolean =
        id?.trim()?.startsWith("P-G") == true

    fun isGoogleEntity(entity: Entity?): Boolean {
        if (entity == null) return false
        if (isGoogleId(entity.id)) return true
        val place = entity.place ?: return false
        return isGoogleId(extractPlaceId(place))
    }

    private fun extractPlaceId(place: Any): String? =
        listOf("getId", "getPlace_id", "getPlaceId")
            .firstNotNullOfOrNull { methodName ->
                runCatching {
                    place.javaClass.getMethod(methodName).invoke(place) as? String
                }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            }

    fun apply(imageView: ImageView, fromGoogle: Boolean) {
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setImageResource(
            if (fromGoogle) R.drawable.ic_google_maps_pin else R.drawable.ic_tn_place_pin
        )
        ImageViewCompat.setImageTintList(imageView, null)
        imageView.clearColorFilter()
    }
}
