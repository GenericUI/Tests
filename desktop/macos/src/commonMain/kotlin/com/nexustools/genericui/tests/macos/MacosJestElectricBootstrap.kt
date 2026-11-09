package com.nexustools.genericui.tests.macos

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrapper that installs [MacosJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for `:desktop:macos` test runs.
 */
object MacosJestElectricBootstrap {

    /**
     * Installs and returns a fresh [MacosJestElectricRunnerNativeImpl].
     */
    fun install(): MacosJestElectricRunnerNativeImpl {
        val impl = MacosJestElectricRunnerNativeImpl()
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }

    /**
     * Installs [MacosJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active.
     */
    fun installIfEnabled(): MacosJestElectricRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is MacosJestElectricRunnerNativeImpl) {
            return current
        }
        return install()
    }
}
