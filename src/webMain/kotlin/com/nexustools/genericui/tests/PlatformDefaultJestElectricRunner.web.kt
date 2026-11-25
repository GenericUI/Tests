package com.nexustools.genericui.tests

import com.nexustools.genericui.tests.web.WebJestElectricRunnerNativeImpl

internal actual fun createPlatformDefaultJestElectricRunner(): JestElectricRunnerNativeImpl =
    WebJestElectricRunnerNativeImpl().also { it.ensureBackendInitialized() }
