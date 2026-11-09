package com.nexustools.genericui.tests.ios

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrapper that installs [IosJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for `:mobile:ios` test runs.
 */
object IosJestElectricBootstrap {

    /**
     * Installs and returns a fresh [IosJestElectricRunnerNativeImpl].
     */
    fun install(): IosJestElectricRunnerNativeImpl {
        val impl = IosJestElectricRunnerNativeImpl()
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }

    /**
     * Installs [IosJestElectricRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active.
     */
    fun installIfEnabled(): IosJestElectricRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is IosJestElectricRunnerNativeImpl) {
            return current
        }
        return install()
    }
}
