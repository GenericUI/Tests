package com.nexustools.genericui.tests.qt

import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.gtk.GtkJestElectricRunnerNativeImpl

/**
 * Bootstrapper that installs [QtJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for Linux Qt6 test runs.
 */
object QtJestElectricBootstrap {

    /**
     * Installs and returns a fresh [QtJestElectricRunnerNativeImpl].
     */
    fun install(): QtJestElectricRunnerNativeImpl {
        val impl = QtJestElectricRunnerNativeImpl()
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        if (JestElectricNativeRegistry.forTarget(BackendTarget.Gtk) == null) {
            JestElectricNativeRegistry.registerTarget(BackendTarget.Gtk, GtkJestElectricRunnerNativeImpl())
        }
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
