package com.nexustools.genericui.tests

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.Disposable

/**
 * Platform-native runner bridge for `JestElectricRunner`.
 *
 * Each of the 8 GenericUI target backends (`:windows`, `:macos`, `:gtk`, `:qt`, `:swing`,
 * `:android`, `:ios`, `:web`) provides a concrete implementation of this interface.
 *
 * On `:desktop:windows`, the implementation launches the actual Win32 window via
 * `Win32NativeWindowHost` with `visualTestMode = true`, renders a live visual test banner
 * in the window titlebar, pumps the Win32 message queue between interactions, and performs
 * automated interactions directly against the live GUI.
 */
interface JestElectricRunnerNativeImpl : Disposable {

    /**
     * Identifier for the active native backend (for example `"windows-win32-visual"`, `"swing-junit"`, `"android-robolectric"`).
     */
    val backendName: String

    /**
     * Returns `true` when an interactive visual native window is currently open on screen.
     */
    val isVisualWindowActive: Boolean

    /**
     * Ensures the underlying `GenericUI` `NativeFactory` and platform toolkit are initialized.
     */
    fun ensureBackendInitialized()

    /**
     * Launches the given [app] for testing.
     * On `:desktop:windows` (when visual mode is enabled), this creates and displays the real
     * Win32 native window on screen so the developer can watch the automated test run live.
     */
    fun launchVisualWindowForApp(app: Application, suiteName: String)

    /**
     * Updates the visual test status banner (for example in the Win32 titlebar or overlay)
     * with the currently executing [testName] and [status].
     */
    fun updateVisualTestBanner(testName: String, status: String)

    /**
     * Pumps pending UI/window events and advances CSS/layout transitions by [durationMs].
     */
    fun pumpUiEvents(durationMs: Long = 32L)

    /**
     * Dispatches a native + logical click event to [component].
     */
    fun performNativeClick(component: Component): Boolean

    /**
     * Dispatches a native + logical hover state change to [component].
     */
    fun performNativeHover(component: Component): Boolean

    /**
     * Dispatches a native + logical focus event to [component].
     */
    fun performNativeFocus(component: Component): Boolean

    /**
     * Types [text] into a text-input [component] (`TextField` / `TextArea`) and fires change callbacks.
     */
    fun performNativeTypeText(component: Component, text: String): Boolean

    /**
     * Invokes a top-level menu item matching [menuTitle] -> [itemLabel] on [app].
     */
    fun performNativeMenuClick(app: Application, menuTitle: String, itemLabel: String): Boolean

    /**
     * Closes any active visual native window opened during the test session.
     */
    fun closeVisualWindow()

    override fun dispose() {
        closeVisualWindow()
    }
}
