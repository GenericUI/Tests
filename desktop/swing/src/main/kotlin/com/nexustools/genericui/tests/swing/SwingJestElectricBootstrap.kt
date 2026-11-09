package com.nexustools.genericui.tests.swing

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrapper that installs [SwingJUnitRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for `:desktop:swing` test runs.
 */
object SwingJestElectricBootstrap {

    /**
     * Installs and returns a fresh [SwingJUnitRunnerNativeImpl].
     */
    fun install(headless: Boolean = true): SwingJUnitRunnerNativeImpl {
        val impl = SwingJUnitRunnerNativeImpl(headless = headless)
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }

    /**
     * Installs [SwingJUnitRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active.
     */
    fun installIfEnabled(): SwingJUnitRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is SwingJUnitRunnerNativeImpl) {
            return current
        }
        return install()
    }
}
