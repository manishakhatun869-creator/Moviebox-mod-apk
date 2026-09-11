package com.towfik.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.towfik.music.databinding.ActivityMainBinding
import com.towfik.music.playback.PlaybackService
import com.towfik.music.ui.LibraryFragment
import com.towfik.music.ui.NowPlayingFragment
import com.towfik.music.ui.PlaylistsFragment
import com.towfik.music.ui.PremiumFragment
import com.towfik.music.ui.SettingsFragment

class MainActivity : AppCompatActivity(), ServiceConnection {

    private lateinit var binding: ActivityMainBinding
    private var playback: PlaybackService? = null
    private val app: MusicApplication get() = application as MusicApplication

    private val miniListener: (PlaybackService.UiState) -> Unit = { state ->
        runOnUiThread { renderMini(state) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_library -> show(LibraryFragment(), "library")
                R.id.nav_playlists -> show(PlaylistsFragment(), "playlists")
                R.id.nav_premium -> show(PremiumFragment(), "premium")
                R.id.nav_settings -> show(SettingsFragment(), "settings")
                else -> false
            }
            true
        }

        binding.miniPlayer.root.setOnClickListener { showNowPlaying() }
        binding.miniPlayer.miniPlay.setOnClickListener { playback?.toggle() }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_library
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, PlaybackService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, this, Context.BIND_AUTO_CREATE)
        refreshPremiumBadge()
    }

    override fun onStop() {
        playback?.removeListener(miniListener)
        unbindService(this)
        playback = null
        super.onStop()
    }

    fun requirePlayback(): PlaybackService? = playback

    fun showNowPlaying() {
        if (playback?.currentState()?.current != null) {
            NowPlayingFragment().show(supportFragmentManager, "now_playing")
        }
    }

    fun goPremium() {
        binding.bottomNav.selectedItemId = R.id.nav_premium
    }

    fun refreshPremiumBadge() {
        binding.toolbarBadge.visibility =
            if (app.premium.status().isPremium) View.VISIBLE else View.GONE
    }

    private fun renderMini(state: PlaybackService.UiState) {
        val track = state.current
        if (track == null) {
            binding.miniPlayer.root.visibility = View.GONE
            return
        }
        binding.miniPlayer.root.visibility = View.VISIBLE
        binding.miniPlayer.miniTitle.text = track.title
        binding.miniPlayer.miniArtist.text = track.artist
        binding.miniPlayer.miniPlay.setImageResource(
            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun show(fragment: Fragment, tag: String): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navHost, fragment, tag)
            .commit()
        return true
    }

    // ---- ServiceConnection ----
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as? PlaybackService.LocalBinder ?: return
        playback = binder.service()
        playback?.addListener(miniListener)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        playback?.removeListener(miniListener)
        playback = null
    }
}
