package com.neuroflow.app.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Adversarial stress tests written by challenger_m1_1.
 * Tests empirical convergence speed, circular wrap-around edge cases,
 * extreme values, NaN handling, Bessel I0 stability, and impulse dynamics.
 */
class CircadianAdversarialStressTest {

    @Test
    fun testLargeJumpConvergence_09to15() {
        // Prior: 09:00, Variance: 4.0, SampleCount: 0.0
        val prior = LearnedCircadianProfile(
            peakHourMean = 9.0,
            peakHourVariance = 4.0,
            sampleCount = 0.0
        )

        // Standard behavioral observation at 15:00 (minute 900)
        val obs1 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 1.0)
        val p1 = AdaptiveCircadianLearner.updateProfile(prior, obs1, kappa = 4.5)

        val obs2 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 1.0)
        val p2 = AdaptiveCircadianLearner.updateProfile(p1, obs2, kappa = 4.5)

        val obs3 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 1.0)
        val p3 = AdaptiveCircadianLearner.updateProfile(p2, obs3, kappa = 4.5)

        println("09:00 -> 15:00 jump:")
        println("  After session 1: mean=${p1.peakHourMean}, var=${p1.peakHourVariance}, conf=${p1.confidence}")
        println("  After session 2: mean=${p2.peakHourMean}, var=${p2.peakHourVariance}, conf=${p2.confidence}")
        println("  After session 3: mean=${p3.peakHourMean}, var=${p3.peakHourVariance}, conf=${p3.confidence}")

        // Check if it converges within 2-3 sessions
        val distAfter2 = abs(p2.peakHourMean - 15.0)
        val distAfter3 = abs(p3.peakHourMean - 15.0)
        println("  Dist after 2 sessions: $distAfter2 hours (${distAfter2 * 60} mins)")
        println("  Dist after 3 sessions: $distAfter3 hours (${distAfter3 * 60} mins)")

        // After 2 sessions: 14.07h -> dist = 0.93h
        // After 3 sessions: 14.37h -> dist = 0.63h
        assertTrue("After 3 sessions with weight=1.0, peak hour should be within 1.0h of 15:00", distAfter3 < 1.0)
    }

    @Test
    fun testLargeJumpConvergence_Weighted_09to15() {
        val prior = LearnedCircadianProfile(
            peakHourMean = 9.0,
            peakHourVariance = 4.0,
            sampleCount = 0.0
        )

        // With stronger user focus sessions (weight = 1.5, rating = 0.95 like FastCircadianLearnerTest)
        val obs1 = BehavioralObservation(minuteOfDay = 900, productivityRating = 0.95, weight = 1.5)
        val p1 = AdaptiveCircadianLearner.updateProfile(prior, obs1, kappa = 4.5)

        val obs2 = BehavioralObservation(minuteOfDay = 900, productivityRating = 0.95, weight = 1.5)
        val p2 = AdaptiveCircadianLearner.updateProfile(p1, obs2, kappa = 4.5)

        val obs3 = BehavioralObservation(minuteOfDay = 900, productivityRating = 1.0, weight = 2.0)
        val p3 = AdaptiveCircadianLearner.updateProfile(p2, obs3, kappa = 4.5)

        println("09:00 -> 15:00 weighted jump:")
        println("  After session 1: mean=${p1.peakHourMean}")
        println("  After session 2: mean=${p2.peakHourMean}")
        println("  After session 3: mean=${p3.peakHourMean}")

        val distAfter3 = abs(p3.peakHourMean - 15.0)
        assertTrue("Weighted session converges within 0.45h of 15:00", distAfter3 < 0.45)
    }

    @Test
    fun testCircularWrapAround_MidnightBoundaries() {
        // Test 23:00 -> 01:00 (+2h shortest path)
        val prior23 = LearnedCircadianProfile(peakHourMean = 23.0, peakHourVariance = 4.0, sampleCount = 0.0)
        val obs01 = BehavioralObservation(minuteOfDay = 60, productivityRating = 1.0, weight = 1.0)
        val pAfter01 = AdaptiveCircadianLearner.updateProfile(prior23, obs01)
        println("23:00 -> 01:00 update: mean=${pAfter01.peakHourMean}")
        // Shortest diff is +2.0h. Kalman gain ~ 0.727.
        // updatedMean = (23.0 + 0.727 * 2.0) = 24.455 -> 0.455h (00:27)
        assertTrue("Peak should cross midnight forward into 00:xx", pAfter01.peakHourMean < 1.0)

        // Test 01:00 -> 23:00 (-2h shortest path)
        val prior01 = LearnedCircadianProfile(peakHourMean = 1.0, peakHourVariance = 4.0, sampleCount = 0.0)
        val obs23 = BehavioralObservation(minuteOfDay = 1380, productivityRating = 1.0, weight = 1.0)
        val pAfter23 = AdaptiveCircadianLearner.updateProfile(prior01, obs23)
        println("01:00 -> 23:00 update: mean=${pAfter23.peakHourMean}")
        // Shortest diff is -2.0h. updatedMean = (1.0 - 0.727 * 2.0) = -0.455 -> 23.545h (23:32)
        assertTrue("Peak should cross midnight backward into 23:xx", pAfter23.peakHourMean > 23.0)

        // Test boundary at 23:59 -> 00:01
        val priorMidnight = LearnedCircadianProfile(peakHourMean = 23.98, peakHourVariance = 2.0, sampleCount = 1.0)
        val obsMidnight = BehavioralObservation(minuteOfDay = 1, productivityRating = 1.0, weight = 1.0)
        val pMidnight = AdaptiveCircadianLearner.updateProfile(priorMidnight, obsMidnight)
        assertTrue("Peak should stay within 0.1h of midnight", pMidnight.peakHourMean > 23.9 || pMidnight.peakHourMean < 0.1)
    }

    @Test
    fun testBesselI0_NumericalStability() {
        // x = 0
        assertEquals(1.0, AdaptiveCircadianLearner.besselI0(0.0), 1e-6)

        // Continuity across the x = 3.75 boundary
        val valBelow = AdaptiveCircadianLearner.besselI0(3.749999)
        val valAbove = AdaptiveCircadianLearner.besselI0(3.750001)
        println("Bessel I0 at 3.75 boundary: below=$valBelow, above=$valAbove")
        assertEquals("Bessel I0 should be continuous at 3.75", valBelow, valAbove, 1e-4)

        // Negative values: I0(-x) == I0(x)
        assertEquals(AdaptiveCircadianLearner.besselI0(4.5), AdaptiveCircadianLearner.besselI0(-4.5), 1e-6)

        // Standard kappa = 4.5: true Bessel I0(4.5) is ~17.481
        val i0_45 = AdaptiveCircadianLearner.besselI0(4.5)
        println("Bessel I0(4.5) = $i0_45")
        assertTrue("I0(4.5) should be ~17.481", abs(i0_45 - 17.481) < 0.1)

        // Large kappa: up to 100
        val i0_100 = AdaptiveCircadianLearner.besselI0(100.0)
        println("Bessel I0(100.0) = $i0_100")
        assertFalse("I0(100.0) should be finite", i0_100.isInfinite() || i0_100.isNaN())

        // Extreme kappa: 700
        val i0_700 = AdaptiveCircadianLearner.besselI0(700.0)
        println("Bessel I0(700.0) = $i0_700")
        assertFalse("I0(700.0) should be finite", i0_700.isInfinite() || i0_700.isNaN())

        // Overflow threshold for exp(x): x > 709.78
        val i0_800 = AdaptiveCircadianLearner.besselI0(800.0)
        println("Bessel I0(800.0) = $i0_800")
        assertTrue("I0(800.0) overflows to Infinity in double precision", i0_800.isInfinite())

        // Von Mises density with extreme kappa (now safe via computeVonMisesDensitySafe)
        val densityExtreme = AdaptiveCircadianLearner.computeVonMisesDensity(14.0, 14.0, kappa = 800.0)
        println("Von Mises density with kappa=800: $densityExtreme")
        assertFalse("Von Mises density must not be NaN even when kappa > 709 due to safe scaling", densityExtreme.isNaN())
        assertTrue("Von Mises density is non-negative", densityExtreme >= 0.0)

        // However, hourly multipliers do NOT use besselI0 and do NOT become NaN
        val multipliersExtreme = AdaptiveCircadianLearner.computeVonMisesHourlyMultipliers(14.0, kappa = 800.0)
        assertEquals(24, multipliersExtreme.size)
        assertEquals(1.0, multipliersExtreme[14], 1e-6)
        assertEquals(0.0, multipliersExtreme[2], 1e-6)
        assertFalse("Multipliers must not be NaN even at extreme kappa", multipliersExtreme.any { it.isNaN() })
    }

    @Test
    fun testZeroVarianceAndExtremePriorHandling() {
        // Zero variance prior
        val zeroVarPrior = LearnedCircadianProfile(
            peakHourMean = 10.0,
            peakHourVariance = 0.0,
            sampleCount = 0.0
        )
        val obs = BehavioralObservation(minuteOfDay = 720, productivityRating = 1.0, weight = 1.0)
        val updated = AdaptiveCircadianLearner.updateProfile(zeroVarPrior, obs)
        println("Zero variance update: mean=${updated.peakHourMean}, var=${updated.peakHourVariance}")
        assertFalse("Mean should not be NaN", updated.peakHourMean.isNaN())
        assertFalse("Var should not be NaN", updated.peakHourVariance.isNaN())
        assertTrue("Variance has a lower floor of PROCESS_NOISE_Q (0.05)", updated.peakHourVariance >= 0.05)

        // Extremely high variance prior
        val hugeVarPrior = LearnedCircadianProfile(
            peakHourMean = 10.0,
            peakHourVariance = 1000.0,
            sampleCount = 0.0
        )
        val updatedHuge = AdaptiveCircadianLearner.updateProfile(hugeVarPrior, obs)
        assertFalse("Updated var should be capped", updatedHuge.peakHourVariance > 12.0)
    }

    @Test
    fun testMealPostPrandialDipDiscontinuityAt180m() {
        val mealBlock = CognitiveBlockWindow(
            startMinuteOfDay = 720, // 12:00
            endMinuteOfDay = 780,   // 13:00
            category = CognitiveBlockCategory.MEAL,
            intensity = 1.0
        )

        // At 179 minutes post-meal start (14:59)
        val impulse179 = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 179, listOf(mealBlock))
        // At 180 minutes post-meal start (15:00)
        val impulse180 = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 180, listOf(mealBlock))
        // At 181 minutes post-meal start (15:01)
        val impulse181 = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 181, listOf(mealBlock))

        println("Meal impulse at 179m: $impulse179")
        println("Meal impulse at 180m: $impulse180")
        println("Meal impulse at 181m: $impulse181")

        // Peak dip at 60m
        val impulsePeak = PhysiologicalImpulseCalculator.computeMealImpulse(720 + 60, listOf(mealBlock))
        assertEquals(-12.0, impulsePeak, 1e-4)

        // Cliff between 180m and 181m
        val cliff = abs(impulse180 - impulse181)
        println("Discontinuity cliff at 180m boundary: $cliff points")
        assertTrue("Cliff at 180m cutoff is ~4.87 points", cliff > 4.5)
    }

    @Test
    fun testExerciseImpulseParametersVersusSpec() {
        val workout = CognitiveBlockWindow(
            startMinuteOfDay = 600, // 10:00
            endMinuteOfDay = 660,   // 11:00
            category = CognitiveBlockCategory.EXERCISE,
            intensity = 1.0
        )

        val duringImpulse = PhysiologicalImpulseCalculator.computeExerciseImpulse(630, listOf(workout))
        // Minute 661 is post-workout (1 min after end at 660, in 15-minute autonomic ramp)
        val postImpulseRamp = PhysiologicalImpulseCalculator.computeExerciseImpulse(661, listOf(workout))
        // Minute 675 is post-workout (15 min after end at 660, fully transitioned into BDNF boost)
        val postImpulsePeak = PhysiologicalImpulseCalculator.computeExerciseImpulse(675, listOf(workout))

        // Documenting the empirically observed values in implementation:
        assertEquals(-8.0, duringImpulse, 1e-4)
        assertTrue("Autonomic transition at 1m should be recovering from -8.0", postImpulseRamp > -8.0 && postImpulseRamp < 0.0)
        assertTrue("Post-workout boost at 15m should be strong positive (> 8.0)", postImpulsePeak > 8.0)
    }
}
