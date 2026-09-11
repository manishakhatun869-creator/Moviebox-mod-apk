package com.towfik.music.playback

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.towfik.music.MainActivity
import com.towfik.music.R
import com.towfik.music.data.Track

/**
 * Owns audio playback for the whole app. Plays only files already present on
 * the device; it never downloads or streams content.
 */
class PlaybackService : Service() {

    data class UiState(
        val isPlaying: Boolean,
        val current: Track?,
        val index: Int,
        val queueSize: Int
    )

    private var player: MediaPlayer? = null
    private var session: MediaSessionCompat? = null
    private var queue: List<Track> = emptyList()
    private var index: Int = -1
    private var shuffle: Boolean = false
    private var repeat: Boolean = false
    private val listeners = mutableListOf<(UiState) -> Unit>()

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun service(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        val s = MediaSessionCompat(this, SESSION_TAG)
        s.setCallback(SessionCallback())
        s.isActive = true
        session = s
    }

    override fun onDestroy() {
        player?.release()
        player = null
        session?.release()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> toggle()
            ACTION_NEXT -> next(auto = false)
            ACTION_PREV -> previous()
            ACTION_STOP -> stopAll()
        }
        return START_NOT_STICKY
    }

    // ---- public API used by the UI ----

    fun addListener(l: (UiState) -> Unit) {
        listeners += l
        l(currentState())
    }

    fun removeListener(l: (UiState) -> Unit) {
        listeners -= l
    }

    fun currentState(): UiState =
        UiState(
            isPlaying = player?.isPlaying == true,
            current = queue.getOrNull(index),
            index = index,
            queueSize = queue.size
        )

    fun currentPositionMs(): Long = (player?.currentPosition ?: 0).toLong()

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        queue = tracks
        index = startIndex.coerceIn(0, tracks.size - 1)
        startCurrent()
    }

    fun toggle() {
        val mp = player ?: return
        if (mp.isPlaying) pause() else resume()
    }

    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
        updateState(playing = false)
        stopForegroundKeep()
    }

    fun resume() {
        player?.takeIf { !it.isPlaying }?.start()
        updateState(playing = true)
        showNotification()
    }

    fun next(auto: Boolean) {
        if (queue.isEmpty()) return
        index = if (shuffle) (0 until queue.size).filter { it != index }.randomOrNull() ?: index
        else advance(index + 1, auto)
        startCurrent()
    }

    fun previous() {
        if (queue.isEmpty()) return
        val mp = player
        if (mp != null && mp.currentPosition > 3000) {
            mp.seekTo(0)
            return
        }
        index = (index - 1).coerceAtLeast(0)
        startCurrent()
    }

    fun seekTo(ms: Long) {
        player?.seekTo(ms.toInt())
    }

    fun setShuffle(on: Boolean) {
        shuffle = on
    }

    fun setRepeat(on: Boolean) {
        repeat = on
    }

    fun stopAll() {
        player?.stop()
        player?.release()
        player = null
        updateState(playing = false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---- internals ----

    private fun advance(nextIndex: Int, auto: Boolean): Int = when {
        nextIndex < queue.size -> nextIndex
        repeat -> 0
        else -> if (auto) index else 0
    }

    private fun startCurrent() {
        val track = queue.getOrNull(index) ?: return
        player?.release()
        val mp = MediaPlayer()
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        mp.setWakeMode(android.os.PowerManager.PARTIAL_WAKE_LOCK)
        mp.setOnPreparedListener { p ->
            p.start()
            updateState(playing = true)
            showNotification()
        }
        mp.setOnCompletionListener { next(auto = true) }
        runCatching {
            mp.setDataSource(this, track.uri)
            mp.prepareAsync()
        }.onFailure {
            // Skip unplayable files rather than crashing.
            mp.release()
            next(auto = true)
            return
        }
        player = mp
        session?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)
                .build()
        )
        updateState(playing = false)
    }

    private fun updateState(playing: Boolean) {
        session?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(
                    if (playing) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED,
                    currentPositionMs(),
                    if (playing) 1f else 0f
                )
                .build()
        )
        val state = currentState().copy(isPlaying = playing)
        listeners.forEach { it(state) }
    }

    private fun ensureChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = android.app.NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_playback_name),
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            ch.description = getString(R.string.channel_playback_description)
            nm.createNotificationChannel(ch)
        }
    }

    private fun showNotification() {
        val track = queue.getOrNull(index) ?: return
        val launch = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val style = MediaStyle()
            .setMediaSession(session?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setContentIntent(launch)
            .setStyle(style)
            .setOngoing(player?.isPlaying == true)
            .addAction(
                R.drawable.ic_skip_previous,
                getString(R.string.previous_track),
                serviceIntent(ACTION_PREV, 1)
            )
            .addAction(
                if (player?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play,
                getString(if (player?.isPlaying == true) R.string.pause else R.string.play),
                serviceIntent(ACTION_TOGGLE, 2)
            )
            .addAction(
                R.drawable.ic_skip_next,
                getString(R.string.next_track),
                serviceIntent(ACTION_NEXT, 3)
            )

        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, builder.build(),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    private fun stopForegroundKeep() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, PlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private inner class SessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() { resume() }
        override fun onPause() { pause() }
        override fun onSkipToNext() { next(auto = false) }
        override fun onSkipToPrevious() { previous() }
        override fun onSeekTo(pos: Long) { seekTo(pos) }
    }

    companion object {
        private const val SESSION_TAG = "towfik_music_session"
        private const val CHANNEL_ID = "towfik_music_playback"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_TOGGLE = "com.towfik.music.action.TOGGLE"
        const val ACTION_NEXT = "com.towfik.music.action.NEXT"
        const val ACTION_PREV = "com.towfik.music.action.PREV"
        const val ACTION_STOP = "com.towfik.music.action.STOP"
    }
}
