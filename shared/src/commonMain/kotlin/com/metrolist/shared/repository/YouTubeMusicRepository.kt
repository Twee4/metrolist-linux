package com.metrolist.shared.repository

import com.metrolist.shared.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class YouTubeMusicException(message: String) : Exception(message)

/**
 * Search backed by YouTube Music's unofficial internal API (the same endpoint music.youtube.com
 * uses). It is undocumented and can change without notice. Library and queue still come from the
 * in-memory demo data until those features are ported.
 */
class YouTubeMusicRepository(
    private val client: HttpClient = HttpClient(),
    private val fallback: MusicRepository = InMemoryMusicRepository(),
) : MusicRepository by fallback {

    override suspend fun search(query: String): List<Track> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()

        val response = client.post(SEARCH_URL) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Origin, ORIGIN)
            contentType(ContentType.Application.Json)
            setBody(searchRequestBody(normalized))
        }
        if (!response.status.isSuccess()) {
            throw YouTubeMusicException("YouTube Music search failed (HTTP ${response.status.value})")
        }
        return YouTubeMusicSearchParser.parse(response.bodyAsText())
    }

    fun close() = client.close()

    internal companion object {
        private const val ORIGIN = "https://music.youtube.com"
        private const val SEARCH_URL = "$ORIGIN/youtubei/v1/search?prettyPrint=false"
        private const val USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0"

        fun searchRequestBody(query: String): String = buildJsonObject {
            put("query", query)
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20250101.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
        }.toString()
    }
}
