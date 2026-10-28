@file:Suppress("FunctionName", "unused")

package com.metrolist.shared.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.freedesktop.dbus.annotations.DBusBoundProperty
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.types.Variant
import kotlin.math.roundToInt

/** `org.mpris.MediaPlayer2`: the application-level half of the standard. */
@DBusInterfaceName(Mpris.ROOT_INTERFACE)
interface MprisRootInterface : DBusInterface {
    fun Raise()
    fun Quit()

    @DBusBoundProperty(name = "CanQuit")
    fun getCanQuit(): Boolean

    @DBusBoundProperty(name = "CanRaise")
    fun getCanRaise(): Boolean

    @DBusBoundProperty(name = "HasTrackList")
    fun getHasTrackList(): Boolean

    @DBusBoundProperty(name = "Identity")
    fun getIdentity(): String

    @DBusBoundProperty(name = "SupportedUriSchemes")
    fun getSupportedUriSchemes(): List<String>

    @DBusBoundProperty(name = "SupportedMimeTypes")
    fun getSupportedMimeTypes(): List<String>
}

/** `org.mpris.MediaPlayer2.Player`: transport controls and what is playing. */
@DBusInterfaceName(Mpris.PLAYER_INTERFACE)
interface MprisPlayerInterface : DBusInterface {
    fun Next()
    fun Previous()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Play()
    fun Seek(offsetMicros: Long)
    fun SetPosition(trackId: DBusPath, positionMicros: Long)
    fun OpenUri(uri: String)

    @DBusBoundProperty(name = "PlaybackStatus")
    fun getPlaybackStatus(): String

    @DBusBoundProperty(name = "LoopStatus")
    fun getLoopStatus(): String

    @DBusBoundProperty(name = "LoopStatus")
    fun setLoopStatus(value: String)

    @DBusBoundProperty(name = "Rate")
    fun getRate(): Double

    @DBusBoundProperty(name = "Shuffle")
    fun getShuffle(): Boolean

    @DBusBoundProperty(name = "Shuffle")
    fun setShuffle(value: Boolean)

    @DBusBoundProperty(name = "Metadata")
    fun getMetadata(): Map<String, Variant<*>>

    @DBusBoundProperty(name = "Volume")
    fun getVolume(): Double

    @DBusBoundProperty(name = "Volume")
    fun setVolume(value: Double)

    @DBusBoundProperty(name = "Position")
    fun getPosition(): Long

    @DBusBoundProperty(name = "MinimumRate")
    fun getMinimumRate(): Double

    @DBusBoundProperty(name = "MaximumRate")
    fun getMaximumRate(): Double

    @DBusBoundProperty(name = "CanGoNext")
    fun getCanGoNext(): Boolean

    @DBusBoundProperty(name = "CanGoPrevious")
    fun getCanGoPrevious(): Boolean

    @DBusBoundProperty(name = "CanPlay")
    fun getCanPlay(): Boolean

    @DBusBoundProperty(name = "CanPause")
    fun getCanPause(): Boolean

    @DBusBoundProperty(name = "CanSeek")
    fun getCanSeek(): Boolean

    @DBusBoundProperty(name = "CanControl")
    fun getCanControl(): Boolean
}

/**
 * The object other programs talk to. Every property is read live from the controller, so a client
 * that asks always gets the current answer; [MprisService] separately announces changes.
 */
class MprisObject(private val controller: PlayerController) : MprisRootInterface, MprisPlayerInterface {
    private fun snapshot() = controller.state.value
    private fun props() = MprisProps.from(snapshot())

    override fun isRemote(): Boolean = false
    override fun getObjectPath(): String = Mpris.OBJECT_PATH

    // --- org.mpris.MediaPlayer2 ---
    override fun Raise() {}
    override fun Quit() {}
    override fun getCanQuit(): Boolean = false
    override fun getCanRaise(): Boolean = false
    override fun getHasTrackList(): Boolean = false
    override fun getIdentity(): String = "Metrolist Linux"
    override fun getSupportedUriSchemes(): List<String> = emptyList()
    override fun getSupportedMimeTypes(): List<String> = emptyList()

    // --- org.mpris.MediaPlayer2.Player: methods ---
    override fun Next() = controller.next()
    override fun Previous() = controller.previous()
    override fun Pause() = controller.pause()
    override fun PlayPause() = controller.togglePlayPause()
    override fun Stop() = controller.stop()
    override fun Play() = controller.resume()

    override fun Seek(offsetMicros: Long) {
        controller.seekTo(snapshot().positionSeconds + offsetMicros / MICROS_PER_SECOND)
    }

    override fun SetPosition(trackId: DBusPath, positionMicros: Long) {
        val current = snapshot().current ?: return
        // The spec says to ignore the call if it refers to a track that is no longer current.
        if (trackId.path != Mpris.trackPath(current)) return
        controller.seekTo(positionMicros / MICROS_PER_SECOND)
    }

    override fun OpenUri(uri: String) {}

