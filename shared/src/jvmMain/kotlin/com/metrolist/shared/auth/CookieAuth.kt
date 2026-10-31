package com.metrolist.shared.auth

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Signs in with the cookie your browser already has for music.youtube.com (the same approach as
 * ytmusicapi). The cookie works like a password, so it is kept in a file readable only by you.
 */
class CookieAuth(
    private val file: Path = defaultFile(),
    private val clock: () -> Long = { System.currentTimeMillis() / 1000 },
) : AuthProvider {
    private var cookie: String? = readSaved()
    private val _signedIn = MutableStateFlow(cookie != null)
    override val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    /** Saves [raw] and returns true, or returns false if it lacks the cookie needed to sign requests. */
    fun signIn(raw: String): Boolean {
        val cleaned = clean(raw)
        if (sapisid(cleaned) == null) return false
        save(cleaned)
        cookie = cleaned
        _signedIn.value = true
        return true
    }

    override fun signOut() {
        cookie = null
        _signedIn.value = false
        runCatching { Files.deleteIfExists(file) }
    }

    override fun headers(origin: String): Map<String, String> {
        val current = cookie ?: return emptyMap()
        val id = sapisid(current) ?: return emptyMap()
        return mapOf(
            "Cookie" to current,
            "Authorization" to "SAPISIDHASH ${sapisidHash(id, clock(), origin)}",
            "X-Goog-AuthUser" to "0",
            "X-Origin" to origin,
        )
    }

    private fun readSaved(): String? = runCatching {
        if (Files.exists(file)) clean(Files.readString(file)).takeIf { sapisid(it) != null } else null
    }.getOrNull()

    private fun save(value: String) {
        Files.createDirectories(file.parent)
        // Create the file private *before* writing the secret into it.
        if (!Files.exists(file)) {
            runCatching {
                Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
            }
        }
        Files.writeString(file, value, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        runCatching { Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------")) }
    }

    companion object {
        fun defaultFile(): Path {
            val base = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
                ?: (System.getProperty("user.home") + "/.config")
            return Path.of(base, "metrolist-linux", "cookie")
        }

        /** Accepts a pasted header value, with or without a "Cookie:" prefix, quotes or line breaks. */
        fun clean(raw: String): String =
            raw.trim()
                .removePrefix("Cookie:").removePrefix("cookie:")
                .replace("\r", "").replace("\n", "")
                .trim().trim('"', '\'').trim()

        /** The value of the cookie that signs requests. The 3P variant is preferred when present. */
        fun sapisid(cookie: String): String? {
            val values = cookie.split(";").mapNotNull {
                val i = it.indexOf('=')
                if (i <= 0) null else it.substring(0, i).trim() to it.substring(i + 1).trim()
            }.toMap()
            return (values["__Secure-3PAPISID"] ?: values["SAPISID"])?.takeIf { it.isNotEmpty() }
        }

        /** `<timestamp>_<sha1 of "timestamp sapisid origin">`, the value after "SAPISIDHASH ". */
        fun sapisidHash(sapisid: String, timestampSeconds: Long, origin: String): String {
            val digest = MessageDigest.getInstance("SHA-1")
                .digest("$timestampSeconds $sapisid $origin".toByteArray())
            return "${timestampSeconds}_" + digest.joinToString("") { "%02x".format(it) }
        }
    }
}
