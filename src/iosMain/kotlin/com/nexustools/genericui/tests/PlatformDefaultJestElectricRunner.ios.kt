package com.nexustools.genericui.tests

import com.nexustools.genericui.tests.ios.IosJestElectricRunnerNativeImpl

internal actual fun createPlatformDefaultJestElectricRunner(): JestElectricRunnerNativeImpl =
    IosJestElectricRunnerNativeImpl().also { it.ensureBackendInitialized() }
