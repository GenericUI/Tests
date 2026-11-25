@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nexustools.genericui.tests.mobile

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.enums.ui.ThemeMode
import com.nexustools.genericui.geometry.Size
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.ImageColorSpace
import com.nexustools.genericui.image.PixelFormat
import com.nexustools.genericui.ios.IosWindowImpl
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.Foundation.NSData
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIView
import platform.posix.memcpy
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shared genuine UIKit offscreen screenshot capture helper for iOS (`IosWindowImpl`)
 * and iPadOS (`IpadosWindowImpl`).
 */
internal object UikitScreenshotCaptureHelper {

    fun captureImageBuffer(
        runner: DefaultJestElectricRunnerNativeImpl,
        app: Application?,
        width: Int,
        height: Int
    ): ImageBuffer? {
        val targetApp = runner.prepareAppForScreenshot(app, pumpMs = 16L) ?: return null
        val (uiImage, targetW, targetH) = renderAppToUiImage(targetApp, width, height) ?: return null
        val cgImage = uiImage.CGImage ?: return null
        val premul = ByteArray(targetW * targetH * 4)
        val colorSpace = CGColorSpaceCreateDeviceRGB() ?: return null
        val ok = try {
            premul.usePinned { pinned ->
                val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big
                val cgCtx = CGBitmapContextCreate(
                    data = pinned.addressOf(0),
                    width = targetW.convert(),
                    height = targetH.convert(),
                    bitsPerComponent = 8uL,
                    bytesPerRow = (targetW * 4).convert(),
                    space = colorSpace,
                    bitmapInfo = bitmapInfo
                ) ?: return@usePinned false
                try {
                    CGContextDrawImage(
                        cgCtx,
                        CGRectMake(0.0, 0.0, targetW.toDouble(), targetH.toDouble()),
                        cgImage
                    )
                    true
                } finally {
                    CGContextRelease(cgCtx)
                }
            }
        } finally {
            CGColorSpaceRelease(colorSpace)
        }
        if (!ok) return null

        val rgba = ByteArray(premul.size)
        var i = 0
        while (i + 3 < premul.size) {
            val r = premul[i].toInt() and 0xFF
            val g = premul[i + 1].toInt() and 0xFF
            val b = premul[i + 2].toInt() and 0xFF
            val a = premul[i + 3].toInt() and 0xFF
            if (a in 1..254) {
                rgba[i] = min(255, (r * 255 + a / 2) / a).toByte()
                rgba[i + 1] = min(255, (g * 255 + a / 2) / a).toByte()
                rgba[i + 2] = min(255, (b * 255 + a / 2) / a).toByte()
            } else {
                rgba[i] = r.toByte()
                rgba[i + 1] = g.toByte()
                rgba[i + 2] = b.toByte()
            }
            rgba[i + 3] = a.toByte()
            i += 4
        }
        return ImageBuffer(
            width = targetW,
            height = targetH,
            pixelFormat = PixelFormat.RGBA_8888,
            colorSpace = ImageColorSpace.SRGB,
            bytes = rgba
        )
    }

    fun capturePngBytes(
        runner: DefaultJestElectricRunnerNativeImpl,
        app: Application?,
        width: Int,
        height: Int
    ): ByteArray? {
        val targetApp = runner.prepareAppForScreenshot(app, pumpMs = 16L) ?: return null
        val (uiImage, _, _) = renderAppToUiImage(targetApp, width, height) ?: return null
        val pngData: NSData = UIImagePNGRepresentation(uiImage) ?: return null
        val len = pngData.length.toInt()
        if (len <= 0) return null
        val out = ByteArray(len)
        val src = pngData.bytes ?: return null
        out.usePinned { pinned ->
            memcpy(pinned.addressOf(0), src, len.convert())
        }
        return out
    }

    private fun renderAppToUiImage(
        app: Application,
        width: Int,
        height: Int
    ): Triple<UIImage, Int, Int>? {
        val windowImpl = app.mainWindow.nativeImpl as? IosWindowImpl ?: return null
        val rootViewController = windowImpl.rootViewController
        rootViewController.loadViewIfNeeded()
        val hostView = rootViewController.view

        val currentW = hostView.bounds.useContents { size.width.roundToInt() }.takeIf { it > 0 }
            ?: windowImpl.getWindowSize().width.coerceAtLeast(390)
        val currentH = hostView.bounds.useContents { size.height.roundToInt() }.takeIf { it > 0 }
            ?: windowImpl.getWindowSize().height.coerceAtLeast(844)
        val targetW = (if (width > 0) width else currentW).coerceAtLeast(1)
        val targetH = (if (height > 0) height else currentH).coerceAtLeast(1)

        windowImpl.setWindowSize(Size(targetW, targetH))
        hostView.setFrame(CGRectMake(0.0, 0.0, targetW.toDouble(), targetH.toDouble()))
        forceLayoutTree(hostView)
        rootViewController.viewDidLayoutSubviews()
        forceLayoutTree(hostView)

        val isDark = app.getResolvedTheme() == ThemeMode.DARK
        val effectiveColors = app.getThemeConfig().resolveEffectiveColors(isDark)
        val bgRgba = effectiveColors.background ?: if (isDark) RgbaColor(0, 0, 0) else RgbaColor(255, 255, 255)
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply {
            scale = 1.0
            opaque = true
        }
        val renderer = UIGraphicsImageRenderer(
            size = CGSizeMake(targetW.toDouble(), targetH.toDouble()),
            format = format
        )
        val rendered = renderer.imageWithActions { rendererCtx ->
            val uiBg = UIColor.colorWithRed(
                bgRgba.r / 255.0,
                bgRgba.g / 255.0,
                bgRgba.b / 255.0,
                1.0
            )
            uiBg.setFill()
            rendererCtx?.fillRect(CGRectMake(0.0, 0.0, targetW.toDouble(), targetH.toDouble()))
            val cgCtx = rendererCtx?.CGContext ?: return@imageWithActions
            hostView.layer.renderInContext(cgCtx)
        }
        return Triple(rendered, targetW, targetH)
    }

    private fun forceLayoutTree(view: UIView) {
        view.setNeedsLayout()
        view.layoutIfNeeded()
        for (child in view.subviews) {
            if (child is UIView) {
                forceLayoutTree(child)
            }
        }
    }
}
