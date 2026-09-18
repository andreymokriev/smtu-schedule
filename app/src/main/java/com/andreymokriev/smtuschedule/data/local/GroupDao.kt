// data/local/GroupDao.kt
package com.andreymokriev.smtuschedule.data.local

import androidx.room.*

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name")
    suspend fun all(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE name = :name LIMIT 1")
    suspend fun find(name: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)

    @Query("DELETE FROM groups")
    suspend fun clear()
}