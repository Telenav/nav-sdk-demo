package com.telenav.sdk.examples.util

import android.os.Handler
import android.os.Looper

class AndroidThreadUtils {
    companion object {
        val mainHandler = Handler(Looper.getMainLooper())

        fun runOnUiThread(runnable: Runnable) {
            if (Thread.currentThread() == mainHandler.looper.thread) {
                runnable.run()
            } else {
                mainHandler.post(runnable)
            }
        }
    }
}