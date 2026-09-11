package com.towfik.music.premium

/**
 * Minimal persistence abstraction so the premium entitlement logic can be
 * unit-tested on the JVM without Android. The production implementation is
 * backed by SharedPreferences.
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}
