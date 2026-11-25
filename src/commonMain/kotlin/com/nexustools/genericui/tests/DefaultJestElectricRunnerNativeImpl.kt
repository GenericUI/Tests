package com.nexustools.genericui.tests

import com.nexustools.genericui.animation.Animator
import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.core.UiTimerScheduler
import com.nexustools.genericui.enums.bootstrap.BootstrapPointerEvents
import com.nexustools.genericui.enums.css.CssPointerEvents
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.codec.LibJpegCodec
import com.nexustools.genericui.image.codec.LibPngCodec
import com.nexustools.genericui.io.FileSystem
import com.nexustools.genericui.threading.GenericUiThreading
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

        fun resolveScreenshotOutputPath(
            outputPath: String,
            defaultExtension: String = ".jpg"
        ): String {
            val trimmed = outputPath.trim().replace('\\', '/')
            val lower = trimmed.lowercase()
            val hasKnownExt = lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".png") ||
                lower.endsWith(".webp") ||
                lower.endsWith(".bmp")
            val normalizedExt = if (defaultExtension.startsWith(".")) defaultExtension else ".$defaultExtension"
            val fileNameWithExt = if (hasKnownExt) trimmed else "$trimmed$normalizedExt"

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

    internal fun prepareAppForScreenshot(
        app: Application?,
        pumpMs: Long = 16L
    ): Application? {
        ensureBackendInitialized()
        val targetApp = app ?: activeApp ?: Application.current ?: return null
        val factory = GenericUI.activeFactory
        if (factory != null && (targetApp.appNativeImpl == null || targetApp.mainWindow.nativeImpl == null)) {
            targetApp.run(factory)
        }
        targetApp.mainWindow.fragmentContainer?.ensureActiveFragmentStartedAndResumed()
        (targetApp as? FragmentApplication)?.rootFragmentContainer?.ensureActiveFragmentStartedAndResumed()
        GenericUiThreading.flushAllQueues()
        if (pumpMs > 0L) {
            pumpUiEvents(pumpMs)
        }
        return targetApp
    }

    override val backendName: String
        get() = GenericUI.activeFactory?.backendName ?: "common-headless"

    override val backendTarget: BackendTarget?
        get() = GenericUI.activeFactory?.backendTarget

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
        val advanceMs = durationMs.coerceAtLeast(0L)
        if (advanceMs > 0L) {
            UiTimerScheduler.advanceTimeBy(advanceMs.toInt())
        }
        val animatorTicks = if (advanceMs > 0L) {
            Animator.advanceTimeBy(advanceMs.toDouble())
        } else {
            0
        }
        if (animatorTicks == 0) {
            val delta = durationMs.coerceAtLeast(1L).toFloat()
            activeApp?.mainWindow?.stepTransitions(delta)
        }
        if (activeApp is FragmentApplication) {
            (activeApp as? FragmentApplication)?.currentFragment?.flushTimeouts(durationMs.toInt())
        }
    }

    protected fun isPointerEventsNoneForComponent(component: AbstractComponent): Boolean =
        component.style.getPointerEvents() == CssPointerEvents.NONE ||
            component.computedStyle?.pointerEventsEnabled == false ||
            component.getBootstrap().getPointerEvents() == BootstrapPointerEvents.NONE ||
            component.getAttribute("pointer-events")?.equals("none", ignoreCase = true) == true ||
            component.getAttribute("layerPassThrough")?.equals("true", ignoreCase = true) == true ||
            component.hasClass("gui-layer-pass-through")

    override fun performNativeClick(component: AbstractComponent): Boolean {
        if (!component.isEnabled || !component.isVisible || isPointerEventsNoneForComponent(component)) return false
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

    fun performNativeClickAt(
        app: Application? = null,
        x: Int,
        y: Int,
        width: Int = 1280,
        height: Int = 800
    ): AbstractComponent? {
        val targetApp = app ?: activeApp ?: Application.current ?: return null
        val scene = JestElectricComponentSnapshotRenderer.renderApplicationScene(targetApp, width, height)
        val hit = JestElectricComponentSnapshotRenderer.hitTestAt(scene.nodes, x, y) ?: return null
        if (!hit.isEnabled || !hit.isVisible) return null
        performNativeClick(hit)
        return hit
    }

    override fun performNativeHover(component: AbstractComponent): Boolean {
        if (!component.isVisible || isPointerEventsNoneForComponent(component)) return false
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

    override fun captureScreenshotPng(
        app: Application?,
        width: Int,
        height: Int
    ): ByteArray? {
        val buffer = captureScreenshot(app, width, height) ?: return null
        return LibPngCodec.encode(buffer)
    }

    override fun saveScreenshotPng(
        outputPath: String,
        app: Application?,
        width: Int,
        height: Int
    ): String? {
        val pngBytes = captureScreenshotPng(app, width, height) ?: return null
        return writeScreenshotBytes(outputPath, pngBytes, ".png")
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
        return writeScreenshotBytes(outputPath, jpegBytes, ".jpg")
    }

    protected fun writeScreenshotBytes(
        outputPath: String,
        bytes: ByteArray,
        defaultExtension: String
    ): String? {
        if (bytes.isEmpty()) return null
        val resolvedPath = resolveScreenshotOutputPath(outputPath, defaultExtension)
        val parentDir = resolvedPath.substringBeforeLast('/', "")
        if (parentDir.isNotEmpty()) {
            JestScreenshotHostIo.mkdirs(parentDir)
            hostCreateDirectoriesHook?.invoke(parentDir)
            runSync { FileSystem.mkdir(parentDir, recursive = true) }
        }
        val wroteViaHostIo = JestScreenshotHostIo.writeBytes(resolvedPath, bytes)
        val wroteViaHost = if (!wroteViaHostIo) hostWriteBytesHook?.invoke(resolvedPath, bytes) == true else true
        val wroteViaFs = if (!wroteViaHost) runSync { FileSystem.writeFile(resolvedPath, bytes) } == true else true
        return if (wroteViaHostIo || wroteViaHost || wroteViaFs) resolvedPath else null
    }
}
