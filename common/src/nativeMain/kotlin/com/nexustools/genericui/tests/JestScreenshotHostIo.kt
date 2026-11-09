@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nexustools.genericui.tests

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.getcwd
import platform.posix.system

actual object JestScreenshotHostIo {
    actual fun currentWorkingDirectory(): String = memScoped {
        val buf = allocArray<ByteVar>(4096)
        getcwd(buf, 4096u)?.toKString() ?: "."
    }

    actual fun pathExists(path: String): Boolean =
        access(path, F_OK) == 0

    actual fun mkdirs(path: String): Boolean {
        if (pathExists(path)) return true
        val norm = path.replace('\\', '/')
        val winCmd = "cmd /c if not exist \"${norm.replace('/', '\\')}\" mkdir \"${norm.replace('/', '\\')}\" >nul 2>nul"
        if (system(winCmd) == 0 && pathExists(path)) return true
        val posixCmd = "mkdir -p \"$norm\" >/dev/null 2>&1"
        system(posixCmd)
        return pathExists(path)
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
