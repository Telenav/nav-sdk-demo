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
        const val MAX_DETECT_DISTANCE = 2000

        /**
         * The max value of the message fall behind the cvp, or it will be removed.
         */
        private const val DELETE_SAFE_DISTANCE = 300

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
    private var distance: Long = 0L
    private var lastSpeed: Int = 0
    private var lastWayOfType = MeaningfulWayType.NONE

    /**
     * put new adas message list to decoder
     */
    fun addMessageList(messageList: List<AdasMessage>) {
        messageList.mapNotNull {
            decodeSingleMessage(it.content)
        }.forEach {
            when (it) {
                is PositionMessage -> {
                    if (it.isCurrentRoad()) {
                        if (it.isPathAvailable()) {
                            val newDistance = calculateCVPDistance(
                                distance,
                                positionMessage?.offset ?: 0,
                                it.offset
                            )
                            it.distance = newDistance
                            if (lastPathIndex != it.pathIndex) {
                                index = (1 + index) % MAX_CYCLIC_INDEX
                                removeMessageByPath(lastPathIndex)
                                segmentMessages.clear()
                                profileMessages.clear()
                            }

                            positionMessage = it
                            lastPathIndex = it.pathIndex
                            distance = newDistance
                        } else {
                            positionMessage = null
                            metaDataMessage = null
                            segmentMessages.clear()
                            profileMessages.clear()
                            lastPathIndex = -1
                            distance = 0
                        }
                    }
                }

                is MetaDataMessage -> {
                    this.metaDataMessage = it
                }

                is ProfileMessage -> {
                    if (it.pathIndex == lastPathIndex && it.active && it.profileType == 16 ) {
                        if (it.offset == 0) {
                            profileMessages.clear()
                        }

                        it.distance = calculateSpeedDistance(distance, positionMessage?.offset?:0, it.offset)
                        profileMessages.add(it)
                    }
                }

                is SegmentMessage -> {
                    if (it.pathIndex == lastPathIndex) {
                        if (it.offset == 0) {
                            segmentMessages.clear()
                        }

                        it.distance = calculateSpeedDistance(distance, positionMessage?.offset?:0, it.offset)
                        segmentMessages.add(it)
                    }
                }
            }

        }

        removeAllBehindMessage()
    }

    private fun calculateCVPDistance(currentDistance: Long, lastOffset : Int, currentOffset: Int) : Long {
        val totalOffset = BaseMessage.MAX_OFFSET - BaseMessage.TRAILING_LENGTH
        return when {
            currentOffset - lastOffset > BaseMessage.MAX_DETECT_DISTANCE -> currentDistance + (currentOffset - lastOffset - totalOffset)
            lastOffset - currentOffset > BaseMessage.MAX_DETECT_DISTANCE -> currentDistance + (currentOffset - lastOffset + totalOffset)
            else -> currentDistance + (currentOffset - lastOffset)
        }
    }

    private fun calculateSpeedDistance(currentDistance: Long, cvpOffset : Int, speedOffset: Int) : Long {
        val totalOffset = BaseMessage.MAX_OFFSET - BaseMessage.TRAILING_LENGTH
        return when {
            speedOffset - cvpOffset > totalOffset - BaseMessage.TRAILING_LENGTH -> currentDistance + (speedOffset - cvpOffset - totalOffset)
            cvpOffset - speedOffset > BaseMessage.TRAILING_LENGTH -> currentDistance + (speedOffset - cvpOffset + totalOffset)
            else -> currentDistance + (speedOffset - cvpOffset)
        }
    }

    fun decodeSingleMessage(content : Long) : BaseMessage? {
        when(BaseMessage(content).type) {
            BaseMessage.MESSAGE_TYPE_POSITION -> {
                return PositionMessage(content)
            }

            BaseMessage.MESSAGE_TYPE_SEGMENT -> {
                return SegmentMessage(content)
            }

            BaseMessage.MESSAGE_TYPE_PROFILE_SHORT -> {
                return ProfileMessage(content)
            }

            BaseMessage.MESSAGE_TYPE_PROFILE_META_DATA -> {
                return MetaDataMessage(content)
            }

            else -> return null
        }
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
            result.add(SpeedLimitPoint(SpeedLimitPoint.NO_VALUE, MAX_DETECT_DISTANCE, DEFAULT_SPEED_UNIT, type, index, 0))
        }

        val lastSpeedChange = (result[0].speed != lastSpeed)
        lastSpeed = result[0].speed

        result.forEach {
            it.speedChange = lastSpeedChange
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
    private fun removeAllBehindMessage() {
        removeBehindMessages(lastPathIndex, SpeedLimitType.TIME)
        removeBehindMessages(lastPathIndex,  SpeedLimitType.RAINY)
        removeBehindMessages(lastPathIndex,  SpeedLimitType.FOGGY)
        removeBehindMessages(lastPathIndex,  SpeedLimitType.SNOWY)
    }


    private fun removeBehindMessages(currentIndex: Int, type: Int) {
        val segmentRemoveList = segmentMessages.filter {
            currentIndex == it.pathIndex && distance - it.distance > DELETE_SAFE_DISTANCE && type == it.type
        }
        segmentMessages.removeAll(segmentRemoveList)

        val profileRemoveList = profileMessages.filter {
            currentIndex == it.pathIndex && distance - it.distance > DELETE_SAFE_DISTANCE && type == it.type
        }

        profileMessages.removeAll(profileRemoveList)
    }



    /**
     * merge the connected, same typed speed positions to one
     */
    private fun mergeSpeedLimitPoint(orderedList: List<SpeedPosition>): List<SpeedPosition> {
        val list = ArrayList<SpeedPosition>()
        // if current position is not in a speed limit region, return empty list
        if (orderedList.isEmpty()) {
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
    val positionIndex = content.shr(38).and(0x3).toInt()

    /**
     * Path index must at least 8
     */
    fun isPathAvailable(): Boolean = pathIndex > 7

    fun isCurrentRoad(): Boolean = positionIndex == 0

    override fun toString(): String {
        return "PositionMessage [path: ${pathIndex}, offset: $offset, distance: $distance]"
    }

}

class MetaDataMessage(content: Long) : BaseMessage(content) {
    /**
     * The country code number.
     */
    val countryCode = content.shr(48).and(0x3ff).toInt()

    /**
     * Speed limit unit. 0 means km/h. 1 means mph.
     */
    val speedUnit = content.shr(4).and(0x1).toInt()


    override fun toString(): String {
        return "MetaDataMessage [countryCode: $countryCode]"
    }
}

class ProfileMessage(content: Long) : BaseMessage(content) {
    /**
     * The profile type. If equals to 16, the profile contains speed limit information.
     */
    val profileType = content.shr(35).and(0x1f).toInt()

    /**
     * The speed limit value. The unit is in meta-data.
     */
    private val speedLimitValue = getSpeedLimitValueByIndex(content.shr(13).and(0x1f).toInt())

    /**
     * The speed limit type. See [SpeedLimitType]
     */
    private val speedLimitType = wrapperSpeedLimitType(content.shr(10).and(0x7).toInt())

    /**
     * If true, this message has already been there.
     */
    val update = content.shr(32).and(0x1).toInt() == 1

    /**
     * Whether the speed condition is met
     */
    val active = content.shr(18).and(0x3).toInt() != 0


    fun getSpeedLimit(positionMessage: PositionMessage): SpeedPosition? {
        if (pathIndex != positionMessage.pathIndex) {
            return null
        }
        return SpeedPosition(
            (distance - positionMessage.distance).toInt(),
            speedLimitValue,
            speedLimitType, 0)
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

class SegmentMessage(content: Long) : BaseMessage(content) {
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
     * If true, this message has already been there.
     */
    val update = content.shr(32).and(0x1).toInt() == 1

    /**
     * Form of way, 9 means entry or exit ramp of freeway
     */
    private val formOfWay = content.shr(16).and(0xf).toInt()


    fun getSpeedLimit(positionMessage: PositionMessage): SpeedPosition? {
        if (pathIndex != positionMessage.pathIndex || retransmission) {
            return null
        }
        return SpeedPosition(
            (distance - positionMessage.distance).toInt(),
            speedLimitValue,
            speedLimitType,
            formOfWay)
    }

    fun getSpeedLimitValue() = speedLimitValue

    private fun wrapperSpeedLimitType(rawType: Int): Int {
        return when (rawType) {
            2 -> SpeedLimitType.RAINY
            3 -> SpeedLimitType.SNOWY
            7 -> SpeedLimitType.FOGGY
            else -> SpeedLimitType.TIME
        }
    }

    override fun toString(): String {
        return "SegmentMessage [path: ${pathIndex}, offset: $offset, distance: $distance，speedLimit:$speedLimitValue, update:${update}, retransmission: $retransmission]"
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

    /**
     * The distance the cvp moves
     */
    open var distance : Long = 0

    companion object {
        const val MESSAGE_TYPE_POSITION = 1
        const val MESSAGE_TYPE_SEGMENT = 2
        const val MESSAGE_TYPE_PROFILE_SHORT = 4
        const val MESSAGE_TYPE_PROFILE_META_DATA = 6

        /**
         * max detect distance in meters
         */
        const val MAX_DETECT_DISTANCE = 2000

        /**
         * max value of offset
         */
        const val MAX_OFFSET = 8191

        /**
         * Trailing length
         */
        const val TRAILING_LENGTH = 100
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
}

/**
 * Because of speed limit can be decoded from Segment and Profile.
 * This class is the common data structure of speed limit.
 * @property distance distance between the end of speed limit region to vehicle.
 * @property speed speed limit value.
 * @property type speed limit type. The value is enum of [SpeedLimitType]
 * @property formOfWay
 */
data class SpeedPosition(var distance: Int, val speed: Int, val type: Int, val formOfWay : Int) {

    /**
     * Return true means they can be merged to one. False otherwise.
     * If two connected Speed position has same type, same speed and same meaningful form of way, they can be merged into one
     */
    fun canMerge(other: SpeedPosition): Boolean {
        return type == other.type && speed == other.speed && getMeaningfulFormOfWay() == other.getMeaningfulFormOfWay()
    }

    /**
     * Return the meaningful form of way.
     */
    private fun getMeaningfulFormOfWay(): MeaningfulWayType {
        return when(formOfWay){
            0 -> MeaningfulWayType.NONE
            4 -> MeaningfulWayType.ROUNDABOUT
            9 -> MeaningfulWayType.RAMP_ON_FREEWAY
            10 -> MeaningfulWayType.RAMP_NOT_ON_FREEWAY
            else -> MeaningfulWayType.NORMAL
        }
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
            ?: AdasMessageDecoder.DEFAULT_SPEED_UNIT, type, index, formOfWay)
}

enum class MeaningfulWayType {
    NONE,
    NORMAL,
    RAMP_ON_FREEWAY,
    RAMP_NOT_ON_FREEWAY,
    ROUNDABOUT,
}