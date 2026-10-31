package com.metrolist.shared.repository

import com.metrolist.shared.auth.AuthProvider
import com.metrolist.shared.auth.NoAuth
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

/** The saved sign-in no longer works (cookie expired or was revoked). */
class AuthExpiredException : Exception("Your YouTube Music sign-in expired. Sign in again in Settings.")

/** The two browse calls the library needs; an interface so paging can be tested without a network. */
interface BrowseApi {
    suspend fun browse(browseId: String): String
    suspend fun browseContinuation(token: String): String
}

/** Thin wrapper over YouTube Music's internal `youtubei/v1` endpoints. Returns raw JSON text. */
class YouTubeMusicApi(
    private val client: HttpClient = HttpClient(),
    private val auth: AuthProvider = NoAuth,
) : BrowseApi {
    override suspend fun browse(browseId: String): String =
        post("browse", body(browseId = browseId), continuation = null)

    override suspend fun browseContinuation(token: String): String =
        post("browse", body(browseId = null), continuation = token)

    private suspend fun post(endpoint: String, body: String, continuation: String?): String {
        val query = buildString {
            append("prettyPrint=false")
            if (continuation != null) append("&ctoken=$continuation&continuation=$continuation")
        }
        val authHeaders = auth.headers(ORIGIN)
        val response = client.post("$ORIGIN/youtubei/v1/$endpoint?$query") {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Origin, ORIGIN)
            authHeaders.forEach { (name, value) -> header(name, value) }
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val status = response.status.value
        if (authHeaders.isNotEmpty() && (status == 401 || status == 403)) throw AuthExpiredException()
        if (!response.status.isSuccess()) throw YouTubeMusicException("YouTube Music request failed (HTTP $status)")
        return response.bodyAsText()
    }

    fun close() = client.close()

    internal companion object {
        const val ORIGIN = "https://music.youtube.com"
        const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0"

        fun body(browseId: String?): String = buildJsonObject {
            if (browseId != null) put("browseId", browseId)
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
