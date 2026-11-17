@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nexustools.genericui.tests

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.getcwd

actual object JestScreenshotHostIo {
    actual fun currentWorkingDirectory(): String = memScoped {
        val buf = allocArray<ByteVar>(4096)
        getcwd(buf, 4096.convert())?.toKString()
            ?: NSFileManager.defaultManager.currentDirectoryPath
    }

    actual fun pathExists(path: String): Boolean =
        access(path, F_OK) == 0 || NSFileManager.defaultManager.fileExistsAtPath(path)

    actual fun mkdirs(path: String): Boolean {
        if (pathExists(path)) return true
        return NSFileManager.defaultManager.createDirectoryAtPath(
            path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    actual fun writeBytes(path: String, bytes: ByteArray): Boolean {
        val norm = path.replace('\\', '/')
        val parent = norm.substringBeforeLast('/', "")
        if (parent.isNotEmpty()) {
            mkdirs(parent)
        }
        val fp = fopen(path, "wb") ?: return false
        return try {
            if (bytes.isNotEmpty()) {
                bytes.usePinned { pinned ->
                    fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), fp)
                }
            }
            true
        } finally {
            fclose(fp)
        }
    }
}
