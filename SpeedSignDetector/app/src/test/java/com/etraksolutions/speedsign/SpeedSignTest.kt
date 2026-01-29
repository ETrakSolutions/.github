package com.etraksolutions.speedsign

import android.graphics.RectF
import com.etraksolutions.speedsign.domain.model.DetectionConfig
import com.etraksolutions.speedsign.domain.model.SpeedSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the SpeedSign domain model.
 */
class SpeedSignTest {

    @Test
    fun `valid Quebec speed limits are recognized`() {
        val validSpeeds = listOf(30, 40, 50, 60, 70, 80, 90, 100, 110)

        validSpeeds.forEach { speed ->
            val sign = createSpeedSign(speed)
            assertTrue(
                "Speed $speed should be a valid Quebec speed limit",
                sign.isValidQuebecSpeedLimit
            )
        }
    }

    @Test
    fun `invalid speed limits are rejected`() {
        val invalidSpeeds = listOf(25, 35, 45, 55, 65, 75, 85, 95, 105, 120)

        invalidSpeeds.forEach { speed ->
            val sign = createSpeedSign(speed)
            assertFalse(
                "Speed $speed should not be a valid Quebec speed limit",
                sign.isValidQuebecSpeedLimit
            )
        }
    }

    @Test
    fun `high confidence threshold is correctly evaluated`() {
        val highConfidenceSign = createSpeedSign(50, confidence = 0.85f)
        val lowConfidenceSign = createSpeedSign(50, confidence = 0.6f)
        val borderlineSign = createSpeedSign(50, confidence = 0.75f)

        assertTrue(highConfidenceSign.isHighConfidence)
        assertFalse(lowConfidenceSign.isHighConfidence)
        assertTrue(borderlineSign.isHighConfidence)
    }

    @Test
    fun `detection config has correct defaults`() {
        val config = DetectionConfig()

        assertEquals(0.6f, config.minConfidence)
        assertTrue(config.enableOverlay)
        assertEquals(100L, config.processingInterval)
        assertEquals(SpeedSign.VALID_QUEBEC_SPEED_LIMITS, config.targetSpeedLimits)
    }

    @Test
    fun `valid Quebec speed limits set contains expected values`() {
        val expectedSpeeds = setOf(30, 40, 50, 60, 70, 80, 90, 100, 110)
        assertEquals(expectedSpeeds, SpeedSign.VALID_QUEBEC_SPEED_LIMITS)
    }

    private fun createSpeedSign(
        speed: Int,
        confidence: Float = 0.8f
    ): SpeedSign {
        return SpeedSign(
            speedLimit = speed,
            confidence = confidence,
            boundingBox = RectF(0f, 0f, 100f, 100f)
        )
    }
}
