package com.nexustools.genericui.tests.ios

import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.ios.IosNativeFactory
import com.nexustools.genericui.tests.mobile.MobileSharedRunnerNativeImpl

/**
 * iOS UIKit runner implementation (`BackendTarget.IOS`) of `JestElectricRunnerNativeImpl`.
 */
class IosJestElectricRunnerNativeImpl : MobileSharedRunnerNativeImpl(
    factoryProvider = { IosNativeFactory() },
    expectedTarget = BackendTarget.IOS
) {
    override val backendTarget: BackendTarget
        get() = BackendTarget.IOS

    override val backendName: String
        get() = "ios-uikit"
}
