package com.nexustools.genericui.tests.ios

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.ios.IosNativeFactory
import com.nexustools.genericui.tests.mobile.MobileSharedRunnerNativeImpl
import com.nexustools.genericui.tests.mobile.UikitScreenshotCaptureHelper

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

    override fun captureScreenshot(
        app: Application?,
        width: Int,
        height: Int
    ): ImageBuffer? = UikitScreenshotCaptureHelper.captureImageBuffer(this, app, width, height)

    override fun captureScreenshotPng(
        app: Application?,
        width: Int,
        height: Int
    ): ByteArray? = UikitScreenshotCaptureHelper.capturePngBytes(this, app, width, height)
}
