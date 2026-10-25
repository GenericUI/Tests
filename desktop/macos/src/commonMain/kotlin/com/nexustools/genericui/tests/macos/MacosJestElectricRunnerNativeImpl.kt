package com.nexustools.genericui.tests.macos

import com.nexustools.genericui.macos.MacosNativeFactory
import com.nexustools.genericui.tests.desktop.DesktopSharedRunnerNativeImpl

/**
 * macOS AppKit runner implementation (`:tests:desktop:macos`) of `JestElectricRunnerNativeImpl`.
 */
class MacosJestElectricRunnerNativeImpl : DesktopSharedRunnerNativeImpl({ MacosNativeFactory() }) {
    override val backendName: String
        get() = "macos-appkit"
}
