@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.ExperimentalStdlibApi::class)

package com.nexustools.genericui.tests.windows

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.tests.desktop.DesktopSharedRunnerNativeImpl
import com.nexustools.genericui.windows.Win32NativeWindowHost
import com.nexustools.genericui.windows.WindowsApplicationNativeImpl
import com.nexustools.genericui.windows.WindowsNativeFactory
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix._getcwd
import platform.posix._mkdir
import platform.posix.access
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

@EagerInitialization
private val windowsScreenshotHostHooksInit: Boolean = WindowsVisualAppRunnerNativeImpl.installHostFileHooks()

/**
 * Windows Win32 visual runner implementation (`:tests:desktop:windows`) of `JestElectricRunnerNativeImpl`.
 *
 * When [visualWindowEnabled] is `true` (default), launching an [Application] in a `JestElectricRunner`
 * test opens the real Win32 native window (`Win32NativeWindowHost`) on screen with `visualTestMode = true`,
 * displays a live test banner in the window titlebar, pumps Win32 messages between interactions, and
 * visually executes clicks, text entry, hover/focus states, and menu actions in real time.
 */
class WindowsVisualAppRunnerNativeImpl(
    var visualWindowEnabled: Boolean = true,
    var stepDelayMs: Long = 24L
) : DesktopSharedRunnerNativeImpl({ WindowsNativeFactory() }) {

    companion object {
        fun installHostFileHooks(): Boolean {
            DefaultJestElectricRunnerNativeImpl.hostWorkingDirectoryProvider = {
                memScoped {
                    val buf = allocArray<ByteVar>(4096)
                    _getcwd(buf, 4096)?.toKString()
                }
            }
            DefaultJestElectricRunnerNativeImpl.hostFileOrDirExistsChecker = { path ->
                access(path.replace('/', '\\'), 0) == 0
            }
            DefaultJestElectricRunnerNativeImpl.hostCreateDirectoriesHook = { dirPath ->
                val norm = dirPath.replace('\\', '/').trimEnd('/')
                val parts = norm.split('/')
                var current = ""
                for (part in parts) {
                    if (part.isEmpty()) continue
                    current = if (current.isEmpty()) part else "$current/$part"
                    if (current.length == 2 && current[1] == ':') continue
                    _mkdir(current.replace('/', '\\'))
                }
                true
            }
            DefaultJestElectricRunnerNativeImpl.hostWriteBytesHook = { filePath, bytes ->
                val file = fopen(filePath.replace('/', '\\'), "wb")
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
        get() = if (visualWindowEnabled) "windows-win32-visual" else "windows-win32-headless"

    override val isVisualWindowActive: Boolean
        get() = Win32NativeWindowHost.activeHwnd != null || super.isVisualWindowActive

    override fun ensureBackendInitialized() {
        if (GenericUI.activeFactory !is WindowsNativeFactory) {
            GenericUI.initialize(WindowsNativeFactory())
        }
        WindowsApplicationNativeImpl.interactiveWin32GuiEnabled = visualWindowEnabled
        Win32NativeWindowHost.visualTestMode = true
    }

    override fun launchVisualWindowForApp(app: Application, suiteName: String) {
        ensureBackendInitialized()
        activeApp = app
        Win32NativeWindowHost.visualTestBannerText = "JestElectricRunner | $suiteName"
        val factory = GenericUI.requireFactory()
        app.run(factory)
        pumpUiEvents(stepDelayMs.coerceAtLeast(32L))
    }

    override fun updateVisualTestBanner(testName: String, status: String) {
        super.updateVisualTestBanner(testName, status)
        Win32NativeWindowHost.visualTestBannerText = "[$status] $testName"
        if (visualWindowEnabled) {
            Win32NativeWindowHost.invalidateAllWindows()
            Win32NativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
    }

    override fun pumpUiEvents(durationMs: Long) {
        super.pumpUiEvents(durationMs)
        if (visualWindowEnabled) {
            Win32NativeWindowHost.invalidateAllWindows()
            Win32NativeWindowHost.pumpVisualWindowEvents(durationMs)
        }
    }

    override fun performNativeClick(component: AbstractComponent): Boolean {
        val ok = super.performNativeClick(component)
        if (ok && visualWindowEnabled) {
            Win32NativeWindowHost.invalidateAllWindows()
            Win32NativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeHover(component: AbstractComponent): Boolean {
        val ok = super.performNativeHover(component)
        if (ok && visualWindowEnabled) {
            Win32NativeWindowHost.invalidateAllWindows()
            Win32NativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeFocus(component: AbstractComponent): Boolean {
        val ok = super.performNativeFocus(component)
        if (ok && visualWindowEnabled) {
            Win32NativeWindowHost.invalidateAllWindows()
            Win32NativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeTypeText(component: AbstractComponent, text: String): Boolean {
        val ok = super.performNativeTypeText(component, text)
        if (ok && visualWindowEnabled) {
            Win32NativeWindowHost.invalidateAllWindows()
            Win32NativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeMenuClick(app: Application, menuTitle: String, itemLabel: String): Boolean {
        val ok = super.performNativeMenuClick(app, menuTitle, itemLabel)
        if (ok && visualWindowEnabled) {
            Win32NativeWindowHost.invalidateAllWindows()
            Win32NativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun closeVisualWindow() {
        Win32NativeWindowHost.closeAllVisualWindows()
        Win32NativeWindowHost.visualTestBannerText = null
        super.closeVisualWindow()
    }

    override fun captureScreenshot(
        app: Application?,
        width: Int,
        height: Int
    ): ImageBuffer? {
        if (visualWindowEnabled && Win32NativeWindowHost.activeHwnd != null) {
            Win32NativeWindowHost.captureWindowScreenshot(width, height)?.let { return it }
        }
        return super.captureScreenshot(app, width, height)
    }
}
