package com.towfik.music.premium

/**
 * The purchasable Premium offers, mapped 1:1 to Google Play product ids.
 * These must be created as in-app products / subscriptions in Play Console
 * before the purchase flow can complete on a real device.
 */
enum class Plan(val productId: String) {
    MONTHLY("towfik_music_premium_monthly"),
    YEARLY("towfik_music_premium_yearly"),
    LIFETIME("towfik_music_premium_lifetime");

    companion object {
        fun fromProductId(id: String): Plan? = entries.firstOrNull { it.productId == id }
    }
}

/** Subscription length in days used to compute entitlement expiry. */
fun Plan.durationDays(): Int = when (this) {
    Plan.MONTHLY -> 30
    Plan.YEARLY -> 365
    Plan.LIFETIME -> 36_500 // ~100 years: effectively no expiry
}

/** Features that require an active Premium entitlement. */
enum class Feature {
    UNLIMITED_PLAYLISTS,
    EQUALIZER,
    PLAYBACK_SPEED,
    ACCENT_THEMES,
    SLEEP_TIMER,
    GAPLESS_QUEUE
}
