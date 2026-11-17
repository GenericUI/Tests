package com.nexustools.genericui.tests.ios

import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.ipados.IpadosJestElectricRunnerNativeImpl

/**
 * Bootstrapper that installs [IosJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for iOS (`BackendTarget.IOS`) test runs, while also ensuring [IpadosJestElectricRunnerNativeImpl]
 * is registered for `BackendTarget.IPadOS` multi-target execution.
 */
object IosJestElectricBootstrap {

    /**
     * Installs and returns a fresh [IosJestElectricRunnerNativeImpl].
     */
    fun install(): IosJestElectricRunnerNativeImpl {
        val impl = IosJestElectricRunnerNativeImpl()
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        if (JestElectricNativeRegistry.forTarget(BackendTarget.IPadOS) == null) {
            JestElectricNativeRegistry.registerTarget(
                BackendTarget.IPadOS,
                IpadosJestElectricRunnerNativeImpl()
            )
        }
        return impl
    }

    /**
     * Installs [IosJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active.
     */
    fun installIfEnabled(): IosJestElectricRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is IosJestElectricRunnerNativeImpl) {
            if (JestElectricNativeRegistry.forTarget(BackendTarget.IPadOS) == null) {
                JestElectricNativeRegistry.registerTarget(
                    BackendTarget.IPadOS,
                    IpadosJestElectricRunnerNativeImpl()
                )
            }
            return current
        }
        return install()
    }
}
