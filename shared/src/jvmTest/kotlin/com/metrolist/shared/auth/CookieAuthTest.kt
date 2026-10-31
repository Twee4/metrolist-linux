package com.metrolist.shared.auth

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CookieAuthTest {
    private val origin = "https://music.youtube.com"

    @Test
    fun hashMatchesKnownValue() {
        // Expected value computed independently with Python's hashlib.sha1.
        assertEquals(
            "1700000000_597a8411c1c01f9c14cea84b6ec377b76d93be6e",
            CookieAuth.sapisidHash("abc123", 1_700_000_000, origin),
        )
    }

    @Test
    fun prefersThirdPartyCookie() {
        assertEquals("three", CookieAuth.sapisid("SAPISID=one; __Secure-3PAPISID=three; x=y"))
        assertEquals("one", CookieAuth.sapisid("a=b; SAPISID=one"))
        assertNull(CookieAuth.sapisid("a=b; c=d"))
        assertNull(CookieAuth.sapisid("SAPISID="))
    }

    @Test
    fun cleansPastedHeader() {
        assertEquals("a=b; SAPISID=x", CookieAuth.clean("Cookie: a=b; SAPISID=x\n"))
        assertEquals("a=b", CookieAuth.clean("\"a=b\""))
    }

    @Test
    fun signInSavesPrivateFileAndSignsRequests() {
        val dir = Files.createTempDirectory("cookie-test")
        val file = dir.resolve("sub/cookie")
        val auth = CookieAuth(file, clock = { 1_700_000_000 })

        assertFalse(auth.signedIn.value)
        assertTrue(auth.headers(origin).isEmpty())
        assertFalse(auth.signIn("nothing=useful"))

        assertTrue(auth.signIn("Cookie: SAPISID=abc123; other=1"))
        assertTrue(auth.signedIn.value)
        val headers = auth.headers(origin)
        assertEquals("SAPISID=abc123; other=1", headers["Cookie"])
        assertEquals("SAPISIDHASH 1700000000_597a8411c1c01f9c14cea84b6ec377b76d93be6e", headers["Authorization"])

        assertEquals(PermissionOwnerRW, Files.getPosixFilePermissions(file))

        // A new instance picks the saved sign-in back up; sign-out removes it.
        assertTrue(CookieAuth(file).signedIn.value)
        auth.signOut()
        assertFalse(auth.signedIn.value)
        assertFalse(Files.exists(file))
    }

    private companion object {
        val PermissionOwnerRW = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
    }
}
