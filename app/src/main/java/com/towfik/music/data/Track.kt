package com.towfik.music.data

import android.net.Uri

/**
 * A single audio file discovered on the user's own device via MediaStore.
 * Towfik Music never downloads or streams content; it only indexes what the
 * user already owns locally.
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri
) {
    val uriString: String get() = uri.toString()

    companion object {
        fun contentUri(id: Long): Uri =
            Uri.parse("content://media/external/audio/media/$id")
    }
}
