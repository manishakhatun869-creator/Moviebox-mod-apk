package com.towfik.music.premium

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumRepositoryTest {

    private val store = FakeKeyValueStore()
    private var now = 1_000_000L
    private val repo = PremiumRepository(store) { now }

    @Test
    fun startsFree() {
        assertFalse(repo.status().isPremium)
        assertFalse(repo.hasFeature(Feature.UNLIMITED_PLAYLISTS))
    }

    @Test
    fun grantMakesPremiumUntilExpiry() {
        repo.grant(Plan.MONTHLY, "token-1")
        assertTrue(repo.status().isPremium)
        assertTrue(repo.hasFeature(Feature.SLEEP_TIMER))

        // Just before 30 days: still premium.
        now += 29L * 24 * 60 * 60 * 1000
        assertTrue(repo.status().isPremium)

        // Just after 30 days: expired.
        now += 2L * 24 * 60 * 60 * 1000
        assertFalse(repo.status().isPremium)
    }

    @Test
    fun lifetimeDoesNotExpire() {
        repo.grant(Plan.LIFETIME, "token-2")
        now += 400L * 24 * 60 * 60 * 1000
        assertTrue(repo.status().isPremium)
    }

    @Test
    fun clearRemovesPremium() {
        repo.grant(Plan.YEARLY, "token-3")
        assertTrue(repo.status().isPremium)
        repo.clear()
        assertFalse(repo.status().isPremium)
    }

    @Test
    fun reconcileClearsWhenNoActivePurchase() {
        repo.grant(Plan.MONTHLY, "token-4")
        repo.reconcile(activeProductIds = emptySet(), activeTokens = emptySet())
        assertFalse(repo.status().isPremium)
    }
}
