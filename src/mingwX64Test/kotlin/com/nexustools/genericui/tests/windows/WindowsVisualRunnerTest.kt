@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nexustools.genericui.tests.windows

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.desktop.core.DesktopNativeWidget
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.tests.ScreenshotVerificationHelper
import com.nexustools.genericui.tests.createPlatformDefaultJestElectricRunner
import com.nexustools.genericui.windows.Win32NativeWindowHost
import com.nexustools.genericui.windows.WindowsNativeFactory
import platform.posix.getenv
import platform.posix.remove
import kotlinx.cinterop.toKString
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Self-tests for Windows (`WindowsVisualAppRunnerNativeImpl` and `WindowsJestElectricBootstrap`).
 */
class WindowsVisualRunnerTest {

    @AfterTest
    fun tearDownWindowsRunner() {
        JestElectricNativeRegistry.resetToDefault()
        GenericUI.activeFactory = null
    }

    @Test
    fun testWindowsVisualRunnerExecutesFragmentAppSuite() {
        val runner = WindowsJestElectricBootstrap.install(
            visualWindowEnabled = false,
            stepDelayMs = 8L
        )

        val report = JestElectricRunner.describe(
            suiteName = "Windows Win32 Visual Runner Verification",
            visualOnWindows = false,
            runnerImpl = runner
        ) {
            it("initializes WindowsNativeFactory, runs FragmentApplication, and updates Win32 banner") {
                var submittedName = ""
                val appCtrl = launchApp(visualOnWindows = false) {
                    val fragment = object : Fragment() {
                        override fun onCreate(savedState: FragmentBundle?) {
                            val header = Text("Windows Visual Test Ready").apply {
                                id = "win-header"
                            }
                            val input = TextField(value = "", placeholder = "Canvas Name").apply {
                                id = "win-input"
                            }
                            val btn = Button("Create Canvas").apply {
                                id = "win-create-btn"
                                onClick = {
                                    submittedName = input.value
                                    header.text = "Created: $submittedName"
                                }
                            }
                            addChild(header)
                            addChild(input)
                            addChild(btn)
                        }
                    }
                    FragmentApplication(initialFragment = fragment, title = "Win32 Visual Test")
                }

                expect(runner.backendName).toContain("windows-win32")
                appCtrl.findById("win-header").assertTextEquals("Windows Visual Test Ready")
                appCtrl.findById("win-input").performTypeText("Artwork_01")
                appCtrl.findById("win-create-btn").performClick()

                expect(submittedName).toBe("Artwork_01")
                appCtrl.findById("win-header").assertTextEquals("Created: Artwork_01")
                expect(Win32NativeWindowHost.visualTestMode).toBe(true)
            }

            it("captures genuine Win32 hidden offscreen DIB PNG screenshot") {
                val appCtrl = launchApp(visualOnWindows = false) {
                    ScreenshotVerificationHelper.buildStyledSampleFragmentApp("Win32 Offscreen PNG")
                }
                val buffer = appCtrl.captureScreenshot(width = 480, height = 320)
                val pngBytes = appCtrl.captureScreenshotPng(width = 480, height = 320)
                ScreenshotVerificationHelper.assertValidNativePngScreenshot(
                    buffer = buffer,
                    pngBytes = pngBytes,
                    expectedW = 480,
                    expectedH = 320,
                    contextLabel = "Windows"
                )

                val tempDir = (getenv("TEMP")?.toKString() ?: getenv("TMP")?.toKString() ?: ".")
                    .replace('\\', '/').trimEnd('/')
                val outPath = "$tempDir/genericui_win32_runner_shot.png"
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
    fun testPlatformDefaultRunnerSelectsWindowsNativeRunner() {
        JestElectricNativeRegistry.resetToDefault()
        val defaultRunner = createPlatformDefaultJestElectricRunner()
        assertNotEquals(DefaultJestElectricRunnerNativeImpl::class, defaultRunner::class)
        assertTrue(defaultRunner is WindowsVisualAppRunnerNativeImpl)
        assertTrue(JestElectricNativeRegistry.current() is WindowsVisualAppRunnerNativeImpl)
        assertEquals(BackendTarget.Windows, defaultRunner.backendTarget)
        assertTrue(GenericUI.activeFactory is WindowsNativeFactory)
    }

    @Test
    fun testVBlankRegistrationAndVisualOnlyInvalidationAndFrameMetrics() {
        val w = DesktopNativeWidget(backendToolkit = "windows-win32", nativeClassName = "canvas")
        var vblankTicks = 0
        val callback: (Double?) -> Unit = { vblankTicks++ }
        w.attributes["vblankDispatch"] = callback
        assertTrue(w in DesktopNativeWidget.activeVBlankWidgetsSnapshot())

        val initialSubtreeVer = w.subtreeMutationVersion
        val initialGlobalVer = DesktopNativeWidget.globalNonOverlayMutationVersion
        w.notifyTreeInvalidated(visualOnly = true)
        assertEquals(initialSubtreeVer, w.subtreeMutationVersion)
        assertEquals(initialGlobalVer, DesktopNativeWidget.globalNonOverlayMutationVersion)

        w.disposed = true
        assertTrue(w !in DesktopNativeWidget.activeVBlankWidgetsSnapshot())

        Win32NativeWindowHost.resetFrameMetrics()
        val snap = Win32NativeWindowHost.getFrameMetricsSnapshot()
        assertEquals(0, snap.totalPaints)
        assertEquals(0, snap.videoStageOnlyPaints)
        assertEquals(0, snap.fullWindowPaints)
    }
}
