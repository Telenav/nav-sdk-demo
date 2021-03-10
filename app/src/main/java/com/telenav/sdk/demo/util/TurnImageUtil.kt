/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.util

import com.telenav.sdk.demo.R
import com.telenav.sdk.map.direction.model.LanePattern

class TurnImageUtil {
    companion object Factory {
        fun getTurnImageRes(guideLaneList: Int, items: List<ImageItems>): Int {

            when (guideLaneList) {
                LanePattern.CONTINUE -> return checkImagesFound(LanePattern.CONTINUE,items,R.drawable.ic_lane_continue)
                LanePattern.CONTINUE_AND_LEFT -> return checkImagesFound(LanePattern.CONTINUE_AND_LEFT,items,R.drawable.ic_lane_continue_and_left)
                LanePattern.CONTINUE_AND_RIGHT ->return checkImagesFound(LanePattern.CONTINUE_AND_RIGHT,items,R.drawable.ic_lane_continue_and_right)
                LanePattern.LEFT -> return checkImagesFound(LanePattern.LEFT,items,R.drawable.ic_lane_left)
                LanePattern.RIGHT -> return checkImagesFound(LanePattern.RIGHT,items,R.drawable.ic_lane_right)
                LanePattern.LEFT_UTURN -> return checkImagesFound(LanePattern.LEFT_UTURN,items,R.drawable.ic_lane_left_uturn)
                LanePattern.RIGHT_UTURN -> return checkImagesFound(LanePattern.RIGHT_UTURN,items,R.drawable.ic_lane_right_uturn)
                LanePattern.LEFT_AND_RIGHT -> return checkImagesFound(LanePattern.LEFT_AND_RIGHT,items,R.drawable.ic_lane_left_and_right)
                LanePattern.LEFT_AND_LEFT_UTURN -> return checkImagesFound(LanePattern.LEFT_AND_LEFT_UTURN,items,R.drawable.ic_lane_left_and_uturn)
                LanePattern.RIGHT_AND_RIGHT_UTURN -> return checkImagesFound(LanePattern.RIGHT_AND_RIGHT_UTURN,items,R.drawable.ic_lane_right_and_uturn)
                LanePattern.CONTINUE_AND_LEFT_UTURN -> return checkImagesFound(LanePattern.CONTINUE_AND_LEFT_UTURN,items,R.drawable.ic_lane_continue_and_left_uturn)
                LanePattern.CONTINUE_AND_LEFT_AND_RIGHT -> return checkImagesFound(LanePattern.CONTINUE_AND_LEFT_AND_RIGHT,items,R.drawable.ic_lane_continue_and_left_and_right)
            }
            return 0
        }

        private fun checkImagesFound(guideLaneList: Int, icLaneContinue: List<ImageItems>,
                                     defaultImageResource: Int): Int {
            for (it in icLaneContinue!!) {
                if(it.tag == guideLaneList)
                    return it.drawable
            }
            return defaultImageResource
        }
    }
}