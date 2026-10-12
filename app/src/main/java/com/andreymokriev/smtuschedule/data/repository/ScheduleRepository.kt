// data/repository/ScheduleRepository.kt
package com.andreymokriev.smtuschedule.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.andreymokriev.smtuschedule.data.local.*
import com.andreymokriev.smtuschedule.data.model.*
import com.andreymokriev.smtuschedule.data.remote.SmtuApi
import com.andreymokriev.smtuschedule.data.remote.SmtuParser
import com.andreymokriev.smtuschedule.data.remote.TeacherRef

class ScheduleRepository(
    private val api: SmtuApi,
    private val groupDao: GroupDao,
    private val scheduleDao: ScheduleDao,
) {

    suspend fun refreshGroups(): List<GroupRef> = withContext(Dispatchers.IO) {
        val html = api.fetchGroupsHtml()
        val groups = SmtuParser.parseGroups(html)
        groupDao.clear()
        groupDao.insertAll(groups.map { GroupEntity(it.name, it.pageId) })
        groups
    }

    suspend fun loadGroups(): List<GroupRef> = withContext(Dispatchers.IO) {
        val cached = groupDao.all()
        if (cached.isNotEmpty()) cached.map { GroupRef(it.name, it.pageId) }
        else refreshGroups()
    }

    /**
     * Поиск преподавателей по фамилии через сайт.
     * Разворачивает «нет результатов» с ошибкой в пустой список.
     */
    suspend fun searchTeachers(surname: String): List<TeacherRef> = withContext(Dispatchers.IO) {
        val html = api.searchTeachersHtml(surname.trim())
        SmtuParser.parseTeacherSearch(html)
    }

    /** Расписание преподавателя; страница устроена так же, как у группы,
     *  поэтому парсинг и кэш переиспользуются. Ключ кэша — teacherId,
     *  он не пересекается с pageId групп. */
    suspend fun getTeacherSchedule(teacherId: Int, forceRefresh: Boolean = false, maxAgeMs: Long = 30 * 60_000): GroupSchedule =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                val cached = scheduleDao.get(teacherId)
                if (cached != null && System.currentTimeMillis() - cached.parsedAt < maxAgeMs) {
                    return@withContext deserialize(cached)
                }
            }
            val html = api.fetchTeacherScheduleHtml(teacherId)
            val parsed = SmtuParser.parseSchedule(teacherId, html)
            scheduleDao.put(serialize(parsed))
            parsed
        }

    suspend fun getSchedule(pageId: Int, forceRefresh: Boolean = false, maxAgeMs: Long = 30 * 60_000): GroupSchedule =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                val cached = scheduleDao.get(pageId)
                if (cached != null && System.currentTimeMillis() - cached.parsedAt < maxAgeMs) {
                    return@withContext deserialize(cached)
                }
            }
            val html = api.fetchScheduleHtml(pageId)
            val parsed = SmtuParser.parseSchedule(pageId, html)
            scheduleDao.put(serialize(parsed))
            parsed
        }

    private fun serialize(s: GroupSchedule): ScheduleEntity =
        ScheduleEntity(
            pageId = s.pageId,
            groupName = s.groupName,
            dateInfo = s.dateInfo,
            parsedAt = s.parsedAt,
            json = ScheduleJson.encode(s.days)
        )

    private fun deserialize(e: ScheduleEntity): GroupSchedule =
        GroupSchedule(
            pageId = e.pageId,
            groupName = e.groupName,
            dateInfo = e.dateInfo,
            parsedAt = e.parsedAt,
            days = ScheduleJson.decode(e.json)
        )
}