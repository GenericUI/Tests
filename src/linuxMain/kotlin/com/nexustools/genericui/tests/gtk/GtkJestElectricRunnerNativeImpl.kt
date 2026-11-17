package com.nexustools.genericui.tests.gtk

import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.gtk.GtkNativeFactory
import com.nexustools.genericui.tests.desktop.DesktopSharedRunnerNativeImpl

/**
 * Linux GTK4 runner implementation of `JestElectricRunnerNativeImpl`.
 */
class GtkJestElectricRunnerNativeImpl : DesktopSharedRunnerNativeImpl(
    factoryProvider = { GtkNativeFactory() },
    expectedTarget = BackendTarget.Gtk
) {
    override val backendTarget: BackendTarget
        get() = BackendTarget.Gtk

    override val backendName: String
        get() = "linux-gtk4"
}
