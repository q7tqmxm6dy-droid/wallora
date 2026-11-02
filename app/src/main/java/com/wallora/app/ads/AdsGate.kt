package com.wallora.app.ads

/**
 * Holds ad requests back until the consent flow has finished and the SDK is initialised.
 *
 * The banner is composed before [AdsConsent] completes, and requesting an ad first would break
 * Google's rule that consent is resolved before any request. Work registered through [whenReady]
 * runs immediately once the gate is open, or is queued and run when [open] is called.
 */
object AdsGate {

    private val pending = mutableListOf<() -> Unit>()
    private var opened = false

    @Synchronized
    fun whenReady(block: () -> Unit) {
        if (opened) block() else pending += block
    }

    @Synchronized
    fun open() {
        if (opened) return
        opened = true
        pending.toList().forEach { it() }
        pending.clear()
    }

    @get:Synchronized
    val isOpen: Boolean get() = opened
}
