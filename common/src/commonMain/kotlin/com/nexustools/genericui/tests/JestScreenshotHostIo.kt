package com.nexustools.genericui.tests

/**
 * Synchronous host filesystem bridge used by `DefaultJestElectricRunnerNativeImpl` to resolve the
 * repository root and write `.jpg` test screenshots directly to disk across JVM, Android/Robolectric,
 * and Kotlin/Native test processes.
 */
expect object JestScreenshotHostIo {
    fun currentWorkingDirectory(): String
    fun pathExists(path: String): Boolean
    fun mkdirs(path: String): Boolean
    fun writeBytes(path: String, bytes: ByteArray): Boolean
}
