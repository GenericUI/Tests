package com.nexustools.genericui.tests

import com.nexustools.genericui.tests.gtk.GtkJestElectricRunnerNativeImpl

internal actual fun createNativeDesktopDefaultJestElectricRunner(): JestElectricRunnerNativeImpl =
    GtkJestElectricRunnerNativeImpl().also {
        it.ensureBackendInitialized()
    }
