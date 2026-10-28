package com.metrolist.shared.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs

/**
 * Plays audio by driving an `mpv` process over its JSON IPC socket. mpv uses `yt-dlp` to resolve
 * YouTube streams, so both must be installed and on the PATH (YouTube changes often; keep yt-dlp
 * up to date). The process is started lazily on the first command and restarted if it dies.
 */
class MpvAudioBackend(
    private val mpvExecutable: String = "mpv",
) : AudioBackend {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<BackendEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: SharedFlow<BackendEvent> = _events.asSharedFlow()

    /** Commands are queued and sent in order by a single worker, so callers never block. */
    private val commands = Channel<String>(Channel.UNLIMITED)

    private class Connection(val process: Process, val channel: SocketChannel, val socketPath: Path) {
        fun dispose() {
            runCatching { channel.close() }
            process.destroy()
            deleteSocket(socketPath)
        }
    }

    @Volatile private var connection: Connection? = null
    @Volatile private var closed = false
    @Volatile private var lastPosition = Double.NEGATIVE_INFINITY

    init {
        // Don't leave an idle mpv behind if the app exits without calling close().
        Runtime.getRuntime().addShutdownHook(Thread { connection?.dispose() })

        scope.launch {
            for (command in commands) {
                try {
                    send(ensureConnected(), command)
                } catch (e: IOException) {
                    connection?.dispose()
                    connection = null
                    _events.tryEmit(BackendEvent.Error(e.message ?: "Could not talk to mpv"))
                }
            }
        }
    }

    override fun play(videoId: String) {
        lastPosition = Double.NEGATIVE_INFINITY
        commands.trySend(MpvProtocol.setPause(false))
        commands.trySend(MpvProtocol.loadFile(MpvProtocol.watchUrl(videoId)))
    }

    override fun setPaused(paused: Boolean) {
        commands.trySend(MpvProtocol.setPause(paused))
    }

    override fun seekTo(seconds: Double) {
        lastPosition = Double.NEGATIVE_INFINITY
        commands.trySend(MpvProtocol.seekAbsolute(seconds))
    }

    override fun stop() {
        commands.trySend(MpvProtocol.stop())
    }

    override fun close() {
        closed = true
        commands.close()
        scope.cancel()
        connection?.dispose()
        connection = null
    }

    private suspend fun ensureConnected(): Connection {
        connection?.takeIf { it.process.isAlive }?.let { return it }
        connection?.dispose()
        connection = null

        val conn = startMpv()
        connection = conn
        send(conn, MpvProtocol.observeProperty(MpvProtocol.PROP_POSITION_ID, "time-pos"))
        send(conn, MpvProtocol.observeProperty(MpvProtocol.PROP_DURATION_ID, "duration"))
        scope.launch { readEvents(conn) }
        return conn
    }

    private suspend fun startMpv(): Connection {
        // A private (owner-only) directory, so other local users cannot reach mpv's control socket.
        val socketDir = Files.createTempDirectory(socketParentDirectory(), "metrolist-mpv")
        val socketPath = socketDir.resolve(SOCKET_NAME)

        val process = try {
            ProcessBuilder(
                mpvExecutable,
                "--no-video",
                "--idle=yes",
                "--no-terminal",
                "--really-quiet",
                "--force-window=no",
                "--ytdl-format=bestaudio/best",
                "--input-ipc-server=$socketPath",
            ).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
        } catch (e: IOException) {
            deleteSocket(socketPath)
            throw IOException("Could not start mpv. Install mpv and yt-dlp to enable playback.", e)
        }

        try {
            return connectWhenReady(process, socketPath)
        } catch (e: IOException) {
            process.destroy()
            deleteSocket(socketPath)
            throw e
        }
    }

    /** mpv creates the socket a moment after it starts; wait until it accepts connections. */
    private suspend fun connectWhenReady(process: Process, socketPath: Path): Connection {
        repeat(SOCKET_WAIT_ATTEMPTS) {
            if (!process.isAlive) {
                throw IOException("mpv exited immediately (exit code ${process.exitValue()}).")
            }
            if (Files.exists(socketPath)) {
                val channel = SocketChannel.open(StandardProtocolFamily.UNIX)
                try {
                    channel.connect(UnixDomainSocketAddress.of(socketPath))
                    return Connection(process, channel, socketPath)
                } catch (e: IOException) {
                    channel.close() // socket file exists but mpv is not listening yet; retry
                }
            }
            delay(SOCKET_WAIT_STEP_MS)
        }
        throw IOException("Timed out waiting for mpv to start.")
    }

    private fun send(conn: Connection, command: String) {
        val buffer = ByteBuffer.wrap((command + "\n").toByteArray(Charsets.UTF_8))
        while (buffer.hasRemaining()) conn.channel.write(buffer)
    }

    private fun readEvents(conn: Connection) {
        try {
            val reader = BufferedReader(Channels.newReader(conn.channel, Charsets.UTF_8))
            while (true) {
                val line = reader.readLine() ?: break
                val event = MpvProtocol.parseEvent(line) ?: continue
                if (event is BackendEvent.Position && !shouldEmit(event.seconds)) continue
                _events.tryEmit(event)
            }
        } catch (e: IOException) {
            // Socket closed: either we shut down, or mpv died.
        } finally {
            if (!closed && connection === conn) {
                conn.dispose()
                connection = null
                _events.tryEmit(BackendEvent.Error("mpv stopped unexpectedly."))
            }
        }
    }

    /** mpv reports the position many times a second; the UI only needs about four updates a second. */
    private fun shouldEmit(seconds: Double): Boolean {
        if (abs(seconds - lastPosition) < POSITION_EPSILON_SECONDS) return false
        lastPosition = seconds
        return true
    }

    private fun socketParentDirectory(): Path {
        val runtimeDir = System.getenv("XDG_RUNTIME_DIR")
        return Path.of(runtimeDir ?: System.getProperty("java.io.tmpdir"))
    }

    private companion object {
        /** Removes the socket file and its private directory. Safe to call more than once. */
        fun deleteSocket(socketPath: Path) {
            runCatching { Files.deleteIfExists(socketPath) }
            runCatching { Files.deleteIfExists(socketPath.parent) }
        }

        const val SOCKET_NAME = "mpv.sock"
        const val SOCKET_WAIT_ATTEMPTS = 100
        const val SOCKET_WAIT_STEP_MS = 50L
        const val POSITION_EPSILON_SECONDS = 0.25
    }
}
