package com.telenav.sdk.demo.util

import com.telenav.sdk.drivesession.model.SpeedLimitType
import com.telenav.sdk.drivesession.model.adas.AdasMessage
import kotlin.collections.ArrayList

/**
 * @author zhai.xiang on 2021/2/3
 */
class AdasMessageDecoder {

    companion object {
        /**
         * the max size of adas message for each type
         */
        private const val MAX_SIZE_OF_EACH_MESSAGE_TYPE = 4

        /**
         * maximum detection distance
         */
        const val MAX_DETECT_DISTANCE = 4096

        /**
         * default speed unit is kilo meter per hour
         */
        const val DEFAULT_SPEED_UNIT = 0

        /**
         * index can be 0, 1, 2
         */
        private const val MAX_CYCLIC_INDEX = 3

    }

    private var lastPathIndex = -1
    private var positionMessage: PositionMessage? = null
    private var metaDataMessage: MetaDataMessage? = null
    private var segmentMessages: MutableList<SegmentMessage> = ArrayList()
    private var profileMessages: MutableList<ProfileMessage> = ArrayList()
    private var index: Int = -1

    /**
     * put new adas message list to decoder
     */
    fun addMessageList(messageList: List<AdasMessage>) {
        PositionMessage.getPositionMessage(messageList)?.let {
            if (it.isPathAvailable()) {
                this.positionMessage = it
                if (lastPathIndex != it.pathIndex) {
                    index = (1 + index) % MAX_CYCLIC_INDEX
                    removeMessageByPath(lastPathIndex)
                    segmentMessages.clear()
                    profileMessages.clear()
                    lastPathIndex = it.pathIndex
                }
            }
        }

        MetaDataMessage.getMetaDataMessage(messageList)?.let {
            this.metaDataMessage = it
        }

        if (positionMessage == null) {
            return
        }

        ProfileMessage.getMessages(messageList, positionMessage!!.offset).let {
            this.profileMessages.addAll(it)
        }

        SegmentMessage.getMessages(messageList, positionMessage!!.offset).let {
            this.segmentMessages.addAll(it)
        }

        removeAllBehindMessage(positionMessage!!)

    }

    /**
     * get the decoded speed limit by type
     */
    fun decodeByType(@SpeedLimitType type: Int): List<SpeedLimitPoint> {
        val list = ArrayList<SpeedPosition>()
        list.addAll(getSpeedLimitFormProfileByType(profileMessages, type))
        list.addAll(getSpeedLimitFormSegmentByType(segmentMessages, type))
        val orderedList = list.sortedWith { o1, o2 ->
            sortCompareByDistance(o1, o2)
        }.distinctBy {
            it.distance
        }

        val result = ArrayList(mergeSpeedLimitPoint(orderedList).map {
            it.convertToSpeedLimitPosition(metaDataMessage!!, index)
        }.filter {
            it.distance >= 0
        }.distinctBy {
            it.distance
        }.take(MAX_SIZE_OF_EACH_MESSAGE_TYPE))

        while (result.size < MAX_SIZE_OF_EACH_MESSAGE_TYPE) {
            result.add(SpeedLimitPoint(SpeedLimitPoint.NO_VALUE, MAX_DETECT_DISTANCE, DEFAULT_SPEED_UNIT, type, index))
        }

        return result
    }

    /**
     * get country code.
     */
    fun getCountryCode(): Int {
        return metaDataMessage?.countryCode ?: -1
    }

    /**
     * Each type can has only one message behind the vehicle
     */
    private fun removeAllBehindMessage(positionMessage: PositionMessage) {
        removeBehindMessages(positionMessage.pathIndex, positionMessage.offset, SpeedLimitType.TIME)
        removeBehindMessages(positionMessage.pathIndex, positionMessage.offset, SpeedLimitType.RAINY)
        removeBehindMessages(positionMessage.pathIndex, positionMessage.offset, SpeedLimitType.FOGGY)
        removeBehindMessages(positionMessage.pathIndex, positionMessage.offset, SpeedLimitType.FOGGY)
    }

    private fun removeBehindMessages(currentIndex: Int, currentOffset: Int, type: Int) {
        val segmentRemoveList = segmentMessages.filter {
            currentIndex == it.pathIndex && currentOffset > it.offset && type == it.type
        }

        if (segmentRemoveList.size > 1) {
            segmentMessages.removeAll(segmentRemoveList.subList(0, segmentRemoveList.size - 1))
        }

        val profileRemoveList = profileMessages.filter {
            currentIndex == it.pathIndex && currentOffset > it.offset && type == it.type
        }

        if (profileRemoveList.size > 1) {
            profileMessages.removeAll(profileRemoveList.subList(0, profileRemoveList.size - 1))
        }
    }


