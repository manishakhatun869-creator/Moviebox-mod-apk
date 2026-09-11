package com.towfik.music.premium

/**
 * Source of truth for the Premium entitlement on this device.
 *
 * Premium is only ever granted from a completed, acknowledged Google Play
 * purchase (see [com.towfik.music.premium.BillingManager]). There is no
 * offline unlock, no patched check: [hasFeature] simply compares the stored,
 * purchase-derived expiry against the current time.
 */
class PremiumRepository(
    private val store: KeyValueStore,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    data class Status(
        val isPremium: Boolean,
        val plan: Plan?,
        val expiresAtMs: Long
    )

    fun status(): Status {
        val expiresAt = store.getLong(KEY_EXPIRES, 0L)
        val planId = store.getString(KEY_PLAN)
        val isPremium = expiresAt > clock()
        return Status(
            isPremium = isPremium,
            plan = if (isPremium) Plan.fromProductId(planId.orEmpty()) else null,
            expiresAtMs = expiresAt
        )
    }

    fun hasFeature(feature: Feature): Boolean = status().isPremium

    /** Called by [BillingManager] after a purchase is verified and acknowledged. */
    fun grant(plan: Plan, token: String, nowMs: Long = clock()) {
        val expiry = nowMs + plan.durationDays() * 24L * 60L * 60L * 1000L
        store.putLong(KEY_EXPIRES, expiry)
        store.putString(KEY_PLAN, plan.productId)
        store.putString(KEY_TOKEN, token)
        store.putBoolean(KEY_LIFETIME, plan == Plan.LIFETIME)
    }

    /** Re-verify against Play; clears entitlement when the purchase is gone. */
    fun reconcile(activeProductIds: Set<String>, activeTokens: Set<String>) {
        val current = status()
        val stillActive = activeProductIds.firstNotNullOfOrNull { Plan.fromProductId(it) }
        if (stillActive == null) {
            if (current.plan != Plan.LIFETIME || !store.getBoolean(KEY_LIFETIME, false)) {
                clear()
            }
        } else if (stillActive != Plan.LIFETIME) {
            store.putString(KEY_PLAN, stillActive.productId)
        }
        activeTokens.firstOrNull()?.let { store.putString(KEY_TOKEN, it) }
    }

    fun clear() {
        store.putLong(KEY_EXPIRES, 0L)
        store.putString(KEY_PLAN, null)
        store.putString(KEY_TOKEN, null)
        store.putBoolean(KEY_LIFETIME, false)
    }

    companion object {
        private const val KEY_EXPIRES = "entitlement_expires_ms"
        private const val KEY_PLAN = "entitlement_plan"
        private const val KEY_TOKEN = "entitlement_token"
        private const val KEY_LIFETIME = "entitlement_lifetime"
    }
}
