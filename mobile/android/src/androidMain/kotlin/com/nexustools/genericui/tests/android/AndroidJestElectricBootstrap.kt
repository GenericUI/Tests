package com.nexustools.genericui.tests.android

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrapper that installs [AndroidRobolectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for `:mobile:android` test runs.
 */
object AndroidJestElectricBootstrap {

    /**
     * Installs and returns a fresh [AndroidRobolectricRunnerNativeImpl].
     */
    fun install(): AndroidRobolectricRunnerNativeImpl {
        val impl = AndroidRobolectricRunnerNativeImpl()
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }

    /**
     * Installs [AndroidRobolectricRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active.
     */
    fun installIfEnabled(): AndroidRobolectricRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is AndroidRobolectricRunnerNativeImpl) {
            return current
        }
        return install()
    }
}
