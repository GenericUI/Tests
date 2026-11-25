package com.nexustools.genericui.tests

import com.nexustools.genericui.tests.swing.SwingJUnitRunnerNativeImpl

internal actual fun createPlatformDefaultJestElectricRunner(): JestElectricRunnerNativeImpl =
    SwingJUnitRunnerNativeImpl(headless = true).also { it.ensureBackendInitialized() }
