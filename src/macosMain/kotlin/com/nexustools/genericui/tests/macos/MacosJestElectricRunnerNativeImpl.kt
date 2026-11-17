@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.ExperimentalStdlibApi::class)

package com.nexustools.genericui.tests.macos

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.macos.MacosApplicationNativeImpl
import com.nexustools.genericui.macos.MacosNativeFactory
import com.nexustools.genericui.macos.MacosNativeWindowHost
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.tests.desktop.DesktopSharedRunnerNativeImpl
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.getcwd
import platform.posix.mkdir

@EagerInitialization
private val macosScreenshotHostHooksInit: Boolean = MacosJestElectricRunnerNativeImpl.installHostFileHooks()

/**
 * macOS AppKit runner implementation of `JestElectricRunnerNativeImpl`.
 *
 * Supports both fast headless testing (`visualWindowEnabled = false`, default) and live visual
 * AppKit `NSWindow` execution (`visualWindowEnabled = true`), pumping Cocoa run-loop events,
 * painting CoreGraphics view hierarchies on screen, and capturing live window screenshots.
 */
class MacosJestElectricRunnerNativeImpl(
    var visualWindowEnabled: Boolean = false,
    var stepDelayMs: Long = 24L
) : DesktopSharedRunnerNativeImpl({ MacosNativeFactory() }, expectedTarget = BackendTarget.MacOS) {

    companion object {
        fun installHostFileHooks(): Boolean {
            DefaultJestElectricRunnerNativeImpl.hostWorkingDirectoryProvider = {
                memScoped {
                    val buf = allocArray<ByteVar>(4096)
                    getcwd(buf, 4096u)?.toKString()
                }
            }
            DefaultJestElectricRunnerNativeImpl.hostFileOrDirExistsChecker = { path ->
                access(path, F_OK) == 0
            }
            DefaultJestElectricRunnerNativeImpl.hostCreateDirectoriesHook = { dirPath ->
                val norm = dirPath.replace('\\', '/').trimEnd('/')
                val isAbs = norm.startsWith("/")
                val parts = norm.split('/')
                var current = if (isAbs) "/" else ""
                for (part in parts) {
                    if (part.isEmpty()) continue
                    current = when {
                        current == "/" -> "/$part"
                        current.isEmpty() -> part
                        else -> "$current/$part"
                    }
                    mkdir(current, 493u.convert()) // 0755
                }
                true
            }
            DefaultJestElectricRunnerNativeImpl.hostWriteBytesHook = { filePath, bytes ->
                val file = fopen(filePath, "wb")
                if (file == null) {
                    false
                } else {
                    try {
                        if (bytes.isNotEmpty()) {
                            bytes.usePinned { pinned ->
                                fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
                            }
                        }
                        true
                    } finally {
                        fclose(file)
                    }
                }
            }
            return true
        }
    }

    init {
        installHostFileHooks()
    }

    override val backendName: String
        get() = if (visualWindowEnabled) "macos-appkit-visual" else "macos-appkit"

    override val backendTarget: BackendTarget
        get() = BackendTarget.MacOS

    override val isVisualWindowActive: Boolean
        get() = (visualWindowEnabled && MacosNativeWindowHost.activeNsWindow != null) || super.isVisualWindowActive

    override fun ensureBackendInitialized() {
        if (GenericUI.activeFactory !is MacosNativeFactory) {
            GenericUI.initialize(MacosNativeFactory())
        }
        MacosApplicationNativeImpl.interactiveMacosGuiEnabled = visualWindowEnabled
        MacosNativeWindowHost.visualTestMode = visualWindowEnabled
    }

    override fun launchVisualWindowForApp(app: Application, suiteName: String) {
        ensureBackendInitialized()
        activeApp = app
        if (visualWindowEnabled) {
            MacosNativeWindowHost.visualTestBannerText = "JestElectricRunner | $suiteName"
            val factory = GenericUI.requireFactory()
            app.run(factory)
            pumpUiEvents(stepDelayMs.coerceAtLeast(32L))
        } else {
            super.launchVisualWindowForApp(app, suiteName)
        }
    }

    override fun updateVisualTestBanner(testName: String, status: String) {
        super.updateVisualTestBanner(testName, status)
        if (visualWindowEnabled) {
            MacosNativeWindowHost.visualTestBannerText = "[$status] $testName"
            MacosNativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
    }

    override fun pumpUiEvents(durationMs: Long) {
        super.pumpUiEvents(durationMs)
        if (visualWindowEnabled) {
            MacosNativeWindowHost.pumpVisualWindowEvents(durationMs)
        }
    }

    override fun performNativeClick(component: AbstractComponent): Boolean {
        val ok = super.performNativeClick(component)
        if (ok && visualWindowEnabled) {
            MacosNativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeHover(component: AbstractComponent): Boolean {
        val ok = super.performNativeHover(component)
        if (ok && visualWindowEnabled) {
            MacosNativeWindowHost.invalidateAllWindows()
            MacosNativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeFocus(component: AbstractComponent): Boolean {
        val ok = super.performNativeFocus(component)
        if (ok && visualWindowEnabled) {
            MacosNativeWindowHost.invalidateAllWindows()
            MacosNativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeTypeText(component: AbstractComponent, text: String): Boolean {
        val ok = super.performNativeTypeText(component, text)
        if (ok && visualWindowEnabled) {
            MacosNativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeMenuClick(app: Application, menuTitle: String, itemLabel: String): Boolean {
        val ok = super.performNativeMenuClick(app, menuTitle, itemLabel)
        if (ok && visualWindowEnabled) {
            MacosNativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun closeVisualWindow() {
        if (visualWindowEnabled || MacosNativeWindowHost.activeNsWindow != null) {
            MacosNativeWindowHost.closeAllVisualWindows()
            MacosNativeWindowHost.visualTestBannerText = null
        }
        super.closeVisualWindow()
    }

    override fun captureScreenshot(
        app: Application?,
        width: Int,
        height: Int
    ): ImageBuffer? {
        if (visualWindowEnabled && MacosNativeWindowHost.activeNsWindow != null) {
            MacosNativeWindowHost.captureWindowScreenshot(width, height)?.let { return it }
        }
        return super.captureScreenshot(app, width, height)
    }
}
