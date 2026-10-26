package com.nexustools.genericui.tests.desktop

import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.core.NativeFactory
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl

/**
 * Shared desktop base implementation of `JestElectricRunnerNativeImpl` for native desktop
 * targets (`:windows`, `:macos`, `:gtk`, `:qt`).
 */
open class DesktopSharedRunnerNativeImpl(
    private val factoryProvider: () -> NativeFactory
) : DefaultJestElectricRunnerNativeImpl() {

    override fun ensureBackendInitialized() {
        if (GenericUI.activeFactory == null) {
            GenericUI.initialize(factoryProvider())
        }
    }
}
