package com.telenav.sdk.demo.search

import com.telenav.sdk.entity.model.base.Entity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EntityFacetExtractor {

    fun photoUrls(entity: Entity): List<String> {
        val items = entity.facets?.photo?.photoItems.orEmpty()
        return items.mapNotNull { item ->
            item.id?.trim()?.takeIf { it.startsWith("http", ignoreCase = true) }
                ?: readString(item, "id", "url", "photoUri", "uri")
        }.distinct()
    }

    fun reviews(entity: Entity): List<SearchDetailReview> {
        val items = entity.facets?.review?.reviews.orEmpty()
        return items.mapNotNull { review ->
            val author = review.user?.nickname?.trim()
                ?: readString(review, "nickname")
                ?: readNestedString(review, "user", "nickname")
                ?: ""
            val text = review.text?.trim()
                ?: readString(review, "text").orEmpty().trim()
            if (text.isEmpty() && author.isEmpty()) return@mapNotNull null
            val rating = review.rating?.overall?.toDouble()
                ?: readDouble(review, "overall")
                ?: readNestedDouble(review, "rating", "overall")
                ?: 0.0
            val created = review.createdTime
                ?: readLong(review, "createdTime")
            SearchDetailReview(
                author = author.ifBlank { "Anonymous" },
                rating = rating,
                text = text,
                timeLabel = formatReviewTime(created)
            )
        }
    }

    fun averageRating(entity: Entity): Double? =
        entity.facets?.rating?.firstOrNull()?.averageRating

    fun ratingCount(entity: Entity): Int? =
        entity.facets?.rating?.firstOrNull()?.totalCount

    fun priceLevel(entity: Entity): String {
        val price = entity.facets?.priceInfo ?: return ""
        readString(price, "priceDescription")?.takeIf { it.isNotBlank() }?.let { return it }
        val level = readInt(price, "priceLevel") ?: return ""
        if (level <= 0) return ""
        return "$".repeat(level.coerceIn(1, 4))
    }

    private fun formatReviewTime(epochMs: Long?): String {
        if (epochMs == null || epochMs <= 0L) return ""
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMs))
    }

    private fun readString(target: Any, vararg names: String): String? {
        for (name in names) {
            val value = runCatching {
                val method = target.javaClass.methods.firstOrNull {
                    it.name.equals("get${name.replaceFirstChar { c -> c.uppercase() }}", true) ||
                        it.name == name
                }
                method?.invoke(target)?.toString()?.trim()
            }.getOrNull()
            if (!value.isNullOrEmpty()) return value
        }
        return null
    }

    private fun readNestedString(target: Any, parent: String, child: String): String? {
        val nested = runCatching {
            target.javaClass.methods.firstOrNull {
                it.name.equals("get${parent.replaceFirstChar { c -> c.uppercase() }}", true)
            }?.invoke(target)
        }.getOrNull() ?: return null
        return readString(nested, child)
    }

    private fun readNestedDouble(target: Any, parent: String, child: String): Double? {
        val nested = runCatching {
            target.javaClass.methods.firstOrNull {
                it.name.equals("get${parent.replaceFirstChar { c -> c.uppercase() }}", true)
            }?.invoke(target)
        }.getOrNull() ?: return null
        return readDouble(nested, child)
    }

    private fun readDouble(target: Any, name: String): Double? =
        runCatching {
            target.javaClass.methods.firstOrNull {
                it.name.equals("get${name.replaceFirstChar { c -> c.uppercase() }}", true) ||
                    it.name == name
            }?.invoke(target)?.let { (it as Number).toDouble() }
        }.getOrNull()

    private fun readLong(target: Any, name: String): Long? =
        runCatching {
            target.javaClass.methods.firstOrNull {
                it.name.equals("get${name.replaceFirstChar { c -> c.uppercase() }}", true) ||
                    it.name == name
            }?.invoke(target)?.let { (it as Number).toLong() }
        }.getOrNull()

    private fun readInt(target: Any, name: String): Int? =
        runCatching {
            target.javaClass.methods.firstOrNull {
                it.name.equals("get${name.replaceFirstChar { c -> c.uppercase() }}", true) ||
                    it.name == name
            }?.invoke(target)?.let { (it as Number).toInt() }
        }.getOrNull()
}
