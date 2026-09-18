// data/repository/ScheduleJson.kt
package com.andreymokriev.smtuschedule.data.repository

import org.json.JSONArray
import org.json.JSONObject
import com.andreymokriev.smtuschedule.data.model.*

object ScheduleJson {

    fun encode(days: List<DaySchedule>): String {
        val arr = JSONArray()
        days.forEach { d ->
            val o = JSONObject()
            o.put("day", d.dayName)
            o.put("status", d.status.name)
            val lessons = JSONArray()
            d.lessons.forEach { l ->
                lessons.put(JSONObject().apply {
                    put("time", l.time)
                    put("week", l.week.name)
                    put("room", l.room)
                    put("group", l.group)
                    put("subject", l.subject)
                    put("type", l.type ?: JSONObject.NULL)
                    put("teacher", l.teacher)
                })
            }
            o.put("lessons", lessons)
            arr.put(o)
        }
        return arr.toString()
    }

    fun decode(json: String): List<DaySchedule> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val lessonsArr = o.getJSONArray("lessons")
            val lessons = (0 until lessonsArr.length()).map { j ->
                val lo = lessonsArr.getJSONObject(j)
                Lesson(
                    time = lo.getString("time"),
                    week = runCatching { WeekType.valueOf(lo.getString("week")) }
                        .getOrDefault(WeekType.UNKNOWN),
                    room = lo.getString("room"),
                    group = lo.getString("group"),
                    subject = lo.getString("subject"),
                    type = if (lo.isNull("type")) null else lo.getString("type"),
                    teacher = lo.getString("teacher"),
                )
            }
            DaySchedule(
                dayName = o.getString("day"),
                status = runCatching { DayStatus.valueOf(o.getString("status")) }
                    .getOrDefault(DayStatus.OK),
                lessons = lessons
            )
        }
    }
}