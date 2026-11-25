@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nexustools.genericui.tests.macos

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.macos.MacosNativeFactory
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.tests.ScreenshotVerificationHelper
import com.nexustools.genericui.tests.createPlatformDefaultJestElectricRunner
import platform.posix.remove
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Self-test for the macOS AppKit runner (`MacosJestElectricRunnerNativeImpl` and `MacosJestElectricBootstrap`).
 */
class MacosRunnerTest {

    @AfterTest
    fun tearDownMacosRunner() {
        JestElectricNativeRegistry.resetToDefault()
        GenericUI.activeFactory = null
    }

    @Test
    fun testMacosRunnerExecutesHeadlessFragmentSuite() {
        val runner = MacosJestElectricBootstrap.install(visualWindowEnabled = false, stepDelayMs = 8L)
        assertEquals(BackendTarget.MacOS, runner.backendTarget)
        assertEquals("macos-appkit", runner.backendName)
        assertTrue(GenericUI.activeFactory is MacosNativeFactory)

        val report = JestElectricRunner.describe(
            suiteName = "macOS AppKit Runner Verification",
            visualOnWindows = false,
            runnerImpl = runner
        ) {
            it("mounts FragmentApplication with MacosNativeFactory and handles clicks") {
                val appCtrl = launchApp(visualOnWindows = false) {
                    val frag = object : Fragment() {
                        override fun onCreate(savedState: FragmentBundle?) {
                            val label = Text("macOS Ready").apply { id = "macos-status" }
                            val btn = Button("Run").apply {
                                id = "macos-btn"
                                onClick = { label.text = "macOS Done" }
                            }
                            addChild(label)
                            addChild(btn)
                        }
                    }
                    FragmentApplication(initialFragment = frag, title = "macOS Runner Test")
                }

                appCtrl.findById("macos-status").assertTextEquals("macOS Ready")
                appCtrl.findById("macos-btn").performClick()
                appCtrl.findById("macos-status").assertTextEquals("macOS Done")
            }

            it("captures genuine AppKit offscreen PNG screenshot") {
                val appCtrl = launchApp(visualOnWindows = false) {
                    ScreenshotVerificationHelper.buildStyledSampleFragmentApp("macOS AppKit PNG")
                }
                val buffer = appCtrl.captureScreenshot(width = 480, height = 320)
                val pngBytes = appCtrl.captureScreenshotPng(width = 480, height = 320)
                ScreenshotVerificationHelper.assertValidNativePngScreenshot(
                    buffer = buffer,
                    pngBytes = pngBytes,
                    expectedW = 480,
                    expectedH = 320,
                    contextLabel = "macOS"
                )

                val outPath = "/tmp/genericui_macos_runner_shot.png"
                try {
                    val saved = appCtrl.takeScreenshotPng(outPath, width = 480, height = 320)
                    assertEquals(outPath, saved)
                } finally {
                    remove(outPath)
                }
            }
        }

        assertTrue(report.allPassed)
    }

    @Test
    fun testPlatformDefaultRunnerSelectsMacosNativeRunner() {
        JestElectricNativeRegistry.resetToDefault()
        val defaultRunner = createPlatformDefaultJestElectricRunner()
        assertNotEquals(DefaultJestElectricRunnerNativeImpl::class, defaultRunner::class)
        assertTrue(defaultRunner is MacosJestElectricRunnerNativeImpl)
        assertTrue(JestElectricNativeRegistry.current() is MacosJestElectricRunnerNativeImpl)
        assertEquals(BackendTarget.MacOS, defaultRunner.backendTarget)
        assertTrue(GenericUI.activeFactory is MacosNativeFactory)
    }
}
