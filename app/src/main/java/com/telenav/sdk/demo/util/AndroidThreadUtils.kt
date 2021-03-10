/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.util

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