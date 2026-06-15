package com.telenav.sdk.demo.search

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.telenav.sdk.examples.R

object RatingDisplay {

    fun formatScore(rating: Double): String = "%.1f".format(rating)

    fun buildStars(context: Context, rating: Double): SpannableString {
        val normalized = rating.coerceIn(0.0, 5.0)
        val starText = buildString {
            for (i in 1..5) {
                append(if (normalized >= i - 0.25) "★" else "☆")
            }
        }
        val span = SpannableString(starText)
        val filledColor = ContextCompat.getColor(context, R.color.search_rating_star)
        val emptyColor = ContextCompat.getColor(context, R.color.search_rating_star_empty)
        for (i in starText.indices) {
            val color = if (starText[i] == '★') filledColor else emptyColor
            span.setSpan(
                ForegroundColorSpan(color),
                i,
                i + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return span
    }

    fun applyStars(textView: TextView, rating: Double) {
        textView.text = buildStars(textView.context, rating)
    }
}
