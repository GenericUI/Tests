package com.nexustools.genericui.tests

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.codec.LibJpegCodec
import com.nexustools.genericui.io.FileSystem
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Cross-platform default implementation of [JestElectricRunnerNativeImpl].
 *
 * Drives component interactions, CSS transitions, and `Fragment` / `Application` lifecycles
 * in-memory and delegates to `GenericUI.activeFactory` when a platform backend is initialized.
 */
open class DefaultJestElectricRunnerNativeImpl : JestElectricRunnerNativeImpl {

    companion object {
        var hostWorkingDirectoryProvider: (() -> String?)? = null
        var hostFileOrDirExistsChecker: ((String) -> Boolean)? = null
        var hostCreateDirectoriesHook: ((String) -> Boolean)? = null
        var hostWriteBytesHook: ((String, ByteArray) -> Boolean)? = null

        private fun <T> runSync(block: suspend () -> T): T? {
            var out: T? = null
            block.startCoroutine(object : Continuation<T> {
                override val context: CoroutineContext = EmptyCoroutineContext
                override fun resumeWith(result: Result<T>) {
                    out = result.getOrNull()
                }
            })
            return out
        }

        private fun checkPathExists(path: String): Boolean {
            if (JestScreenshotHostIo.pathExists(path)) return true
            hostFileOrDirExistsChecker?.invoke(path)?.let { if (it) return true }
            return runSync { FileSystem.exists(path) } == true
        }

        private fun resolveCurrentWorkingDirectory(): String {
            hostWorkingDirectoryProvider?.invoke()?.takeIf { it.isNotBlank() }?.let { return it }
            val hostCwd = JestScreenshotHostIo.currentWorkingDirectory()
            if (hostCwd.isNotBlank() && hostCwd != ".") {
                return hostCwd
            }
            val statDot = runSync { FileSystem.stat(".") }
            val p = statDot?.path
            if (!p.isNullOrBlank() && p != ".") {
                return p
            }
            return "."
        }

        fun resolveScreenshotOutputPath(outputPath: String): String {
            val trimmed = outputPath.trim().replace('\\', '/')
            val fileNameWithExt = if (trimmed.endsWith(".jpg", ignoreCase = true) ||
                trimmed.endsWith(".jpeg", ignoreCase = true)
            ) {
                trimmed
            } else {
                "$trimmed.jpg"
            }

            // Absolute path on Windows (e.g. D:/...) or POSIX (/...)
            if ((fileNameWithExt.length >= 3 && fileNameWithExt[1] == ':' && fileNameWithExt[2] == '/') ||
                fileNameWithExt.startsWith("/")
            ) {
                return fileNameWithExt
            }

            val cwd = resolveCurrentWorkingDirectory().replace('\\', '/').trimEnd('/')
            val repoRoot = findRepositoryRoot(cwd)
            val cleanRel = fileNameWithExt.removePrefix("./")
            return if (cleanRel.startsWith("examples/screenshots/")) {
                if (repoRoot != null) "$repoRoot/$cleanRel" else cleanRel
            } else {
                val baseName = cleanRel.substringAfterLast('/')
                if (repoRoot != null) {
                    "$repoRoot/examples/screenshots/$baseName"
                } else {
                    "examples/screenshots/$baseName"
                }
            }
        }

        private fun findRepositoryRoot(startDir: String): String? {
            if (startDir.isNotBlank() && startDir != ".") {
                val examplesIdx = startDir.indexOf("/examples/")
                if (examplesIdx > 0) {
                    return startDir.substring(0, examplesIdx)
                }
                var cur = startDir
                for (depth in 0..8) {
                    if (checkPathExists("$cur/examples") && checkPathExists("$cur/common")) {
                        return cur
                    }
                    val slash = cur.lastIndexOf('/')
                    if (slash <= 0) break
                    cur = cur.substring(0, slash)
                }
            }
            val relPrefixes = listOf(".", "..", "../..", "../../..", "../../../..", "../../../../..")
            for (prefix in relPrefixes) {
                if (checkPathExists("$prefix/examples") && checkPathExists("$prefix/common")) {
                    return prefix
                }
            }
            return if (startDir.isNotBlank() && startDir != ".") startDir else null
        }
    }

    protected var activeApp: Application? = null
    protected var currentBannerText: String = ""

