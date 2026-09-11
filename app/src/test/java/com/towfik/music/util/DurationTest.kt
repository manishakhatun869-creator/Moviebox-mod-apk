package com.towfik.music.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationTest {

    @Test
    fun formatsMinutesSeconds() {
        assertEquals("12:34", Duration.format(754_000))
        assertEquals("0:00", Duration.format(0))
        assertEquals("0:05", Duration.format(5_000))
    }

    @Test
    fun formatsHours() {
        assertEquals("1:02:34", Duration.format(3_754_000))
    }

    @Test
    fun clampsNegative() {
        assertEquals("0:00", Duration.format(-100))
    }
}
