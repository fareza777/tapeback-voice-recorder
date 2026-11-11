package com.f7developer.tapeback.domain

import com.f7developer.tapeback.domain.policy.ReelPhysics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class ReelPhysicsTest {

    @Test
    fun `at the start of a side the supply reel is full and the take-up reel is bare hub`() {
        val packs = ReelPhysics.packs(0f)
        assertEquals(1f, packs.supply, 1e-4f)
        assertEquals(ReelPhysics.HUB_RADIUS, packs.takeUp, 1e-4f)
    }

    @Test
    fun `tape area is conserved at every progress value`() {
        val h2 = ReelPhysics.HUB_RADIUS * ReelPhysics.HUB_RADIUS
        val expected = (1f - h2) + 2 * h2 - h2 // total pack area of one full reel plus one hub
        for (i in 0..20) {
            val p = ReelPhysics.packs(i / 20f)
            val area = p.supply * p.supply + p.takeUp * p.takeUp - h2
            assertEquals(expected, area, 1e-4f)
        }
    }

    @Test
    fun `empty reel spins faster than full reel because omega equals v over r`() {
        val packs = ReelPhysics.packs(0.1f)
        val v = ReelPhysics.PLAY_SPEED
        val wSupply = ReelPhysics.angularVelocity(v, packs.supply)
        val wTakeUp = ReelPhysics.angularVelocity(v, packs.takeUp)
        assertTrue(wTakeUp > wSupply)
        assertEquals(v / packs.takeUp, wTakeUp, 1e-5f)
    }

    @Test
    fun `hub speed matches a real cassette at roughly 0_7 revolutions per second`() {
        val revPerSecond = ReelPhysics.angularVelocity(ReelPhysics.PLAY_SPEED, ReelPhysics.HUB_RADIUS) / (2 * PI.toFloat())
        assertTrue(revPerSecond in 0.5f..0.9f)
    }

    @Test
    fun `tape counter is monotonic and non-linear like a real deck`() {
        var last = -1
        for (i in 0..100) {
            val c = ReelPhysics.counter(i / 100f)
            assertTrue(c >= last)
            last = c
        }
        val firstHalf = ReelPhysics.counter(0.5f) - ReelPhysics.counter(0f)
        val secondHalf = ReelPhysics.counter(1f) - ReelPhysics.counter(0.5f)
        assertTrue("first half of the tape counts more turns", firstHalf > secondHalf)
        assertTrue(ReelPhysics.counter(1f) in 550..650)
    }

    @Test
    fun `motor inertia approaches target and settles exactly`() {
        var s = 0f
        repeat(10) { s = ReelPhysics.motorStep(s, 1f, 0.016f) }
        assertTrue(s in 0.4f..0.95f)
        repeat(200) { s = ReelPhysics.motorStep(s, 1f, 0.016f) }
        assertEquals(1f, s, 0f)
        assertEquals(0.3f, ReelPhysics.motorStep(0.3f, 1f, 0f), 0f)
    }
}
