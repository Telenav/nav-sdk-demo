package com.telenav.sdk.demo.seekbar

import android.animation.*
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.*
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import com.telenav.sdk.examples.R
import java.math.BigDecimal
import kotlin.math.max

class ProgressSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        @IntDef(NONE, SIDES, BOTTOM_SIDES, BELOW_SECTION_MARK)
        @Retention(AnnotationRetention.SOURCE)
        annotation class TextPosition

        const val NONE = -1
        const val SIDES = 0
        const val BOTTOM_SIDES = 1
        const val BELOW_SECTION_MARK = 2
    }

    var min: Float = 0.0f
    var max: Float= 100.0f
    private var progress: Float

    private var trackSize: Int
    private var secondTrackSize: Int
    private var thumbRadius: Int
    private var thumbRadiusOnDragging: Int
    private var trackColor: Int
    private var secondTrackColor: Int
    private var thumbColor: Int
    private var sectionCount: Int
    private var isShowSectionMark: Boolean

    private var isShowSectionText: Boolean
    private var sectionTextSize: Int
    private var sectionTextColor: Int
    private var sectionTextPosition = NONE
    private var sectionTextInterval: Int
    private var isShowThumbText: Boolean
    private var thumbTextSize: Int
    private var thumbTextColor: Int

    private var isTouchToSeek: Boolean

    private var isSeekBySection: Boolean
    private var animDuration: Long
    private var isAlwaysShowBubble: Boolean
    private var alwaysShowBubbleDelay: Long
    private var isHideBubble: Boolean

    private var bubbleColor: Int
    private var bubbleTextSize: Int
    private var bubbleTextColor: Int
    private var delta: Float = 0.0f
    private var sectionValue: Float = 0.0f
    private var thumbCenterX: Float = 0.0f
    private var trackLength: Float = 0.0f
    private var sectionOffset: Float = 0.0f
    private var isThumbOnDragging: Boolean = false
    private val textSpace: Int
    private var triggerBubbleShowing = false
    private var sectionTextArray = SparseArray<String?>()
       private var triggerSeekBySection = false
    private var left: Float = 0.0f
    private var right: Float = 0.0f
    private val paint: Paint
    private val rectText: Rect
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val bubbleView: BubbleView = BubbleView(context)
    private var bubbleRadius = 0
    private var bubbleCenterRawSolidX = 0f
    private var bubbleCenterRawSolidY = 0f
    private var bubbleCenterRawX = 0f
    private lateinit var layoutParams: WindowManager.LayoutParams
    private val point = IntArray(2)
    private var isTouchToSeekAnimEnd = true
    private var preSecValue: Float = 0.0f
    private var configBuilder: ProgressSeekBarConfigBuilder? = null

    var onProgressChangedListener: OnProgressChangedListener? = null

    private fun initConfigByPriority() {
        if (min == max) {
            min = 0.0f
            max = 100.0f
        }
        if (min > max) {
            val tmp = max
            max = min
            min = tmp
        }
        if (progress < min) {
            progress = min
        }
        if (progress > max) {
            progress = max
        }
        if (secondTrackSize < trackSize) {
            secondTrackSize = trackSize + ProgressSeekBarUtils.dp2px(2)
        }
        if (thumbRadius <= secondTrackSize) {
            thumbRadius = secondTrackSize + ProgressSeekBarUtils.dp2px(2)
        }
        if (thumbRadiusOnDragging <= secondTrackSize) {
            thumbRadiusOnDragging = secondTrackSize * 2
        }
        if (sectionCount <= 0) {
            sectionCount = 10
        }
        delta = max - min
        sectionValue = delta / sectionCount

        if (sectionTextPosition != NONE) {
            isShowSectionText = true
        }
        if (isShowSectionText) {
            if (sectionTextPosition == NONE) {
                sectionTextPosition = SIDES
            }
            if (sectionTextPosition == BELOW_SECTION_MARK) {
                isShowSectionMark = true
            }
        }
        if (sectionTextInterval < 1) {
            sectionTextInterval = 1
        }
        initSectionTextArray()


        if (isSeekBySection) {
            preSecValue = min
            if (progress != min) {
                preSecValue = sectionValue
            }
            isShowSectionMark = true

        }
        if (isHideBubble) {
            isAlwaysShowBubble = false
        }
        if (isAlwaysShowBubble) {
            setProgress(progress)
        }
        thumbTextSize =
            if (isSeekBySection || isShowSectionText && sectionTextPosition ==
                BELOW_SECTION_MARK
            ) sectionTextSize else thumbTextSize
    }

    /**
     * Calculate radius of bubble according to the Min and the Max
     */
    private fun calculateRadiusOfBubble() {
        paint.textSize = bubbleTextSize.toFloat()
        var text: String =
                min.toString()

        paint.getTextBounds(text, 0, text.length, rectText)
        val w1 = rectText.width() + textSpace * 2 shr 1
        text =

            max.toString()

        paint.getTextBounds(text, 0, text.length, rectText)
        val w2 = rectText.width() + textSpace * 2 shr 1
        bubbleRadius = ProgressSeekBarUtils.dp2px(14) // default 14dp
        val max = max(bubbleRadius, max(w1, w2))
        bubbleRadius = max + textSpace
    }

    private fun initSectionTextArray() {
        val ifBelowSection =
            sectionTextPosition == BELOW_SECTION_MARK
        val ifInterval = sectionTextInterval > 1 && sectionCount % 2 == 0
        var sectionValue: Float
        for (i in 0..sectionCount) {
            sectionValue =  min + this.sectionValue * i
            if (ifBelowSection) {
                if (ifInterval) {
                    sectionValue = if (i % sectionTextInterval == 0) {
                        min + this.sectionValue * i
                    } else {
                        continue
                    }
                }
            } else {
                if (i != 0 && i != sectionCount) {
                    continue
                }
            }
            sectionTextArray.put(
                i,
               sectionValue.toString() + ""
            )
        }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var height = thumbRadiusOnDragging * 2
        if (isShowThumbText) {
            paint.textSize = thumbTextSize.toFloat()
            paint.getTextBounds("j", 0, 1, rectText)
            height += rectText.height()
        }
        if (isShowSectionText && sectionTextPosition >= BOTTOM_SIDES) {
            paint.textSize = sectionTextSize.toFloat()
            paint.getTextBounds("j", 0, 1, rectText)
            height = max(height, thumbRadiusOnDragging * 2 + rectText.height())
        }
        height += textSpace * 2
        setMeasuredDimension(
            resolveSize(
                ProgressSeekBarUtils.dp2px(180),
                widthMeasureSpec
            ), height
        )
        left = paddingLeft + thumbRadiusOnDragging.toFloat()
        right = measuredWidth - paddingRight - thumbRadiusOnDragging.toFloat()
        if (isShowSectionText) {
            paint.textSize = sectionTextSize.toFloat()
            if (sectionTextPosition == SIDES) {
                var text = sectionTextArray[0]
                paint.getTextBounds(text, 0, text!!.length, rectText)
                left = left + (rectText.width() + textSpace).toFloat()
                text = sectionTextArray[sectionCount]
                paint.getTextBounds(text, 0, text!!.length, rectText)
                right = right - (rectText.width() + textSpace).toFloat()
            } else if (sectionTextPosition >= BOTTOM_SIDES) {
                var text = sectionTextArray[0]
                paint.getTextBounds(text, 0, text!!.length, rectText)
                var max =
                    max(thumbRadiusOnDragging.toFloat(), rectText.width() / 2f)
                left = paddingLeft + max + textSpace
                text = sectionTextArray[sectionCount]
                paint.getTextBounds(text, 0, text!!.length, rectText)
                max = max(thumbRadiusOnDragging.toFloat(), rectText.width() / 2f)
                right = measuredWidth - paddingRight - max - textSpace
            }
        } else if (isShowThumbText && sectionTextPosition == NONE) {
            paint.textSize = thumbTextSize.toFloat()
            var text = sectionTextArray[0]
            paint.getTextBounds(text, 0, text!!.length, rectText)
            var max =
                max(thumbRadiusOnDragging.toFloat(), rectText.width() / 2f)
            left = paddingLeft + max + textSpace
            text = sectionTextArray[sectionCount]
            paint.getTextBounds(text, 0, text!!.length, rectText)
            max = max(thumbRadiusOnDragging.toFloat(), rectText.width() / 2f)
            right = measuredWidth - paddingRight - max - textSpace
        }
        trackLength = right - left
        sectionOffset = trackLength * 1f / sectionCount
        if (!isHideBubble) {
            bubbleView!!.measure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        if (!isHideBubble) {
            locatePositionInWindow()
        }
    }

    /**
     * In fact there two parts of the ProgressSeeBar, they are the BubbleView and the SeekBar.
     *
     *
     * The BubbleView is added to Window by the WindowManager, so the only connection between
     * BubbleView and SeekBar is their origin raw coordinates on the screen.
     *
     *
     * It's easy to compute the coordinates(mBubbleCenterRawSolidX, mBubbleCenterRawSolidY) of point
     * when the Progress equals the Min. Then compute the pixel length increment when the Progress is
     * changing, the result is mBubbleCenterRawX. At last the WindowManager calls updateViewLayout()
     * to update the LayoutParameter.x of the BubbleView.
     */
    private fun locatePositionInWindow() {
        getLocationInWindow(point)
        val parent = parent
        if (parent is View && (parent as View).measuredWidth > 0) {
            point[0] %= (parent as View).measuredWidth
        }
        bubbleCenterRawSolidX =        point[0] + left - bubbleView!!.measuredWidth / 2f
        //}
        bubbleCenterRawX = calculateCenterRawXofBubbleView()
        bubbleCenterRawSolidY = point[1] - bubbleView.measuredHeight.toFloat()
        bubbleCenterRawSolidY -= ProgressSeekBarUtils.dp2px(24).toFloat()
        if (ProgressSeekBarUtils.isMIUI()) {
            bubbleCenterRawSolidY -= ProgressSeekBarUtils.dp2px(4).toFloat()
        }
        val context = context
        if (context is Activity) {
            val window = context.window
            if (window != null) {
                val flags = window.attributes.flags
                if (flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0) {
                    val res =
                        Resources.getSystem()
                    val id = res.getIdentifier("status_bar_height", "dimen", "android")
                    bubbleCenterRawSolidY += res.getDimensionPixelSize(id).toFloat()
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var xLeft = paddingLeft.toFloat()
        var xRight = measuredWidth - paddingRight.toFloat()
        val yTop = paddingTop + thumbRadiusOnDragging.toFloat()

        // draw sectionText SIDES or BOTTOM_SIDES
        if (isShowSectionText) {
            paint.color = sectionTextColor
            paint.textSize = sectionTextSize.toFloat()
            paint.getTextBounds(
                "0123456789",
                0,
                "0123456789".length,
                rectText
            ) // compute solid height
            if (sectionTextPosition == SIDES) {
                val y_ = yTop + rectText.height() / 2f
                var text = sectionTextArray[0]
                paint.getTextBounds(text, 0, text!!.length, rectText)
                canvas.drawText(text, xLeft + rectText.width() / 2f, y_, paint)
                xLeft += rectText.width() + textSpace.toFloat()
                text = sectionTextArray[sectionCount]
                paint.getTextBounds(text, 0, text!!.length, rectText)
                canvas.drawText(text, xRight - (rectText.width() + 0.5f) / 2f, y_, paint)
                xRight -= (rectText.width() + textSpace).toFloat()
            } else if (sectionTextPosition >= BOTTOM_SIDES) {
                var y_ = yTop + thumbRadiusOnDragging + textSpace
                var text = sectionTextArray[0]
                paint.getTextBounds(text, 0, text!!.length, rectText)
                y_ += rectText.height().toFloat()
                xLeft = left
                if (sectionTextPosition == BOTTOM_SIDES) {
                    canvas.drawText(text, xLeft, y_, paint)
                }
                text = sectionTextArray[sectionCount]
                paint.getTextBounds(text, 0, text!!.length, rectText)
                xRight = right
                if (sectionTextPosition == BOTTOM_SIDES) {
                    canvas.drawText(text, xRight, y_, paint)
                }
            }
        } else if (isShowThumbText && sectionTextPosition == NONE) {
            xLeft = left
            xRight = right
        }
        if (!isShowSectionText && !isShowThumbText || sectionTextPosition == SIDES) {
            xLeft += thumbRadiusOnDragging.toFloat()
            xRight -= thumbRadiusOnDragging.toFloat()
        }
        val isShowTextBelowSectionMark = isShowSectionText && sectionTextPosition ==
                BELOW_SECTION_MARK

        // draw sectionMark & sectionText BELOW_SECTION_MARK
        if (isShowTextBelowSectionMark || isShowSectionMark) {
            paint.textSize = sectionTextSize.toFloat()
            paint.getTextBounds(
                "0123456789",
                0,
                "0123456789".length,
                rectText
            ) // compute solid height
            var x_: Float
            val y_ = yTop + rectText.height() + thumbRadiusOnDragging + textSpace
            val r = (thumbRadiusOnDragging - ProgressSeekBarUtils.dp2px(2)) / 2f
            val junction: Float // where secondTrack meets firstTrack
            junction =
                left + trackLength / delta * Math.abs(progress - min)

            for (i in 0..sectionCount) {
                x_ = xLeft + i * sectionOffset

                    paint.color = if (x_ <= junction) secondTrackColor else trackColor

                // sectionMark
                canvas.drawCircle(x_, yTop, r, paint)

                // sectionText belows section
                if (isShowTextBelowSectionMark) {
                    paint.color = sectionTextColor
                    if (sectionTextArray[i, null] != null) {
                        canvas.drawText(sectionTextArray[i]!!, x_, y_, paint)
                    }
                }
            }
        }
        if (!isThumbOnDragging || isAlwaysShowBubble) {

                thumbCenterX = xLeft + trackLength / delta * (progress - min)

        }

        // draw thumbText
        if (isShowThumbText && !isThumbOnDragging && isTouchToSeekAnimEnd) {
            paint.color = thumbTextColor
            paint.textSize = thumbTextSize.toFloat()
            paint.getTextBounds(
                "0123456789",
                0,
                "0123456789".length,
                rectText
            ) // compute solid height
            val y_ = yTop + rectText.height() + thumbRadiusOnDragging + textSpace
            if (sectionTextPosition == BOTTOM_SIDES && progress != min && progress != max
            ) {
                canvas.drawText(progressFloat.toString(), thumbCenterX, y_, paint)
            } else {
                canvas.drawText(getProgress().toString(), thumbCenterX, y_, paint)
            }
        }

        // draw track
        paint.color = secondTrackColor
        paint.strokeWidth = secondTrackSize.toFloat()

            canvas.drawLine(xLeft, yTop, thumbCenterX, yTop, paint)
        //}

        // draw second track
        paint.color = trackColor
        paint.strokeWidth = trackSize.toFloat()

            canvas.drawLine(thumbCenterX, yTop, xRight, yTop, paint)
       // }

        // draw thumb
        paint.color = thumbColor
        canvas.drawCircle(
            thumbCenterX,
            yTop,
            if (isThumbOnDragging) thumbRadiusOnDragging.toFloat() else thumbRadius.toFloat(),
            paint
        )
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        post { requestLayout() }
    }

    override fun onVisibilityChanged(
        changedView: View,
        visibility: Int
    ) {
        if (isHideBubble || !isAlwaysShowBubble) return
        if (visibility != VISIBLE) {
            hideBubble()
        } else {
            if (triggerBubbleShowing) {
                showBubble()
            }
        }
        super.onVisibilityChanged(changedView, visibility)
    }

    override fun onDetachedFromWindow() {
        hideBubble()
        super.onDetachedFromWindow()
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    var dx = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                performClick()
                parent.requestDisallowInterceptTouchEvent(true)
                isThumbOnDragging = isThumbTouched(event)
                if (isThumbOnDragging) {
                    if (isSeekBySection && !triggerSeekBySection) {
                        triggerSeekBySection = true
                    }
                    if (isAlwaysShowBubble && !triggerBubbleShowing) {
                        triggerBubbleShowing = true
                    }
                    if (!isHideBubble) {
                        showBubble()
                    }
                    invalidate()
                } else if (isTouchToSeek && isTrackTouched(event)) {
                    isThumbOnDragging = true
                    if (isSeekBySection && !triggerSeekBySection) {
                        triggerSeekBySection = true
                    }
                    if (isAlwaysShowBubble) {
                        hideBubble()
                        triggerBubbleShowing = true
                    }

                        thumbCenterX = event.x
                        if (thumbCenterX < left) {
                            thumbCenterX = left
                        }
                        if (thumbCenterX > right) {
                            thumbCenterX = right
                        }

                    progress = calculateProgress()
                    if (!isHideBubble) {
                        bubbleCenterRawX = calculateCenterRawXofBubbleView()
                        showBubble()
                    }
                    invalidate()
                }
                dx = thumbCenterX - event.x
            }
            MotionEvent.ACTION_MOVE -> if (isThumbOnDragging) {
                var flag = true

                    thumbCenterX = event.x + dx
                    if (thumbCenterX < left) {
                        thumbCenterX = left
                    }
                    if (thumbCenterX > right) {
                        thumbCenterX = right
                    }

                if (flag) {
                    progress = calculateProgress()
                    if (!isHideBubble && bubbleView!!.parent != null) {
                        bubbleCenterRawX = calculateCenterRawXofBubbleView()
                        layoutParams.x = (bubbleCenterRawX + 0.5f).toInt()
                        windowManager.updateViewLayout(bubbleView, layoutParams)
                        bubbleView.setProgressText( getProgress().toString())
                    } else {
                        processProgress()
                    }
                    invalidate()
                    if (onProgressChangedListener != null) {
                        onProgressChangedListener!!.onProgressChanged(
                            this,
                            getProgress(),
                            progressFloat,
                            true
                        )
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                 if (isThumbOnDragging || isTouchToSeek) {
                    if (isHideBubble) {
                        animate()
                            .setDuration(animDuration)
                            .setStartDelay(if (!isThumbOnDragging && isTouchToSeek) 300 else 0.toLong())
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    isThumbOnDragging = false
                                    invalidate()
                                }

                                override fun onAnimationCancel(animation: Animator) {
                                    isThumbOnDragging = false
                                    invalidate()
                                }
                            }).start()
                    } else {
                        postDelayed({
                            bubbleView!!.animate()
                                .alpha(if (isAlwaysShowBubble) 1f else 0f)
                                .setDuration(animDuration)
                                .setListener(object :
                                    AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        if (!isAlwaysShowBubble) {
                                            hideBubble()
                                        }
                                        isThumbOnDragging = false
                                        invalidate()
                                    }

                                    override fun onAnimationCancel(animation: Animator) {
                                        if (!isAlwaysShowBubble) {
                                            hideBubble()
                                        }
                                        isThumbOnDragging = false
                                        invalidate()
                                    }
                                }).start()
                        }, animDuration)
                    }
                }
                if (onProgressChangedListener != null) {
                    onProgressChangedListener!!.onProgressChanged(
                        this,
                        getProgress(),
                        progressFloat,
                        true
                    )
                    onProgressChangedListener!!.getProgressOnActionUp(
                        this,
                        getProgress(),
                        progressFloat
                    )
                }
            }
        }
        return isThumbOnDragging || isTouchToSeek || super.onTouchEvent(event)
    }

    /**
     * Detect effective touch of thumb
     */
    private fun isThumbTouched(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        val distance = trackLength / delta * (progress - min)
        val x = left + distance
        val y = measuredHeight / 2f
        return ((event.x - x) * (event.x - x) + (event.y - y) * (event.y - y)
                <= (left + ProgressSeekBarUtils.dp2px(8)) * (left + ProgressSeekBarUtils.dp2px(8)))
    }

    /**
     * Detect effective touch of track
     */
    private fun isTrackTouched(event: MotionEvent): Boolean {
        return isEnabled && event.x >= paddingLeft && event.x <= measuredWidth - paddingRight && event.y >= paddingTop && event.y <= measuredHeight - paddingBottom
    }




    /**
     * Showing the Bubble depends the way that the WindowManager adds a Toast type view to the Window.
     *
     *
     */
    private fun showBubble() {
        if (bubbleView == null || bubbleView.parent != null) {
            return
        }
        layoutParams.x = (bubbleCenterRawX + 0.5f).toInt()
        layoutParams.y = (bubbleCenterRawSolidY + 0.5f).toInt()
        bubbleView.alpha = 0f
        bubbleView.visibility = VISIBLE
        bubbleView.animate().alpha(1f).setDuration(if (isTouchToSeek) 0 else animDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    windowManager.addView(bubbleView, layoutParams)
                }
            }).start()
        bubbleView.setProgressText(getProgress().toString())
    }

    /**
     * The WindowManager removes the BubbleView from the Window.
     */
    private fun hideBubble() {
        if (bubbleView == null) return
        bubbleView.visibility = GONE
        if (bubbleView.parent != null) {
            windowManager.removeViewImmediate(bubbleView)
        }
    }


    private fun formatFloat(value: Float): Float {
        val bigDecimal = BigDecimal.valueOf(value.toDouble())
        return bigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP).toFloat()
    }

    private fun calculateCenterRawXofBubbleView(): Float {
         return bubbleCenterRawSolidX + trackLength * (progress - min) / delta
    }

    private fun calculateProgress(): Float {
      return (thumbCenterX - left) * delta / trackLength + min
    }
    /////// Api begins /////////////////////////////////////////////////////////////////////////////
    /**
     * When ProgressSeekBar's parent view is scrollable, must listener to it's scrolling and call this
     * method to correct the offsets.
     */


    fun setProgress(progress: Float) {
        this.progress = progress
        if (onProgressChangedListener != null) {
            onProgressChangedListener!!.onProgressChanged(this, getProgress(), progressFloat, false)
            onProgressChangedListener!!.getProgressOnFinally(
                this,
                getProgress(),
                progressFloat,
                false
            )
        }
        if (!isHideBubble) {
            bubbleCenterRawX = calculateCenterRawXofBubbleView()
        }
        if (isAlwaysShowBubble) {
            hideBubble()
            postDelayed({
                showBubble()
                triggerBubbleShowing = true
            }, alwaysShowBubbleDelay)
        }
        if (isSeekBySection) {
            triggerSeekBySection = false
        }
        postInvalidate()
    }

    fun getProgress(): Int {
        return Math.round(processProgress())
    }

    val progressFloat: Float
        get() = formatFloat(processProgress())

    private fun processProgress(): Float {
        val progress = progress
        if (isSeekBySection && triggerSeekBySection) {
            val half = sectionValue / 2
            if (isTouchToSeek) {
                if (progress == min || progress == max) {
                    return progress
                }
                var secValue: Float
                for (i in 0..sectionCount) {
                    secValue = i * sectionValue
                    if (secValue < progress && secValue + sectionValue >= progress) {
                        return if (secValue + half > progress) {
                            secValue
                        } else {
                            secValue + sectionValue
                        }
                    }
                }
            }
            return if (progress >= preSecValue) { // increasing
                if (progress >= preSecValue + half) {
                    preSecValue = preSecValue + sectionValue
                    preSecValue
                } else {
                    preSecValue
                }
            } else { // reducing
                if (progress >= preSecValue - half) {
                    preSecValue
                } else {
                    preSecValue = preSecValue - sectionValue
                    preSecValue
                }
            }
        }
        return progress
    }

    fun config(builder: ProgressSeekBarConfigBuilder) {
        min = builder.min
        max = builder.max
        progress = builder.progress

        trackSize = builder.trackSize
        secondTrackSize = builder.secondTrackSize
        thumbRadius = builder.thumbRadius
        thumbRadiusOnDragging = builder.thumbRadiusOnDragging
        trackColor = builder.trackColor
        secondTrackColor = builder.secondTrackColor
        thumbColor = builder.thumbColor
        sectionCount = builder.sectionCount
        isShowSectionMark = builder.isShowSectionMark

        isShowSectionText = builder.isShowSectionText
        sectionTextSize = builder.sectionTextSize
        sectionTextColor = builder.sectionTextColor
        sectionTextPosition = builder.sectionTextPosition
        sectionTextInterval = builder.sectionTextInterval
        isShowThumbText = builder.isShowThumbText
        thumbTextSize = builder.thumbTextSize
        thumbTextColor = builder.thumbTextColor

        animDuration = builder.animDuration
        isTouchToSeek = builder.isTouchToSeek

        isSeekBySection = builder.isSeekBySection
        bubbleColor = builder.bubbleColor
        bubbleTextSize = builder.bubbleTextSize
        bubbleTextColor = builder.bubbleTextColor
        isAlwaysShowBubble = builder.isAlwaysShowBubble
        alwaysShowBubbleDelay = builder.alwaysShowBubbleDelay
        isHideBubble = builder.isHideBubble

        initConfigByPriority()
        calculateRadiusOfBubble()
        if (onProgressChangedListener != null) {
            onProgressChangedListener!!.onProgressChanged(this, getProgress(), progressFloat, false)
            onProgressChangedListener!!.getProgressOnFinally(
                this,
                getProgress(),
                progressFloat,
                false
            )
        }
        configBuilder = null
        requestLayout()
    }




    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("save_instance", super.onSaveInstanceState())
        bundle.putFloat("progress", progress)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            progress = state.getFloat("progress")
            super.onRestoreInstanceState(state.getParcelable("save_instance"))
            bubbleView?.setProgressText( getProgress().toString())
            setProgress(progress)
            return
        }
        super.onRestoreInstanceState(state)
    }

    /**
     * Listen to progress onChanged, onActionUp, onFinally
     */
    interface OnProgressChangedListener {
        fun onProgressChanged(
            progressSeekBar: ProgressSeekBar?,
            progress: Int,
            progressFloat: Float,
            fromUser: Boolean
        )

        fun getProgressOnActionUp(
            progressSeekBar: ProgressSeekBar?,
            progress: Int,
            progressFloat: Float
        )

        fun getProgressOnFinally(
            progressSeekBar: ProgressSeekBar?,
            progress: Int,
            progressFloat: Float,
            fromUser: Boolean
        )
    }

    /***********************************************************************************************
     * custom bubble view  ***********************************
     */
    private inner class BubbleView @JvmOverloads internal constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {
        private val mBubblePaint: Paint
        private val mBubblePath: Path
        private val mBubbleRectF: RectF
        private val mRect: Rect
        private var mProgressText = ""
        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int
        ) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(3 * bubbleRadius, 3 * bubbleRadius)
            mBubbleRectF[measuredWidth / 2f - bubbleRadius, 0f, measuredWidth / 2f + bubbleRadius] =
                2 * bubbleRadius.toFloat()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            mBubblePath.reset()
            val x0 = measuredWidth / 2f
            val y0 = measuredHeight - bubbleRadius / 3f
            mBubblePath.moveTo(x0, y0)
            val x1 =
                (measuredWidth / 2f - Math.sqrt(3.0) / 2f * bubbleRadius).toFloat()
            val y1 = 3 / 2f * bubbleRadius
            mBubblePath.quadTo(
                x1 - ProgressSeekBarUtils.dp2px(2), y1 - ProgressSeekBarUtils.dp2px(2),
                x1, y1
            )
            mBubblePath.arcTo(mBubbleRectF, 150f, 240f)
            val x2 =
                (measuredWidth / 2f + Math.sqrt(3.0) / 2f * bubbleRadius).toFloat()
            mBubblePath.quadTo(
                x2 + ProgressSeekBarUtils.dp2px(2), y1 - ProgressSeekBarUtils.dp2px(2),
                x0, y0
            )
            mBubblePath.close()
            mBubblePaint.color = bubbleColor
            canvas.drawPath(mBubblePath, mBubblePaint)
            mBubblePaint.textSize = bubbleTextSize.toFloat()
            mBubblePaint.color = bubbleTextColor
            mBubblePaint.getTextBounds(mProgressText, 0, mProgressText.length, mRect)
            val fm = mBubblePaint.fontMetrics
            val baseline = bubbleRadius + (fm.descent - fm.ascent) / 2f - fm.descent
            canvas.drawText(mProgressText, measuredWidth / 2f, baseline, mBubblePaint)
        }

        fun setProgressText(progressText: String?) {
            if (progressText != null && mProgressText != progressText) {
                mProgressText = progressText
                invalidate()
            }
        }

        init {
            mBubblePaint = Paint()
            mBubblePaint.isAntiAlias = true
            mBubblePaint.textAlign = Paint.Align.CENTER
            mBubblePath = Path()
            mBubbleRectF = RectF()
            mRect = Rect()
        }
    }

    init {
        kotlin.run {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ProgressSeekBar,
                defStyleAttr,
                0
            )

            progress = a.getFloat(R.styleable.ProgressSeekBar_bsb_progress, min)

            trackSize = ProgressSeekBarUtils.dp2px(2)

            secondTrackSize =  trackSize + ProgressSeekBarUtils.dp2px(2)

            thumbRadius = secondTrackSize + ProgressSeekBarUtils.dp2px(2)

            thumbRadiusOnDragging =  secondTrackSize * 2

            sectionCount = 10
            trackColor = a.getColor(
                R.styleable.ProgressSeekBar_bsb_track_color,
                ContextCompat.getColor(
                    context,
                    R.color.colorPrimary
                )
            )
            secondTrackColor = a.getColor(
                R.styleable.ProgressSeekBar_bsb_second_track_color,
                ContextCompat.getColor(
                    context,
                    R.color.colorAccent
                )
            )
            thumbColor = secondTrackColor

            isShowSectionText = false

            sectionTextSize = ProgressSeekBarUtils.sp2px(14)

            sectionTextColor =   trackColor


            isSeekBySection = false

            val pos = NONE

            sectionTextPosition = NONE

            sectionTextInterval = 1

            isShowThumbText = false

            thumbTextSize = ProgressSeekBarUtils.sp2px(14)

            thumbTextColor = secondTrackColor

            bubbleColor =  secondTrackColor

            bubbleTextSize = ProgressSeekBarUtils.sp2px(14)

            bubbleTextColor = a.getColor(
                R.styleable.ProgressSeekBar_bsb_bubble_text_color,
                Color.WHITE
            )
            isShowSectionMark = a.getBoolean(
                R.styleable.ProgressSeekBar_bsb_show_section_mark,
                false
            )


            var duration = 1
            animDuration = if (duration < 0) 200 else duration.toLong()
            isTouchToSeek =  false

            isAlwaysShowBubble =  false

            duration = 0

            alwaysShowBubbleDelay = if (duration < 0) 0 else duration.toLong()
            isHideBubble = false

            isEnabled = a.getBoolean(
                R.styleable.ProgressSeekBar_android_enabled,
                isEnabled
            )
            a.recycle()
            paint = Paint()
            paint.isAntiAlias = true
            paint.strokeCap = Paint.Cap.ROUND
            paint.textAlign = Paint.Align.CENTER
            rectText = Rect()
            textSpace = ProgressSeekBarUtils.dp2px(2)
            initConfigByPriority()
            if (isHideBubble) return@run
            bubbleView.setProgressText(getProgress().toString())
            layoutParams = WindowManager.LayoutParams()
            layoutParams.gravity = Gravity.START or Gravity.TOP
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.format = PixelFormat.TRANSLUCENT
            layoutParams.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            if (ProgressSeekBarUtils.isMIUI() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }
            calculateRadiusOfBubble()
        }
    }
}