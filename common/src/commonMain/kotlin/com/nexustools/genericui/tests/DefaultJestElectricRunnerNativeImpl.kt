package com.nexustools.genericui.tests

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.core.GenericUI

/**
 * Cross-platform default implementation of [JestElectricRunnerNativeImpl].
 *
 * Drives component interactions, CSS transitions, and `Fragment` / `Application` lifecycles
 * in-memory and delegates to `GenericUI.activeFactory` when a platform backend is initialized.
 */
open class DefaultJestElectricRunnerNativeImpl : JestElectricRunnerNativeImpl {

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
        } else {
            app.mainWindow.show(null)
            if (app is FragmentApplication) {
                app.rootFragmentContainer.ensureActiveFragmentStartedAndResumed()
            }
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

    override fun performNativeClick(component: Component): Boolean {
        if (!component.isEnabled || !component.isVisible) return false
        when (component) {
            is CheckBox -> {
                component.isChecked = !component.isChecked
                component.onClick?.invoke()
            }
            is Button -> {
                component.click()
            }
            else -> {
                component.onClick?.invoke()
            }
        }
        pumpUiEvents(16L)
        return true
    }

    override fun performNativeHover(component: Component): Boolean {
        if (!component.isVisible) return false
        component.isHovered = true
        pumpUiEvents(16L)
        return true
    }

    override fun performNativeFocus(component: Component): Boolean {
        if (!component.isEnabled || !component.isVisible) return false
        component.isFocused = true
        pumpUiEvents(16L)
        return true
    }

    override fun performNativeTypeText(component: Component, text: String): Boolean {
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
}
