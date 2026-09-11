package com.towfik.music

import android.app.Application
import com.towfik.music.data.PlaylistRepository
import com.towfik.music.data.TrackRepository
import com.towfik.music.premium.BillingManager
import com.towfik.music.premium.PremiumRepository
import com.towfik.music.premium.SharedPreferencesStore

/**
 * Process-wide singletons so every screen shares the same state.
 */
class MusicApplication : Application() {

    lateinit var tracks: TrackRepository
        private set
    lateinit var playlists: PlaylistRepository
        private set
    lateinit var premium: PremiumRepository
        private set
    lateinit var billing: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        tracks = TrackRepository()
        playlists = PlaylistRepository(this)
        premium = PremiumRepository(SharedPreferencesStore(this))
        billing = BillingManager(this, premium)
    }
}
