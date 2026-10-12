// data/remote/SmtuApi.kt
package com.andreymokriev.smtuschedule.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SmtuApi {

    private val cookieJar = object : CookieJar {
        private val store = ConcurrentHashMap<String, List<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store[url.host] = cookies
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            store[url.host].orEmpty()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
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

    suspend fun fetchTeacherScheduleHtml(teacherId: Int, week: String = "all"): String =
        fetch(SmtuParser.buildTeacherScheduleUrl(teacherId, week))

    /**
     * Поиск преподавателя по фамилии.
     *
     * Сайт работает так: GET /ru/searchschedule/ отдаёт страницу с одноразовым
     * скрытым полем search_key (PHP uniqid) и cookie PHPSESSID; POST формы с
     * этим же key и cookies возвращает HTML со списком результатов
     * (<ul class="schedule-search-results">...). Cookie-хранилище OkHttpClient
     * прозрачно держит сессию между GET и POST.
     */
    suspend fun searchTeachersHtml(surname: String): String = withContext(Dispatchers.IO) {
        val seedUrl = SmtuParser.teacherSearchUrl()
        val seedHtml = fetchInternal(seedUrl)
        val searchKey = SmtuParser.extractSearchKey(seedHtml)
            ?: error("Не удалось получить ключ поиска (search_key)")

        val form = FormBody.Builder()
            .add("search_key", searchKey)
            .add("whatsearch", surname)
            .build()
        val req = Request.Builder()
            .url(seedUrl)
            .post(form)
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} для поиска преподавателя")
            resp.body?.string() ?: error("Пустой ответ поиска преподавателя")
        }
    }

    private suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        fetchInternal(url)
    }

    private fun fetchInternal(url: String): String {
        val req = Request.Builder().url(url).build()
        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} for $url")
            resp.body?.string() ?: error("Empty body for $url")
        }
    }
}