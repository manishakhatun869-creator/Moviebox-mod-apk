package com.towfik.music.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<Long>
)

/**
 * Persists user-created playlists (name + track ids) in SharedPreferences.
 */
class PlaylistRepository(context: Context) {

    private val prefs = context.getSharedPreferences("towfik_music_playlists", Context.MODE_PRIVATE)

    fun all(): List<Playlist> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val ids = obj.optJSONArray("trackIds") ?: JSONArray()
                Playlist(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    trackIds = (0 until ids.length()).map { j -> ids.getLong(j) }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun create(name: String): Playlist {
        val existing = all().toMutableList()
        val playlist = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name.trim(),
            trackIds = emptyList()
        )
        existing += playlist
        save(existing)
        return playlist
    }

    fun delete(id: String) {
        save(all().filterNot { it.id == id })
    }

    fun addTrack(playlistId: String, trackId: Long) {
        save(all().map { p ->
            if (p.id == playlistId && !p.trackIds.contains(trackId))
                p.copy(trackIds = p.trackIds + trackId)
            else p
        })
    }

    private fun save(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            val ids = JSONArray()
            p.trackIds.forEach { ids.put(it) }
            obj.put("trackIds", ids)
            arr.put(obj)
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val KEY = "playlists_json"
    }
}
