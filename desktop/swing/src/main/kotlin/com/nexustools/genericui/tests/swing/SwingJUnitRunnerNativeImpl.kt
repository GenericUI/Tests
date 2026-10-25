package com.nexustools.genericui.tests.swing

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.swing.SwingNativeFactory
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl

/**
 * Java Swing + JUnit 4 runner implementation (`:tests:desktop:swing`) of `JestElectricRunnerNativeImpl`.
 */
class SwingJUnitRunnerNativeImpl(
    val headless: Boolean = true
) : DefaultJestElectricRunnerNativeImpl() {

    override val backendName: String
        get() = "swing-junit"

    override fun ensureBackendInitialized() {
        if (headless) {
            System.setProperty("java.awt.headless", "true")
        }
        if (GenericUI.activeFactory !is SwingNativeFactory) {
            GenericUI.initialize(SwingNativeFactory())
        }
    }

    override fun launchVisualWindowForApp(app: Application, suiteName: String) {
        ensureBackendInitialized()
        activeApp = app
        try {
            app.run(GenericUI.requireFactory())
        } catch (_: NullPointerException) {
            // Headless fallback if a Swing native widget constructor encounters an uninitialized AWT peer
        }
        pumpUiEvents(16L)
    }

    override fun closeVisualWindow() {
        try {
            super.closeVisualWindow()
        } catch (_: NullPointerException) {
            activeApp = null
        }
    }
}
