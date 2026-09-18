// data/local/Prefs.kt
package com.andreymokriev.smtuschedule.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("prefs")

class Prefs(private val context: Context) {
    private val KEY_GROUP = stringPreferencesKey("group_name")
    private val KEY_PAGE_ID = intPreferencesKey("group_page_id")

    val selectedGroup: Flow<Pair<String, Int>?> = context.dataStore.data.map { p ->
        val name = p[KEY_GROUP] ?: return@map null
        val pageId = p[KEY_PAGE_ID] ?: return@map null
        name to pageId
    }

    suspend fun setGroup(name: String, pageId: Int) {
        context.dataStore.edit {
            it[KEY_GROUP] = name
            it[KEY_PAGE_ID] = pageId
        }
    }
}