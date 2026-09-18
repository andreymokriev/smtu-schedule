// data/remote/SmtuParser.kt
package com.andreymokriev.smtuschedule.data.remote

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import com.andreymokriev.smtuschedule.data.model.*

object SmtuParser {

    private const val BASE = "https://www.smtu.ru"

    fun parseGroups(html: String): List<GroupRef> {
        val doc: Document = Jsoup.parse(html)
        return doc.select("div.gr a").mapNotNull { a ->
            val href = a.attr("href")
            val m = Regex("""/viewschedule_new/(\d+)/?""").find(href) ?: return@mapNotNull null
            val name = a.text().trim()
            if (name.isEmpty()) null
            else GroupRef(name = name, pageId = m.groupValues[1].toInt())
        }
    }

    fun parseSchedule(pageId: Int, html: String): GroupSchedule {
        val doc = Jsoup.parse(html)

        val groupName = doc.selectFirst("h1")
            ?.text()
            ?.substringAfter("группы", "")?.trim()?.ifEmpty { null }

        val dateInfo = (doc.selectFirst("h2.h5.text-muted") ?: doc.selectFirst("h4"))
            ?.text()?.trim()

        val days = mutableListOf<DaySchedule>()

        doc.select("#table-container .card").forEach { card ->
            val header = card.selectFirst(".card-header h3") ?: return@forEach
            val dayName = header.text().trim()

            val table = card.selectFirst("table")
            if (table == null) {
                days += DaySchedule(dayName, DayStatus.PENDING, emptyList())
                return@forEach
            }

            val lessons = mutableListOf<Lesson>()
            table.select("tbody tr").forEach { tr ->
                val tds = tr.select("td")
                if (tds.size < 5) return@forEach

                val time = tr.selectFirst("th")?.text()?.trim().orEmpty()
                val week = detectWeek(tr, tds[0])
                val room = tds[1].text().trim()
                val group = tds[2].text().trim()

                val subjectCell = tds[3]
                val subject = (subjectCell.selectFirst("span") ?: subjectCell)
                    .text().trim()
                val lessonType = subjectCell.selectFirst("small")?.text()?.trim()

                val teacherCell = tds[4]
                val teacher = (teacherCell.selectFirst("span") ?: teacherCell)
                    .text().replace(Regex("\\s+"), " ").trim()

                lessons += Lesson(time, week, room, group, subject, lessonType, teacher)
            }

            days += DaySchedule(
                dayName = dayName,
                status = if (lessons.isEmpty()) DayStatus.EMPTY else DayStatus.OK,
                lessons = lessons
            )
        }

        return GroupSchedule(
            pageId = pageId,
            groupName = groupName,
            dateInfo = dateInfo,
            parsedAt = System.currentTimeMillis(),
            days = days
        )
    }

    private fun detectWeek(tr: Element, weekCell: Element): WeekType {
        val cls = weekCell.classNames()
        val id = tr.id()
        return when {
            "text-success" in cls -> WeekType.UP
            "text-warning" in cls -> WeekType.DOWN
            id.contains("week-both") -> WeekType.BOTH
            else -> WeekType.UNKNOWN
        }
    }

    fun buildScheduleUrl(pageId: Int, week: String = "all", view: String = "table"): String =
        "$BASE/ru/viewschedule_new/$pageId/?week=$week&view_mode=$view"

    fun groupsUrl(): String = "$BASE/ru/listschedule/"
}