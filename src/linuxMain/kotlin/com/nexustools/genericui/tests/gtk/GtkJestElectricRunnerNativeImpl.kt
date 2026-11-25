package com.nexustools.genericui.tests.gtk

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.gtk.GtkApplicationNativeImpl
import com.nexustools.genericui.gtk.GtkNativeFactory
import com.nexustools.genericui.gtk.GtkNativeWindowHost
import com.nexustools.genericui.gtk.GtkWindowNativeImpl
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.ImageEncodeOptions
import com.nexustools.genericui.image.ImageFormat
import com.nexustools.genericui.image.codec.LibPngCodec
import com.nexustools.genericui.linux.LinuxGdkPixbufImageCodecNativeImpl
import com.nexustools.genericui.tests.desktop.DesktopSharedRunnerNativeImpl

/**
 * Linux GTK4 runner implementation of `JestElectricRunnerNativeImpl`.
 *
 * Delegates window presentation and pixel screenshot capture directly to [GtkNativeWindowHost]
 * against the live retained GTK4 native widget hierarchy without falling back to synthetic snapshots.
 */
class GtkJestElectricRunnerNativeImpl(
    var visualWindowEnabled: Boolean = false,
    var stepDelayMs: Long = 8L
) : DesktopSharedRunnerNativeImpl(
    factoryProvider = { GtkNativeFactory() },
    expectedTarget = BackendTarget.Gtk
) {
    override val backendTarget: BackendTarget
        get() = BackendTarget.Gtk

    override val backendName: String
        get() = "linux-gtk4"

    override fun ensureBackendInitialized() {
        super.ensureBackendInitialized()
        GtkApplicationNativeImpl.interactiveGtkGuiEnabled = visualWindowEnabled
        GtkNativeWindowHost.visualTestMode = visualWindowEnabled
    }

    override fun launchVisualWindowForApp(app: Application, suiteName: String) {
        ensureBackendInitialized()
        activeApp = app
        if (visualWindowEnabled) {
            val factory = GenericUI.requireFactory()
            app.run(factory)
            (app.mainWindow.nativeImpl as? GtkWindowNativeImpl)?.let { winImpl ->
                if (winImpl.isVisible()) {
                    GtkNativeWindowHost.presentWindow(winImpl)
                }
            }
            pumpUiEvents(stepDelayMs.coerceAtLeast(16L))
        } else {
            super.launchVisualWindowForApp(app, suiteName)
        }
    }

    override fun pumpUiEvents(durationMs: Long) {
        super.pumpUiEvents(durationMs)
        if (visualWindowEnabled) {
            GtkNativeWindowHost.pumpEvents(maxIterations = 8, sleepMsPerStep = 4)
        }
    }

    override fun captureScreenshot(
        app: Application?,
        width: Int,
        height: Int
    ): ImageBuffer? {
        ensureBackendInitialized()
        GtkNativeWindowHost.prepareEventLoop()
        val targetApp = prepareAppForScreenshot(app, pumpMs = 16L) ?: return null

        val winImpl = targetApp.mainWindow.nativeImpl as? GtkWindowNativeImpl ?: return null
        val fallbackSize = winImpl.getWindowSize()
        val targetW = (if (width > 0) width else fallbackSize.width).coerceAtLeast(1)
        val targetH = (if (height > 0) height else fallbackSize.height).coerceAtLeast(1)
        winImpl.setWindowSize(Size(targetW, targetH))

        if (visualWindowEnabled) {
            GtkNativeWindowHost.presentWindow(winImpl)
            GtkNativeWindowHost.pumpEvents(maxIterations = 8, sleepMsPerStep = 4)
        }
        return GtkNativeWindowHost.captureWindowScreenshot(winImpl, targetW, targetH)
    }

    override fun captureScreenshotPng(
        app: Application?,
        width: Int,
        height: Int
    ): ByteArray? {
        val buffer = captureScreenshot(app, width, height) ?: return null
        return LinuxGdkPixbufImageCodecNativeImpl().encode(
            buffer = buffer,
            options = ImageEncodeOptions(format = ImageFormat.PNG)
        ) ?: LibPngCodec.encode(buffer)
    }
}
