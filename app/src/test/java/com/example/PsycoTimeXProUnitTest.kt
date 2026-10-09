package com.example

import com.example.service.AiSportsScienceAnalyzer
import org.junit.Assert.*
import org.junit.Test

class PsycoTimeXProUnitTest {

    @Test
    fun testMotionDetection_StationaryCamera_NoFakeStats() {
        // When motion energy is near zero (stationary camera / room)
        val result = AiSportsScienceAnalyzer.analyzeMovement(
            motionEnergy = 0.05f,
            recordedTimeMillis = 10450L,
            eventCategory = "Track",
            eventName = "100m Sprint"
        )

        // Must strictly report no motion detected without fake metrics
        assertFalse(result.hasMotion)
        assertEquals(0.0, result.maxVelocityMs, 0.001)
        assertEquals(0, result.cadenceSpM)
        assertTrue(result.statusDescription.contains("Tiada Pergerakan Dikesan"))
    }

    @Test
    fun testMotionDetection_ActiveRunner_GeneratesValidKinematics() {
        // When runner crosses optical threshold
        val result = AiSportsScienceAnalyzer.analyzeMovement(
            motionEnergy = 0.85f,
            recordedTimeMillis = 10450L,
            eventCategory = "Track",
            eventName = "100m Sprint"
        )

        assertTrue(result.hasMotion)
        assertTrue(result.cadenceSpM > 200)
        assertTrue(result.maxVelocityMs > 8.0)
        assertTrue(result.torsoLeanAngleDeg > 10.0)
        assertTrue(result.groundContactTimeMs in 90..140)
    }

    @Test
    fun testFieldEventKinematics() {
        val result = AiSportsScienceAnalyzer.analyzeMovement(
            motionEnergy = 0.70f,
            recordedTimeMillis = 0L,
            eventCategory = "Field",
            eventName = "Lompat Jauh"
        )

        assertTrue(result.hasMotion)
        assertTrue(result.takeoffOrReleaseAngleDeg > 15.0)
        assertTrue(result.approachSpeedMs > 6.0)
    }
}
