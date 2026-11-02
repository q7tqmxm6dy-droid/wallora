package com.wallora.app.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsGateTest {

    @Test
    fun requestsWaitForTheGateAndThenRunOnce() {
        val ran = mutableListOf<String>()

        assertFalse(AdsGate.isOpen)
        AdsGate.whenReady { ran += "queued" }
        assertTrue("nothing may run before consent finishes", ran.isEmpty())

        AdsGate.open()
        AdsGate.open() // a second open must not replay the queue
        assertEquals(listOf("queued"), ran)

        AdsGate.whenReady { ran += "late" }
        assertEquals(listOf("queued", "late"), ran)
    }
}
