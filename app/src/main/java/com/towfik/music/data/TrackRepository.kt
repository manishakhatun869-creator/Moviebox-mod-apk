package com.towfik.music.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/**
 * Reads the audio library that already exists on this device.
 */
class TrackRepository {

    fun load(context: Context): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown"
                    val artist = cursor.getString(artistCol) ?: ""
                    val album = cursor.getString(albumCol) ?: ""
                    val duration = cursor.getLong(durationCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    tracks += Track(id, title, artist, album, duration, uri)
                }
            }
        return tracks
    }
}
