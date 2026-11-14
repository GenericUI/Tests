package com.nexustools.genericui.tests.macos

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrapper that installs [MacosJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for `:desktop:macos` test runs.
 */
object MacosJestElectricBootstrap {

    /**
     * Installs and returns a [MacosJestElectricRunnerNativeImpl].
     *
     * @param visualWindowEnabled When `true`, opens the live AppKit `NSWindow` during test execution.
     * @param stepDelayMs Duration in milliseconds to pump AppKit events after each visual interaction.
     */
    fun install(
        visualWindowEnabled: Boolean = false,
        stepDelayMs: Long = 24L
    ): MacosJestElectricRunnerNativeImpl {
        val impl = MacosJestElectricRunnerNativeImpl(
            visualWindowEnabled = visualWindowEnabled,
            stepDelayMs = stepDelayMs
        )
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }

    /**
     * Installs [MacosJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active
     * (or upgrades its visual mode when requested).
     */
    fun installIfEnabled(
        visualWindowEnabled: Boolean = false,
        stepDelayMs: Long = 24L
    ): MacosJestElectricRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is MacosJestElectricRunnerNativeImpl) {
            if (current.visualWindowEnabled != visualWindowEnabled) {
                current.visualWindowEnabled = visualWindowEnabled
                current.stepDelayMs = stepDelayMs
                current.ensureBackendInitialized()
            }
            return current
        }
        return install(visualWindowEnabled = visualWindowEnabled, stepDelayMs = stepDelayMs)
    }
}
