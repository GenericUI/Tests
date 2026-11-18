package com.nexustools.genericui.tests.gtk

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrapper that installs [GtkJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for Linux GTK4 test runs.
 */
object GtkJestElectricBootstrap {

    /**
     * Installs and returns a fresh [GtkJestElectricRunnerNativeImpl].
     */
    fun install(): GtkJestElectricRunnerNativeImpl {
        val impl = GtkJestElectricRunnerNativeImpl()
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }

    /**
     * Installs [GtkJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active.
     */
    fun installIfEnabled(): GtkJestElectricRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is GtkJestElectricRunnerNativeImpl) {
            return current
        }
        return install()
    }
}
