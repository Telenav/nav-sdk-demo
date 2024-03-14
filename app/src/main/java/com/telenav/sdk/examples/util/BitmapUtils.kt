package com.telenav.sdk.examples.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat

/**
 * Tools of bitmap operations
 * @author zhai.xiang on 2021/3/30
 */
object BitmapUtils {
    /**
     * convert vector resource to bitmap
     */
    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        val drawable: Drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap: Bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Create bitmap by view
     */
    fun createBitmapFromView(view: View): Bitmap {
        val width = Resources.getSystem().displayMetrics.widthPixels
        val height = Resources.getSystem().displayMetrics.heightPixels
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        view.measure(width, height)
        view.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
}