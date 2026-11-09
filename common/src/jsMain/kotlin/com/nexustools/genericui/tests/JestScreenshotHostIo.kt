package com.nexustools.genericui.tests

actual object JestScreenshotHostIo {
    actual fun currentWorkingDirectory(): String = "."
    actual fun pathExists(path: String): Boolean = false
    actual fun mkdirs(path: String): Boolean = false
    actual fun writeBytes(path: String, bytes: ByteArray): Boolean = false
}
