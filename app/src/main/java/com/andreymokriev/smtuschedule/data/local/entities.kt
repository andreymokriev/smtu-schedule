// data/local/entities.kt
package com.andreymokriev.smtuschedule.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val name: String,
    val pageId: Int,
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val pageId: Int,
    val groupName: String?,
    val dateInfo: String?,
    val parsedAt: Long,
    val json: String,          // сериализованные дни — проще, чем плодить таблицы
)