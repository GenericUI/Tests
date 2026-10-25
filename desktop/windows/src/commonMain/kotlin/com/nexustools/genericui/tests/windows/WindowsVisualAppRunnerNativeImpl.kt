package com.nexustools.genericui.tests.windows

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.tests.Component
import com.nexustools.genericui.tests.desktop.DesktopSharedRunnerNativeImpl
import com.nexustools.genericui.windows.Win32NativeWindowHost
import com.nexustools.genericui.windows.WindowsApplicationNativeImpl
import com.nexustools.genericui.windows.WindowsNativeFactory

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

    override fun performNativeClick(component: Component): Boolean {
        val ok = super.performNativeClick(component)
        if (ok && visualWindowEnabled) {
            Win32NativeWindowHost.invalidateAllWindows()
            Win32NativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeHover(component: Component): Boolean {
        val ok = super.performNativeHover(component)
        if (ok && visualWindowEnabled) {
            Win32NativeWindowHost.invalidateAllWindows()
            Win32NativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeFocus(component: Component): Boolean {
        val ok = super.performNativeFocus(component)
        if (ok && visualWindowEnabled) {
            Win32NativeWindowHost.invalidateAllWindows()
            Win32NativeWindowHost.pumpVisualWindowEvents(stepDelayMs)
        }
        return ok
    }

    override fun performNativeTypeText(component: Component, text: String): Boolean {
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
}
