package com.nexustools.genericui.tests.ios

import com.nexustools.genericui.ios.IosNativeFactory
import com.nexustools.genericui.tests.mobile.MobileSharedRunnerNativeImpl

/**
 * iOS UIKit runner implementation (`:tests:mobile:ios`) of `JestElectricRunnerNativeImpl`.
 */
class IosJestElectricRunnerNativeImpl : MobileSharedRunnerNativeImpl({ IosNativeFactory() }) {
    override val backendName: String
        get() = "ios-uikit"
}
