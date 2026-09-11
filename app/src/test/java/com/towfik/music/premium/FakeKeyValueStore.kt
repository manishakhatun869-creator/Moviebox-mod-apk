package com.towfik.music.premium

class FakeKeyValueStore : KeyValueStore {
    private val strings = mutableMapOf<String, String>()
    private val longs = mutableMapOf<String, Long>()
    private val booleans = mutableMapOf<String, Boolean>()

    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String?) {
        if (value == null) strings.remove(key) else strings[key] = value
    }

    override fun getLong(key: String, default: Long): Long = longs[key] ?: default
    override fun putLong(key: String, value: Long) { longs[key] = value }

    override fun getBoolean(key: String, default: Boolean): Boolean = booleans[key] ?: default
    override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
}
