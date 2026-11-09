package com.nexustools.genericui.tests.qt

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrapper that installs [QtJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for `:desktop:qt` test runs.
 */
object QtJestElectricBootstrap {

    /**
     * Installs and returns a fresh [QtJestElectricRunnerNativeImpl].
     */
    fun install(): QtJestElectricRunnerNativeImpl {
        val impl = QtJestElectricRunnerNativeImpl()
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }

    /**
     * Installs [QtJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active.
     */
    fun installIfEnabled(): QtJestElectricRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is QtJestElectricRunnerNativeImpl) {
            return current
        }
        return install()
    }
}