    // --- org.mpris.MediaPlayer2.Player: properties ---
    override fun getPlaybackStatus(): String = props().playbackStatus

    override fun getLoopStatus(): String = Mpris.loopStatus(snapshot().repeat)
    override fun setLoopStatus(value: String) {
        Mpris.repeatMode(value)?.let(controller::setRepeat)
    }

    override fun getRate(): Double = 1.0
    override fun getMinimumRate(): Double = 1.0
    override fun getMaximumRate(): Double = 1.0

    override fun getShuffle(): Boolean = snapshot().shuffle
    override fun setShuffle(value: Boolean) = controller.setShuffle(value)

    override fun getMetadata(): Map<String, Variant<*>> = metadataVariants(props().metadata)

    override fun getVolume(): Double = snapshot().volume / 100.0
    override fun setVolume(value: Double) = controller.setVolume((value * 100).roundToInt())

    override fun getPosition(): Long = Mpris.toMicros(snapshot().positionSeconds)

    override fun getCanGoNext(): Boolean = props().canGoNext
    override fun getCanGoPrevious(): Boolean = props().canGoPrevious
    override fun getCanPlay(): Boolean = props().canPlay
    override fun getCanPause(): Boolean = props().canPause
    override fun getCanSeek(): Boolean = props().canSeek
    override fun getCanControl(): Boolean = true

    private companion object {
        const val MICROS_PER_SECOND = 1_000_000.0
    }
}

internal fun metadataVariants(m: MprisMetadata?): Map<String, Variant<*>> {
    val map = LinkedHashMap<String, Variant<*>>()
    if (m == null) {
        map["mpris:trackid"] = Variant(DBusPath(Mpris.NO_TRACK_PATH))
        return map
    }
    map["mpris:trackid"] = Variant(DBusPath(m.trackId))
    map["mpris:length"] = Variant(m.lengthMicros)
    map["xesam:title"] = Variant(m.title)
    map["xesam:artist"] = Variant(m.artists, "as")
    map["xesam:album"] = Variant(m.album)
    m.artUrl?.let { map["mpris:artUrl"] = Variant(it) }
    return map
}

/** Builds the PropertiesChanged payload for the properties named in [names]. */
internal fun changedVariants(props: MprisProps, names: Set<String>): Map<String, Variant<*>> {
    val changed = LinkedHashMap<String, Variant<*>>()
    for (name in names) {
        changed[name] = when (name) {
            "PlaybackStatus" -> Variant(props.playbackStatus)
            "LoopStatus" -> Variant(props.loopStatus)
            "Shuffle" -> Variant(props.shuffle)
            "Volume" -> Variant(props.volume)
            "Metadata" -> Variant(metadataVariants(props.metadata), "a{sv}")
            "CanGoNext" -> Variant(props.canGoNext)
            "CanGoPrevious" -> Variant(props.canGoPrevious)
            "CanPlay" -> Variant(props.canPlay)
            "CanPause" -> Variant(props.canPause)
            "CanSeek" -> Variant(props.canSeek)
            else -> continue
        }
    }
    return changed
}

/**
 * Registers the player on the D-Bus session bus so desktop environments can show it in their
 * "now playing" widget and route media keys to it (test with `playerctl`). If there is no session
 * bus, or the name is taken, it logs one line and quietly does nothing.
 */
class MprisService(private val controller: PlayerController) {

    /** Runs until cancelled, then unregisters. */
    suspend fun serve() {
        withContext(Dispatchers.IO) {
            val connection = try {
                DBusConnectionBuilder.forSessionBus().build().also {
                    it.requestBusName(Mpris.BUS_NAME)
                    it.exportObject(Mpris.OBJECT_PATH, MprisObject(controller))
                }
            } catch (e: Throwable) {
                // Throwable, not Exception: a missing library or native transport is a LinkageError,
                // and media keys are optional, so they must never be able to take the app down.
                System.err.println("Media keys / MPRIS disabled: could not register on the D-Bus session bus (${e.message}).")
                return@withContext
            }

            try {
                var previous: MprisProps? = null
                controller.state.map { MprisProps.from(it) }.distinctUntilChanged().collect { props ->
                    val old = previous
                    previous = props
                    // The first snapshot is not an announcement: clients read the properties
                    // themselves when they connect. Only real changes are announced.
                    if (old != null) {
                        val changedNames = props.changedFrom(old)
                        if (changedNames.isNotEmpty()) announce(connection, props, changedNames)
                    }
                }
            } finally {
                runCatching { connection.close() }
            }
        }
    }

    private fun announce(connection: DBusConnection, props: MprisProps, names: Set<String>) {
        try {
            connection.sendMessage(
                Properties.PropertiesChanged(
                    Mpris.OBJECT_PATH,
                    Mpris.PLAYER_INTERFACE,
                    changedVariants(props, names),
                    emptyList(),
                )
            )
        } catch (e: Exception) {
            System.err.println("MPRIS: could not announce a change (${e.message}).")
        }
    }
}
