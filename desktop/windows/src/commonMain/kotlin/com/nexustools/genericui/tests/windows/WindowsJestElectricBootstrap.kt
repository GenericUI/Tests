package com.nexustools.genericui.tests.windows

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrapper that installs [WindowsVisualAppRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for `:desktop:windows` test runs.
 */
object WindowsJestElectricBootstrap {

    /**
     * Installs and returns a [WindowsVisualAppRunnerNativeImpl].
     *
     * @param visualWindowEnabled When `true` (default), opens the live Win32 window during test execution.
     * @param stepDelayMs Duration in milliseconds to pump Win32 events after each visual interaction.
     */
    fun install(
        visualWindowEnabled: Boolean = true,
        stepDelayMs: Long = 24L
    ): WindowsVisualAppRunnerNativeImpl {
        val impl = WindowsVisualAppRunnerNativeImpl(
            visualWindowEnabled = visualWindowEnabled,
            stepDelayMs = stepDelayMs
        )
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }
}
