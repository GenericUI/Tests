package com.nexustools.genericui.tests

import com.nexustools.genericui.tests.android.AndroidRobolectricRunnerNativeImpl

internal actual fun createPlatformDefaultJestElectricRunner(): JestElectricRunnerNativeImpl =
    AndroidRobolectricRunnerNativeImpl().also { it.ensureBackendInitialized() }
