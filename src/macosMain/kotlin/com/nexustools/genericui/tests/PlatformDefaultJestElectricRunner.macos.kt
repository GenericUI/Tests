package com.nexustools.genericui.tests

import com.nexustools.genericui.tests.macos.MacosJestElectricRunnerNativeImpl

internal actual fun createNativeDesktopDefaultJestElectricRunner(): JestElectricRunnerNativeImpl =
    MacosJestElectricRunnerNativeImpl(visualWindowEnabled = false).also {
        it.ensureBackendInitialized()
    }
