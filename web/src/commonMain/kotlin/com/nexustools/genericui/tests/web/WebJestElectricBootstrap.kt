package com.nexustools.genericui.tests.web

import com.nexustools.genericui.tests.JestElectricNativeRegistry

/**
 * Bootstrap helper for `:tests:web` that registers [WebJestElectricRunnerNativeImpl]
 * into [JestElectricNativeRegistry].
 */
object WebJestElectricBootstrap {
    fun installIfEnabled() {
        JestElectricNativeRegistry.register(WebJestElectricRunnerNativeImpl())
    }
}
