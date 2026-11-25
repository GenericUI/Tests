package com.nexustools.genericui.tests

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.layout.StyledContainer
import com.nexustools.genericui.component.layout.VStack
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Heading
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.ImageFormat
import com.nexustools.genericui.image.codec.LibPngCodec
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.style.css.CssLength
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Shared test utilities for verifying native screenshot buffers and encoded PNG outputs
 * across `commonTest` and platform test suites.
 */
internal object ScreenshotVerificationHelper {

    fun buildStyledSampleFragmentApp(
        title: String,
        statusId: String = "screenshot-status",
        buttonId: String = "screenshot-btn"
    ): FragmentApplication {
        val frag = object : Fragment() {
            override fun onCreate(savedState: FragmentBundle?) {
                val card = StyledContainer().apply {
                    backgroundColor = RgbaColor(30, 41, 59)
                    borderColor = RgbaColor(96, 165, 250)
                    borderWidthPx = 2f
                    borderRadiusPx = 12f
                    style.paddingLeft = CssLength.px(16)
                    style.paddingRight = CssLength.px(16)
                    style.paddingTop = CssLength.px(14)
                    style.paddingBottom = CssLength.px(14)

                    addChild(
                        VStack(gapPx = 8).apply {
                            addChild(Heading(title, level = 2))
                            addChild(
                                Text("Native Backend Render Verification").apply {
                                    id = statusId
                                }
                            )
                            addChild(
                                Button("Primary Action", type = BootstrapType.PRIMARY).apply {
                                    id = buttonId
                                }
                            )
                        }
                    )
                }
                addChild(card)
            }
        }
        return FragmentApplication(initialFragment = frag, title = title)
    }

    fun hasPngMagicHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 24) return false
        return (bytes[0].toInt() and 0xFF) == 0x89 &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() &&
            bytes[3] == 'G'.code.toByte() &&
            (bytes[4].toInt() and 0xFF) == 0x0D &&
            (bytes[5].toInt() and 0xFF) == 0x0A &&
            (bytes[6].toInt() and 0xFF) == 0x1A &&
            (bytes[7].toInt() and 0xFF) == 0x0A
    }

    fun assertHasNonUniformPixels(buffer: ImageBuffer, contextLabel: String) {
        val bytes = buffer.bytes
        assertTrue(bytes.size >= buffer.width * buffer.height * 4, "$contextLabel: pixel buffer too small")
        val r0 = bytes[0]
        val g0 = bytes[1]
        val b0 = bytes[2]
        var hasVariation = false
        var i = 4
        while (i + 2 < bytes.size) {
            if (bytes[i] != r0 || bytes[i + 1] != g0 || bytes[i + 2] != b0) {
                hasVariation = true
                break
            }
            i += 4
        }
        assertTrue(hasVariation, "$contextLabel: expected rendered screenshot to contain non-uniform pixels")
    }

    fun assertValidNativePngScreenshot(
        buffer: ImageBuffer?,
        pngBytes: ByteArray?,
        expectedW: Int,
        expectedH: Int,
        contextLabel: String
    ) {
        val nonNullBuffer = assertNotNull(buffer, "$contextLabel: captureScreenshot returned null")
        assertEquals(expectedW, nonNullBuffer.width, "$contextLabel: screenshot width mismatch")
        assertEquals(expectedH, nonNullBuffer.height, "$contextLabel: screenshot height mismatch")
        assertHasNonUniformPixels(nonNullBuffer, contextLabel)

        val nonNullPng = assertNotNull(pngBytes, "$contextLabel: captureScreenshotPng returned null")
        assertTrue(nonNullPng.size > 64, "$contextLabel: PNG byte payload too small (${nonNullPng.size})")
        assertTrue(hasPngMagicHeader(nonNullPng), "$contextLabel: invalid PNG magic header")

        val metadata = LibPngCodec.probePngMetadata(nonNullPng)
        assertEquals(ImageFormat.PNG, metadata.format, "$contextLabel: expected PNG format metadata")
        assertEquals(expectedW, metadata.width, "$contextLabel: PNG IHDR width mismatch")
        assertEquals(expectedH, metadata.height, "$contextLabel: PNG IHDR height mismatch")
    }
}
