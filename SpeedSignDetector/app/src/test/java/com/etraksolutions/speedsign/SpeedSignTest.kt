package com.etraksolutions.speedsign

import com.etraksolutions.speedsign.domain.model.DetectionConfig
import com.etraksolutions.speedsign.domain.model.SpeedSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the SpeedSign domain model.
 *
 * Note: Tests that require android.graphics.RectF are in androidTest
 * as they require the Android framework.
 */
class SpeedSignTest {

    @Test
    fun `detection config has correct defaults`() {
        val config = DetectionConfig()

        assertEquals(0.6f, config.minConfidence, 0.001f)
        assertTrue(config.enableOverlay)
        assertEquals(100L, config.processingInterval)
        assertEquals(SpeedSign.VALID_QUEBEC_SPEED_LIMITS, config.targetSpeedLimits)
    }

    @Test
    fun `valid Quebec speed limits set contains expected values`() {
        val expectedSpeeds = setOf(30, 40, 50, 60, 70, 80, 90, 100, 110)
        assertEquals(expectedSpeeds, SpeedSign.VALID_QUEBEC_SPEED_LIMITS)
    }

    @Test
    fun `high confidence threshold constant is correct`() {
        assertEquals(0.75f, SpeedSign.HIGH_CONFIDENCE_THRESHOLD, 0.001f)
    }
}
