package com.nexustools.genericui.tests

import java.io.File

actual object JestScreenshotHostIo {
    actual fun currentWorkingDirectory(): String =
        runCatching { File(System.getProperty("user.dir") ?: ".").canonicalPath }.getOrElse {
            System.getProperty("user.dir") ?: "."
        }

    actual fun pathExists(path: String): Boolean =
        runCatching { File(path).exists() }.getOrDefault(false)

    actual fun mkdirs(path: String): Boolean =
        runCatching {
            val dir = File(path)
            if (dir.exists()) true else dir.mkdirs()
        }.getOrDefault(false)

    actual fun writeBytes(path: String, bytes: ByteArray): Boolean =
        runCatching {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            file.isFile && file.length() > 0L
        }.getOrDefault(false)
}
