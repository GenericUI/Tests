package com.nexustools.genericui.tests

/**
 * Eagerly initializes and resolves the platform-native [JestElectricRunnerNativeImpl] for the active target
 * (`SwingJUnitRunnerNativeImpl` on JVM/Swing, `MacosJestElectricRunnerNativeImpl` on macOS,
 * `WindowsVisualAppRunnerNativeImpl` on Windows, `GtkJestElectricRunnerNativeImpl` on Linux,
 * `AndroidRobolectricRunnerNativeImpl` on Android, `IosJestElectricRunnerNativeImpl` on iOS,
 * and `WebJestElectricRunnerNativeImpl` on Web).
 */
internal expect fun createPlatformDefaultJestElectricRunner(): JestElectricRunnerNativeImpl
