package com.nexustools.genericui.tests.desktop

import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.core.NativeFactory
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl

/**
 * Shared desktop base implementation of `JestElectricRunnerNativeImpl` for native desktop
 * targets (`Windows`, `MacOS`, `Gtk`, `Kde`).
 */
open class DesktopSharedRunnerNativeImpl(
    private val factoryProvider: () -> NativeFactory,
    private val expectedTarget: BackendTarget? = null
) : DefaultJestElectricRunnerNativeImpl() {

    override val backendTarget: BackendTarget?
        get() = expectedTarget ?: super.backendTarget

    override fun ensureBackendInitialized() {
        val current = GenericUI.activeFactory
        if (current == null || (expectedTarget != null && current.backendTarget != expectedTarget)) {
            GenericUI.initialize(factoryProvider())
        }
    }
}
