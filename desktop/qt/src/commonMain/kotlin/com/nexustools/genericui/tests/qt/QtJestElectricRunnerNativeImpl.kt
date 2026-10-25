package com.nexustools.genericui.tests.qt

import com.nexustools.genericui.qt.QtNativeFactory
import com.nexustools.genericui.tests.desktop.DesktopSharedRunnerNativeImpl

/**
 * Linux Qt6 runner implementation (`:tests:desktop:qt`) of `JestElectricRunnerNativeImpl`.
 */
class QtJestElectricRunnerNativeImpl : DesktopSharedRunnerNativeImpl({ QtNativeFactory() }) {
    override val backendName: String
        get() = "linux-qt6"
}