    /**
     * merge the connected, same typed speed positions to one
     */
    private fun mergeSpeedLimitPoint(orderedList: List<SpeedPosition>): List<SpeedPosition> {
        val list = ArrayList<SpeedPosition>()
        // if current position is not in a speed limit region, return empty list
        if (orderedList.isEmpty() || orderedList[0].distance > 0) {
            return list
        }

        orderedList.forEachIndexed { index, point ->
            if (list.isEmpty()) {
                list.add(point)
            } else {
                val last = list[list.size - 1]
                if (last.canMerge(point) && index != orderedList.size - 1) {
                    last.mergeWith(point)
                } else {
                    last.mergeWith(point)
                    list.add(point)
                }
            }
        }

        return list
    }

    private fun getSpeedLimitFormSegmentByType(segmentMessages: List<SegmentMessage>, type: Int): List<SpeedPosition> {
        return segmentMessages.mapNotNull {
            it.getSpeedLimit(positionMessage!!)
        }.filter {
            it.type == type
        }
    }

    private fun getSpeedLimitFormProfileByType(profileMessages: List<ProfileMessage>, type: Int): List<SpeedPosition> {
        return profileMessages.mapNotNull {
            it.getSpeedLimit(positionMessage!!)
        }.filter {
            it.type == type
        }
    }

    /**
     * remove messages by path
     */
    private fun removeMessageByPath(pathIndex: Int) {
        val segmentRemoveList = segmentMessages.filter { pathIndex == it.pathIndex }
        segmentMessages.removeAll(segmentRemoveList)
        val profileRemoveList = profileMessages.filter { pathIndex == it.pathIndex }
        profileMessages.removeAll(profileRemoveList)
    }

    /**
     * sorted by distance
     */
    private fun sortCompareByDistance(o1: SpeedPosition, o2: SpeedPosition): Int = o1.distance - o2.distance

}

class PositionMessage(content: Long) : BaseMessage(content) {
    /**
     * Index of position. 0 means main path, other number means the candidate path.
     */
    private val positionIndex = content.shr(38).and(0x3).toInt()

    /**
     * Path index must at least 8
     */
    fun isPathAvailable(): Boolean = pathIndex > 7

    companion object {
        private const val MESSAGE_TYPE_POSITION = 1

        fun getPositionMessage(messageList: List<AdasMessage>): PositionMessage? {
            return messageList.filter {
                it.messageType == MESSAGE_TYPE_POSITION
            }.map {
                PositionMessage(it.content)
            }.findLast {
                // main road
                it.positionIndex == 0
            }
        }
    }

}

class MetaDataMessage(content: Long) {
    /**
     * The country code number.
     */
    val countryCode = content.shr(48).and(0x3ff).toInt()

    /**
     * Speed limit unit. 0 means km/h. 1 means mph.
     */
    val speedUnit = content.shr(8).and(0x1).toInt()

    companion object {
        private const val MESSAGE_TYPE_PROFILE_META_DATA = 6

        fun getMetaDataMessage(messageList: List<AdasMessage>): MetaDataMessage? {
            val positionMessage = messageList.findLast {
                it.messageType == MESSAGE_TYPE_PROFILE_META_DATA
            }
            if (positionMessage != null) {
                return MetaDataMessage(positionMessage.content)
            }
            return null
        }
    }

}

class ProfileMessage(content: Long, vehicleOffset: Int) : BaseMessage(content) {
    /**
     * The profile type. If equals to 16, the profile contains speed limit information.
     */
    private val profileType = content.shr(35).and(0x1f).toInt()

    /**
     * The speed limit value. The unit is in meta-data.
     */
    private val speedLimitValue = getSpeedLimitValueByIndex(content.shr(13).and(0x1f).toInt())

    /**
     * The speed limit type. See [SpeedLimitType]
     */
    private val speedLimitType = wrapperSpeedLimitType(content.shr(10).and(0x7).toInt())

    /**
     * The actual offset of this message. can be larger than 8192.
     */
    override val offset: Int = getActualOffset(super.offset, vehicleOffset)

    companion object {
        private const val MESSAGE_TYPE_PROFILE_SHORT = 4

        fun getMessages(messageList: List<AdasMessage>, currentVehicleOffset: Int): List<ProfileMessage> {
            return messageList.filter {
                it.messageType == MESSAGE_TYPE_PROFILE_SHORT
            }.map {
                ProfileMessage(it.content, currentVehicleOffset)
            }.filter {
                it.profileType == 16
            }
        }
    }

    fun getSpeedLimit(positionMessage: PositionMessage): SpeedPosition? {
        if (pathIndex != positionMessage.pathIndex) {
            return null
        }
        return SpeedPosition(
                offset - positionMessage.offset,
                speedLimitValue,
                speedLimitType)
    }

