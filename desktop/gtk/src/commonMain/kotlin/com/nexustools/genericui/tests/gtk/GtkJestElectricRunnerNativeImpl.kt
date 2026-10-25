package com.nexustools.genericui.tests.gtk

import com.nexustools.genericui.gtk.GtkNativeFactory
import com.nexustools.genericui.tests.desktop.DesktopSharedRunnerNativeImpl

/**
 * Linux GTK4 runner implementation (`:tests:desktop:gtk`) of `JestElectricRunnerNativeImpl`.
 */
class GtkJestElectricRunnerNativeImpl : DesktopSharedRunnerNativeImpl({ GtkNativeFactory() }) {
    override val backendName: String
        get() = "linux-gtk4"
}
