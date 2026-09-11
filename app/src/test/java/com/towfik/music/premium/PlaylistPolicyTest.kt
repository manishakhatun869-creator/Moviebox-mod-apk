package com.towfik.music.premium

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistPolicyTest {

    @Test
    fun freeCanCreateBelowLimit() {
        assertTrue(PlaylistPolicy.canCreatePlaylist(0, isPremium = false))
        assertTrue(PlaylistPolicy.canCreatePlaylist(1, isPremium = false))
    }

    @Test
    fun freeBlockedAtLimit() {
        assertFalse(PlaylistPolicy.canCreatePlaylist(2, isPremium = false))
        assertFalse(PlaylistPolicy.canCreatePlaylist(5, isPremium = false))
    }

    @Test
    fun premiumAlwaysAllowed() {
        assertTrue(PlaylistPolicy.canCreatePlaylist(2, isPremium = true))
        assertTrue(PlaylistPolicy.canCreatePlaylist(1000, isPremium = true))
    }
}
