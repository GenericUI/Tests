package com.nexustools.genericui.tests

import com.nexustools.genericui.enums.system.BackendTarget

/**
 * Global registry for the active [JestElectricRunnerNativeImpl] and per-[BackendTarget]
 * runner implementations (`Windows`, `MacOS`, `Gtk`, `Qt`, `Swing`, `Android`, `IOS`, `IPadOS`, `Web`).
 *
 * Platform test bootstraps in `:tests` register their platform-specific [JestElectricRunnerNativeImpl]
 * instances here before executing suites.
 */
object JestElectricNativeRegistry {

    private var activeImpl: JestElectricRunnerNativeImpl = DefaultJestElectricRunnerNativeImpl()
    private val runnersByTarget = LinkedHashMap<BackendTarget, JestElectricRunnerNativeImpl>()

    /**
     * Registers [impl] as the active platform runner bridge and indexes it under its
     * [JestElectricRunnerNativeImpl.backendTarget] if non-null.
     */
    fun register(impl: JestElectricRunnerNativeImpl) {
        activeImpl = impl
        impl.backendTarget?.let { target ->
            runnersByTarget[target] = impl
        }
    }

    /**
     * Registers [impl] specifically for [target] without changing [current].
     */
    fun registerTarget(target: BackendTarget, impl: JestElectricRunnerNativeImpl) {
        runnersByTarget[target] = impl
    }

    /**
     * Returns the registered [JestElectricRunnerNativeImpl] for [target], or falls back to
     * [current] when its [JestElectricRunnerNativeImpl.backendTarget] matches [target].
     */
    fun forTarget(target: BackendTarget): JestElectricRunnerNativeImpl? =
        runnersByTarget[target] ?: activeImpl.takeIf { it.backendTarget == target }

    /**
     * Returns all [BackendTarget] values currently registered in this registry.
     */
    fun registeredTargets(): Set<BackendTarget> = runnersByTarget.keys.toSet()

    /**
     * Returns the currently registered [JestElectricRunnerNativeImpl].
     */
    fun current(): JestElectricRunnerNativeImpl = activeImpl

    /**
     * Resets the registry back to [DefaultJestElectricRunnerNativeImpl] and clears target overrides.
     */
    fun resetToDefault() {
        activeImpl = DefaultJestElectricRunnerNativeImpl()
        runnersByTarget.clear()
    }
}
