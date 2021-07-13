package com.telenav.sdk.demo.seekbar

import com.telenav.sdk.demo.seekbar.ProgressSeekBar.Companion.TextPosition

class ProgressSeekBarConfigBuilder internal constructor(private val mProgressSeekBar: ProgressSeekBar) {
    var min = 0f
    var max = 0f
    var progress = 0f

    var trackSize = 0
    var secondTrackSize = 0
    var thumbRadius = 0
    var thumbRadiusOnDragging = 0
    var trackColor = 0
    var secondTrackColor = 0
    var thumbColor = 0
    var sectionCount = 0
    var isShowSectionMark = false
    var isShowSectionText = false
    var sectionTextSize = 0
    var sectionTextColor = 0

    @TextPosition
    var sectionTextPosition = 0
    var sectionTextInterval = 0
    var isShowThumbText = false
    var thumbTextSize = 0
    var thumbTextColor = 0
    var animDuration: Long = 0
    var isTouchToSeek = false
    var isSeekBySection = false
    var bubbleColor = 0
    var bubbleTextSize = 0
    var bubbleTextColor = 0
    var isAlwaysShowBubble = false
    var alwaysShowBubbleDelay: Long = 0
    var isHideBubble = false

    fun build() {
        mProgressSeekBar.config(this)
    }

    fun min(min: Float): ProgressSeekBarConfigBuilder {
        this.min = min
        progress = min
        return this
    }

    fun max(max: Float): ProgressSeekBarConfigBuilder {
        this.max = max
        return this
    }

}
