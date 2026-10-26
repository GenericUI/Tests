package com.nexustools.genericui.tests.mobile

import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.core.NativeFactory
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl

/**
 * Shared mobile base implementation (`:tests:mobile:shared`) of `JestElectricRunnerNativeImpl`
 * for `:android` and `:ios`.
 */
open class MobileSharedRunnerNativeImpl(
    private val factoryProvider: () -> NativeFactory
) : DefaultJestElectricRunnerNativeImpl() {

    override fun ensureBackendInitialized() {
        if (GenericUI.activeFactory == null) {
            GenericUI.initialize(factoryProvider())
        }
    }
}
