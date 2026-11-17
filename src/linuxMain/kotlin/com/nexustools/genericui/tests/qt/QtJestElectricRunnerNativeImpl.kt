package com.nexustools.genericui.tests.qt

import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.qt.QtNativeFactory
import com.nexustools.genericui.tests.desktop.DesktopSharedRunnerNativeImpl

/**
 * Linux Qt6 runner implementation of `JestElectricRunnerNativeImpl`.
 */
class QtJestElectricRunnerNativeImpl : DesktopSharedRunnerNativeImpl(
    factoryProvider = { QtNativeFactory() },
    expectedTarget = BackendTarget.Qt
) {
    override val backendTarget: BackendTarget
        get() = BackendTarget.Qt

    override val backendName: String
        get() = "linux-qt6"
}