    override val backendName: String
        get() = GenericUI.activeFactory?.backendName ?: "common-headless"

    override val isVisualWindowActive: Boolean
        get() = activeApp?.mainWindow?.isWindowShown == true

    override fun ensureBackendInitialized() {
        // Default headless runner operates with whatever NativeFactory is registered (or pure Kotlin model).
    }

    override fun launchVisualWindowForApp(app: Application, suiteName: String) {
        ensureBackendInitialized()
        activeApp = app
        val factory = GenericUI.activeFactory
        if (factory != null) {
            app.run(factory)
        } else if (app is FragmentApplication) {
            app.rootFragmentContainer.ensureActiveFragmentStartedAndResumed()
        }
        pumpUiEvents(16L)
    }

    override fun updateVisualTestBanner(testName: String, status: String) {
        currentBannerText = "[$status] $testName"
    }

    override fun pumpUiEvents(durationMs: Long) {
        val delta = durationMs.coerceAtLeast(1L).toFloat()
        activeApp?.mainWindow?.stepTransitions(delta)
        if (activeApp is FragmentApplication) {
            (activeApp as? FragmentApplication)?.currentFragment?.flushTimeouts(durationMs.toInt())
        }
    }

    override fun performNativeClick(component: AbstractComponent): Boolean {
        if (!component.isEnabled || !component.isVisible) return false
        when (component) {
            is CheckBox -> {
                component.isChecked = !component.isChecked
                component.onClick?.invoke()
            }
            is Button -> {
                component.onClick?.invoke()
            }
            else -> {
                component.onClick?.invoke()
            }
        }
        pumpUiEvents(16L)
        return true
    }

    override fun performNativeHover(component: AbstractComponent): Boolean {
        if (!component.isVisible) return false
        component.isHovered = true
        pumpUiEvents(16L)
        return true
    }

    override fun performNativeFocus(component: AbstractComponent): Boolean {
        if (!component.isEnabled || !component.isVisible) return false
        component.isFocused = true
        pumpUiEvents(16L)
        return true
    }

    override fun performNativeTypeText(component: AbstractComponent, text: String): Boolean {
        if (!component.isEnabled || !component.isVisible) return false
        if (component is TextField) {
            if (component.isReadOnly) return false
            component.isFocused = true
            component.value = text
            component.onValueChanged?.invoke(text)
            pumpUiEvents(16L)
            return true
        }
        return false
    }

    override fun performNativeMenuClick(app: Application, menuTitle: String, itemLabel: String): Boolean {
        val item = RoboComponentTreeFinder.findMenuItem(app, menuTitle, itemLabel) ?: return false
        if (!item.isEnabled) return false
        item.onClick?.invoke()
        pumpUiEvents(16L)
        return true
    }

    override fun closeVisualWindow() {
        activeApp?.mainWindow?.close()
        activeApp = null
    }

    override fun captureScreenshot(
        app: Application?,
        width: Int,
        height: Int
    ): ImageBuffer? {
        val targetApp = app ?: activeApp ?: Application.current ?: return null
        pumpUiEvents(16L)
        return JestElectricComponentSnapshotRenderer.renderApplication(targetApp, width, height)
    }

    override fun saveScreenshotJpeg(
        outputPath: String,
        app: Application?,
        quality: Int,
        width: Int,
        height: Int
    ): String? {
        val buffer = captureScreenshot(app, width, height) ?: return null
        val jpegBytes = LibJpegCodec.encode(buffer, quality)
        val resolvedPath = resolveScreenshotOutputPath(outputPath)
        val parentDir = resolvedPath.substringBeforeLast('/', "")
        if (parentDir.isNotEmpty()) {
            JestScreenshotHostIo.mkdirs(parentDir)
            hostCreateDirectoriesHook?.invoke(parentDir)
            runSync { FileSystem.mkdir(parentDir, recursive = true) }
        }
        val wroteViaHostIo = JestScreenshotHostIo.writeBytes(resolvedPath, jpegBytes)
        val wroteViaHost = if (!wroteViaHostIo) hostWriteBytesHook?.invoke(resolvedPath, jpegBytes) == true else true
        val wroteViaFs = if (!wroteViaHost) runSync { FileSystem.writeFile(resolvedPath, jpegBytes) } == true else true
        return if (wroteViaHostIo || wroteViaHost || wroteViaFs) resolvedPath else resolvedPath
    }
}