    private fun wrapperSpeedLimitType(type: Int): Int {
        return when (type) {
            2 -> SpeedLimitType.RAINY
            3 -> SpeedLimitType.SNOWY
            7 -> SpeedLimitType.FOGGY
            else -> SpeedLimitType.TIME
        }
    }

}

class SegmentMessage(content: Long, vehicleOffset: Int) : BaseMessage(content) {
    /**
     * The speed limit value. The unit is in meta-data.
     */
    private val speedLimitValue = getSpeedLimitValueByIndex(content.shr(11).and(0x1f).toInt())

    /**
     * The speed limit type. See [SpeedLimitType]
     */
    private val speedLimitType = wrapperSpeedLimitType(content.shr(8).and(0x7).toInt())

    /**
     * If true, this message was sent already at least once before
     */
    private val retransmission = content.shr(33).and(0x1).toInt() == 1

    /**
     * The actual offset of this message. can be larger than 8192.
     */
    override val offset: Int = getActualOffset(super.offset, vehicleOffset)

    companion object {
        private const val MESSAGE_TYPE_SEGMENT = 2

        fun getMessages(messageList: List<AdasMessage>, currentVehicleOffset: Int): List<SegmentMessage> {
            return messageList.filter {
                it.messageType == MESSAGE_TYPE_SEGMENT
            }.map {
                SegmentMessage(it.content, currentVehicleOffset)
            }.filter {
                !it.retransmission
            }
        }

    }

    fun getSpeedLimit(positionMessage: PositionMessage): SpeedPosition? {
        if (pathIndex != positionMessage.pathIndex && retransmission) {
            return null
        }
        return SpeedPosition(
                offset - positionMessage.offset,
                speedLimitValue,
                speedLimitType)
    }

    private fun wrapperSpeedLimitType(rawType: Int): Int {
        return when (rawType) {
            5 -> SpeedLimitType.RAINY
            6 -> SpeedLimitType.SNOWY
            else -> SpeedLimitType.TIME
        }
    }
}

open class BaseMessage(val content: Long) {
    /**
     * The type of message.
     */
    open val type = content.shr(61).and(0x7).toInt()

    /**
     * The offset of this message from the start of the path.
     */
    open val offset = content.shr(48).and(0x1fff).toInt()

    /**
     * Value is index of path of which this message is part
     */
    open val pathIndex = content.shr(40).and(0x3f).toInt()

    companion object {
        /**
         * max detect distance in meters
         */
        const val MAX_DETECT_DISTANCE = 4096

        /**
         * max value of offset
         */
        const val MAX_OFFSET = 8192
    }

    /**
     * get actual speed limit value
     */
    fun getSpeedLimitValueByIndex(index: Int): Int {
        return when {
            index == 0 -> SpeedLimitPoint.UNKNOWN_SPEED
            index == 1 -> 5
            index == 2 -> 7
            index == 3 -> 10
            index <= 25 -> (index - 1) * 5
            index <= 28 -> (index - 13) * 10
            index <= 29 -> SpeedLimitPoint.UNLIMITED_SPEED
            else -> SpeedLimitPoint.UNLIMITED_SPEED
        }
    }

    /**
     * Since the maximum value of offset is 8192, if the new message's offset is smaller than
     * position's offset, it doesn't mean its behind, the actual offset is current value + 8192
     */
    fun getActualOffset(currentOffset: Int, vehicleOffset: Int): Int = if (currentOffset < vehicleOffset &&
            currentOffset + MAX_OFFSET < vehicleOffset + MAX_DETECT_DISTANCE
    ) {
        currentOffset + MAX_OFFSET
    } else {
        currentOffset
    }
}

/**
 * Because of speed limit can be decoded from Segment and Profile.
 * This class is the common data structure of speed limit.
 * @property distance distance between the end of speed limit region to vehicle.
 * @property speed speed limit value.
 * @property type speed limit type. The value is enum of [SpeedLimitType]
 */
data class SpeedPosition(var distance: Int, val speed: Int, val type: Int) {

    /**
     * Return true means they can be merged to one. False otherwise.
     * If two connected Speed position has same type and same speed, they can be merged into one
     */
    fun canMerge(other: SpeedPosition): Boolean {
        return type == other.type && speed == other.speed
    }

    /**
     * merge two SpeedPosition to one.
     */
    fun mergeWith(other: SpeedPosition) {
        distance = other.distance
    }

    /**
     * Convert to output data structure
     */
    fun convertToSpeedLimitPosition(metaDataMessage: MetaDataMessage?, index: Int): SpeedLimitPoint =
            SpeedLimitPoint(speed, distance.coerceAtMost(AdasMessageDecoder.MAX_DETECT_DISTANCE), metaDataMessage?.speedUnit
                    ?: AdasMessageDecoder.DEFAULT_SPEED_UNIT, type, index)
}