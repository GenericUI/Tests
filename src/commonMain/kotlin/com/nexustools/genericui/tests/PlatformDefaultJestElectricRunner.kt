package com.nexustools.genericui.tests

/**
 * Resolves the platform-appropriate default [JestElectricRunnerNativeImpl] for the active target
 * (for example [com.nexustools.genericui.tests.android.AndroidRobolectricRunnerNativeImpl] powered
 * by `RoboElectricRunner` on `:android`, or [DefaultJestElectricRunnerNativeImpl] elsewhere).
 */
internal expect fun createPlatformDefaultJestElectricRunner(): JestElectricRunnerNativeImpl
