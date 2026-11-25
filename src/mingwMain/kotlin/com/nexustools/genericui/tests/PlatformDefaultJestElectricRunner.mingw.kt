package com.nexustools.genericui.tests

import com.nexustools.genericui.tests.windows.WindowsVisualAppRunnerNativeImpl

internal actual fun createNativeDesktopDefaultJestElectricRunner(): JestElectricRunnerNativeImpl =
    WindowsVisualAppRunnerNativeImpl(visualWindowEnabled = false).also {
        it.ensureBackendInitialized()
    }
