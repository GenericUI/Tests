package com.nexustools.genericui.tests.ipados

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.ipados.IpadosMenuBarBridge
import com.nexustools.genericui.ipados.IpadosNativeFactory
import com.nexustools.genericui.tests.mobile.MobileSharedRunnerNativeImpl
import com.nexustools.genericui.tests.mobile.UikitScreenshotCaptureHelper

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

    override fun closeVisualWindow() {
        super.closeVisualWindow()
        if (IpadosMenuBarBridge.activeMenuBar != null) {
            IpadosMenuBarBridge.registerAndRebuild(null)
        }
    }
}
