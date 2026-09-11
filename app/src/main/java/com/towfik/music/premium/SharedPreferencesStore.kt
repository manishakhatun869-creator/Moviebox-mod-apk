package com.towfik.music.premium

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesStore(context: Context) : KeyValueStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("towfik_music_premium", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
}
