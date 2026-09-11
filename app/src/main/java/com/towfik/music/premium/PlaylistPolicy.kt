package com.towfik.music.premium

/** Free-vs-Premium rules for the playlist feature. Pure logic, unit-tested. */
object PlaylistPolicy {

    const val FREE_PLAYLIST_LIMIT = 2

    fun canCreatePlaylist(currentCount: Int, isPremium: Boolean): Boolean =
        isPremium || currentCount < FREE_PLAYLIST_LIMIT
}
