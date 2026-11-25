@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.ExperimentalStdlibApi::class)

package com.nexustools.genericui.tests.windows

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.ImageEncodeOptions
import com.nexustools.genericui.image.ImageFormat
import com.nexustools.genericui.image.codec.LibPngCodec
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.tests.desktop.DesktopSharedRunnerNativeImpl
import com.nexustools.genericui.windows.Win32NativeWindowHost
import com.nexustools.genericui.windows.WindowsApplicationNativeImpl
import com.nexustools.genericui.windows.WindowsNativeFactory
import com.nexustools.genericui.windows.WindowsWicImageCodecNativeImpl
import com.nexustools.genericui.windows.WindowsWindowNativeImpl
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix._getcwd
import platform.posix.access
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.windows.CreateDirectoryA

@EagerInitialization
private val windowsScreenshotHostHooksInit: Boolean = WindowsVisualAppRunnerNativeImpl.installHostFileHooks()

/**
 * Windows Win32 runner implementation of `JestElectricRunnerNativeImpl`.
 *
 * Delegates window presentation and pixel screenshot capture to [Win32NativeWindowHost] against
 * the live retained Win32 native control hierarchy (`visualWindowEnabled = true` on-screen or
 * `visualWindowEnabled = false` headless), never falling back to synthetic component snapshots.
 */
class WindowsVisualAppRunnerNativeImpl(
    var visualWindowEnabled: Boolean = true,
    var stepDelayMs: Long = 24L
) : DesktopSharedRunnerNativeImpl({ WindowsNativeFactory() }, expectedTarget = BackendTarget.Windows) {

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
                    CreateDirectoryA(current.replace('/', '\\'), null)
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

    override val backendTarget: BackendTarget
        get() = BackendTarget.Windows

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
        val targetApp = prepareAppForScreenshot(app, pumpMs = 16L) ?: return null

        val winImpl = targetApp.mainWindow.nativeImpl as? WindowsWindowNativeImpl ?: return null
        val fallbackSize = winImpl.getWindowSize()
        val targetW = (if (width > 0) width else fallbackSize.width).coerceAtLeast(1)
        val targetH = (if (height > 0) height else fallbackSize.height).coerceAtLeast(1)
        winImpl.setWindowSize(Size(targetW, targetH))

        return Win32NativeWindowHost.captureWindowScreenshot(winImpl, targetW, targetH)
    }

    override fun captureScreenshotPng(
        app: Application?,
        width: Int,
        height: Int
    ): ByteArray? {
        val buffer = captureScreenshot(app, width, height) ?: return null
        return WindowsWicImageCodecNativeImpl().encode(
            buffer = buffer,
            options = ImageEncodeOptions(format = ImageFormat.PNG)
        ) ?: LibPngCodec.encode(buffer)
    }
}
