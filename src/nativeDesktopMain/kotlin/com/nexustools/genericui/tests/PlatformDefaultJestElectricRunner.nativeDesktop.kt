package com.nexustools.genericui.tests

internal expect fun createNativeDesktopDefaultJestElectricRunner(): JestElectricRunnerNativeImpl

internal actual fun createPlatformDefaultJestElectricRunner(): JestElectricRunnerNativeImpl =
    createNativeDesktopDefaultJestElectricRunner()
