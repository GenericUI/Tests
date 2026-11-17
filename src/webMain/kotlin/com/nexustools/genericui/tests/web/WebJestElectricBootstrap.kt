package com.nexustools.genericui.tests.web

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrap helper for Web tests that registers [WebJestElectricRunnerNativeImpl]
 * into [JestElectricNativeRegistry].
 */
object WebJestElectricBootstrap {

    fun install(): WebJestElectricRunnerNativeImpl {
        val impl = WebJestElectricRunnerNativeImpl()
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }

    fun installIfEnabled() {
        val current = JestElectricNativeRegistry.current()
        if (current !is WebJestElectricRunnerNativeImpl) {
            install()
        }
    }
}
