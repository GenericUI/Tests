package com.nexustools.genericui.tests.ipados

import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.ios.IosJestElectricRunnerNativeImpl

/**
 * Bootstrapper that installs [IpadosJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for iPadOS (`BackendTarget.IPadOS`) test runs, while also ensuring [IosJestElectricRunnerNativeImpl]
 * is registered for `BackendTarget.IOS` multi-target execution.
 */
object IpadosJestElectricBootstrap {

    /**
     * Installs and returns a fresh [IpadosJestElectricRunnerNativeImpl].
     */
    fun install(): IpadosJestElectricRunnerNativeImpl {
        val impl = IpadosJestElectricRunnerNativeImpl()
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        if (JestElectricNativeRegistry.forTarget(BackendTarget.IOS) == null) {
            JestElectricNativeRegistry.registerTarget(
                BackendTarget.IOS,
                IosJestElectricRunnerNativeImpl()
            )
        }
        return impl
    }

    /**
     * Installs [IpadosJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active.
     */
    fun installIfEnabled(): IpadosJestElectricRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is IpadosJestElectricRunnerNativeImpl) {
            if (JestElectricNativeRegistry.forTarget(BackendTarget.IOS) == null) {
                JestElectricNativeRegistry.registerTarget(
                    BackendTarget.IOS,
                    IosJestElectricRunnerNativeImpl()
                )
            }
            return current
        }
        return install()
    }
}
