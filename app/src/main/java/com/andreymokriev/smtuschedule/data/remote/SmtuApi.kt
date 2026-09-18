// data/remote/SmtuApi.kt
package com.andreymokriev.smtuschedule.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SmtuApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Android) SmtuSchedule/1.0 (student app)"
                )
                .build()
            chain.proceed(req)
        }
        .build()

    suspend fun fetchGroupsHtml(): String = fetch(SmtuParser.groupsUrl())

    suspend fun fetchScheduleHtml(pageId: Int, week: String = "all"): String =
        fetch(SmtuParser.buildScheduleUrl(pageId, week))

    private suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} for $url")
            resp.body?.string() ?: error("Empty body for $url")
        }
    }
}