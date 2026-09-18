// ScheduleApp.kt
package com.andreymokriev.smtuschedule

import android.app.Application
import com.andreymokriev.smtuschedule.data.local.AppDatabase
import com.andreymokriev.smtuschedule.data.remote.SmtuApi
import com.andreymokriev.smtuschedule.data.repository.ScheduleRepository

class ScheduleApp : Application() {
    val repository: ScheduleRepository by lazy {
        val db = AppDatabase.get(this)
        ScheduleRepository(SmtuApi(), db.groupDao(), db.scheduleDao())
    }
}