package com.metrolist.shared.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MpvProtocolTest {
    @Test
    fun buildsCommands() {
        assertEquals("""{"command":["loadfile","https://x/y"]}""", MpvProtocol.loadFile("https://x/y"))
        assertEquals("""{"command":["set_property","pause",true]}""", MpvProtocol.setPause(true))
        assertEquals("""{"command":["set_property","volume",40]}""", MpvProtocol.setVolume(40))
        assertEquals("""{"command":["seek",12.5,"absolute"]}""", MpvProtocol.seekAbsolute(12.5))
        assertEquals("""{"command":["stop"]}""", MpvProtocol.stop())
        assertEquals("""{"command":["observe_property",1,"time-pos"]}""", MpvProtocol.observeProperty(1, "time-pos"))
    }

    @Test
    fun escapesQuotesInUrls() {
        val cmd = MpvProtocol.loadFile("""a"b""")
        assertEquals("""{"command":["loadfile","a\"b"]}""", cmd)
    }

    @Test
    fun watchUrlUsesVideoId() {
        assertEquals("https://music.youtube.com/watch?v=abc", MpvProtocol.watchUrl("abc"))
    }

    @Test
    fun parsesPositionAndDuration() {
        assertEquals(
            BackendEvent.Position(12.5),
            MpvProtocol.parseEvent("""{"event":"property-change","id":1,"name":"time-pos","data":12.5}"""),
        )
        assertEquals(
            BackendEvent.Duration(214.0),
            MpvProtocol.parseEvent("""{"event":"property-change","id":2,"name":"duration","data":214}"""),
        )
    }

    @Test
    fun ignoresPropertyChangesWithoutNumericData() {
        assertNull(MpvProtocol.parseEvent("""{"event":"property-change","id":1,"name":"time-pos"}"""))
        assertNull(MpvProtocol.parseEvent("""{"event":"property-change","id":1,"name":"time-pos","data":null}"""))
        assertNull(MpvProtocol.parseEvent("""{"event":"property-change","id":1,"name":"time-pos","data":"7"}"""))
        assertNull(MpvProtocol.parseEvent("""{"event":"property-change","id":9,"name":"volume","data":50.0}"""))
    }

    @Test
    fun endFileEof_isEnded() {
        assertEquals(BackendEvent.Ended, MpvProtocol.parseEvent("""{"event":"end-file","reason":"eof"}"""))
    }

    @Test
    fun endFileStopAndQuitAreIgnored() {
        assertNull(MpvProtocol.parseEvent("""{"event":"end-file","reason":"stop"}"""))
        assertNull(MpvProtocol.parseEvent("""{"event":"end-file","reason":"quit"}"""))
    }

    @Test
    fun endFileError_isErrorWithHint() {
        val event = MpvProtocol.parseEvent(
            """{"event":"end-file","reason":"error","file_error":"loading failed"}"""
        ) as BackendEvent.Error
        assertTrue("loading failed" in event.message)
        assertTrue("yt-dlp" in event.message)
    }

    @Test
    fun ignoresRepliesOtherEventsAndGarbage() {
        assertNull(MpvProtocol.parseEvent("""{"error":"success","request_id":0}"""))
        assertNull(MpvProtocol.parseEvent("""{"event":"playback-restart"}"""))
        assertNull(MpvProtocol.parseEvent("not json"))
        assertNull(MpvProtocol.parseEvent(""))
        assertNull(MpvProtocol.parseEvent("[1,2]"))
    }
}
