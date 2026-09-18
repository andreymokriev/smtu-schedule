// data/local/LessonDao.kt
package com.andreymokriev.smtuschedule.data.local

import androidx.room.*

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE pageId = :pageId LIMIT 1")
    suspend fun get(pageId: Int): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: ScheduleEntity)
}