// data/model/Models.kt
package com.andreymokriev.smtuschedule.data.model

enum class WeekType { UP, DOWN, BOTH, UNKNOWN }

data class GroupRef(
    val name: String,
    val pageId: Int,
)

data class Lesson(
    val time: String,
    val week: WeekType,
    val room: String,
    val group: String,
    val subject: String,
    val type: String?,
    val teacher: String,
)

data class DaySchedule(
    val dayName: String,
    val status: DayStatus,
    val lessons: List<Lesson>,
)

enum class DayStatus { OK, PENDING, EMPTY }

data class GroupSchedule(
    val pageId: Int,
    val groupName: String?,
    val dateInfo: String?,
    val parsedAt: Long,
    val days: List<DaySchedule>,
)