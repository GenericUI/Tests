package com.nexustools.genericui.tests

/**
 * Global registry for the active [JestElectricRunnerNativeImpl].
 *
 * Platform test runners (`:tests:desktop:windows`, `:tests:desktop:swing`, `:tests:mobile:android`, etc.)
 * register their platform-specific [JestElectricRunnerNativeImpl] here before executing suites.
 */
object JestElectricNativeRegistry {

    private var activeImpl: JestElectricRunnerNativeImpl = DefaultJestElectricRunnerNativeImpl()

    /**
     * Registers [impl] as the active platform runner bridge.
     */
    fun register(impl: JestElectricRunnerNativeImpl) {
        activeImpl = impl
    }

    /**
     * Returns the currently registered [JestElectricRunnerNativeImpl].
     */
    fun current(): JestElectricRunnerNativeImpl = activeImpl

    /**
     * Resets the registry back to [DefaultJestElectricRunnerNativeImpl].
     */
    fun resetToDefault() {
        activeImpl = DefaultJestElectricRunnerNativeImpl()
    }
}
