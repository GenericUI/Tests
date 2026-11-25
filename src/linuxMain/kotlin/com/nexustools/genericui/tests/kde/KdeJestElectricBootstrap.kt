package com.nexustools.genericui.tests.kde

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrapper that installs [KdeJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for Linux KDE Plasma test runs.
 */
object KdeJestElectricBootstrap {

    /**
     * Installs and returns a fresh [KdeJestElectricRunnerNativeImpl].
     *
     * @param visualWindowEnabled When `true`, presents the live KDE window during test execution.
     * @param stepDelayMs Duration in milliseconds to pump events after each visual interaction.
     */
    fun install(
        visualWindowEnabled: Boolean = false,
        stepDelayMs: Long = 8L
    ): KdeJestElectricRunnerNativeImpl {
        val impl = KdeJestElectricRunnerNativeImpl(
            visualWindowEnabled = visualWindowEnabled,
            stepDelayMs = stepDelayMs
        )
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }

    /**
     * Installs [KdeJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active.
     */
    fun installIfEnabled(): KdeJestElectricRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is KdeJestElectricRunnerNativeImpl) {
            return current
        }
        return install()
    }
}
