package com.nexustools.genericui.tests.ipados

import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.ipados.IpadosMenuBarBridge
import com.nexustools.genericui.ipados.IpadosNativeFactory
import com.nexustools.genericui.tests.mobile.MobileSharedRunnerNativeImpl

/**
 * iPadOS UIKit (`UIWindowScene` + `UIMenuSystem`) runner implementation (`BackendTarget.IPadOS`)
 * of `JestElectricRunnerNativeImpl`.
 */
class IpadosJestElectricRunnerNativeImpl : MobileSharedRunnerNativeImpl(
    factoryProvider = { IpadosNativeFactory() },
    expectedTarget = BackendTarget.IPadOS
) {
    override val backendTarget: BackendTarget
        get() = BackendTarget.IPadOS

    override val backendName: String
        get() = "ipados-uikit"

    override fun closeVisualWindow() {
        super.closeVisualWindow()
        if (IpadosMenuBarBridge.activeMenuBar != null) {
            IpadosMenuBarBridge.registerAndRebuild(null)
        }
    }
}
