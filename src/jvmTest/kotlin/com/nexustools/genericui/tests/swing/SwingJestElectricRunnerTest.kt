package com.nexustools.genericui.tests.swing

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.shell.Menu
import com.nexustools.genericui.component.shell.MenuBar
import com.nexustools.genericui.component.shell.MenuItem
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.image.ImageFormat
import com.nexustools.genericui.image.codec.LibPngCodec
import com.nexustools.genericui.swing.SwingNativeFactory
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.ScreenshotVerificationHelper
import com.nexustools.genericui.tests.createPlatformDefaultJestElectricRunner
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * JUnit 4 self-test for Java Swing (`SwingJUnitRunnerNativeImpl` & `JestElectricSwingTestBase`).
 */
class SwingJestElectricRunnerTest : JestElectricSwingTestBase(headless = true) {

    @Test
    fun testSwingBackendWithJestElectricRunner() {
        val report = describeSwingSuite("Swing JUnit JestElectricRunner Suite") {
            it("mounts SwingNativeFactory application and dispatches UI actions") {
                val appCtrl = launchApp(visualOnWindows = false) {
                    val frag = object : Fragment() {
                        override fun onCreate(savedState: FragmentBundle?) {
                            val label = Text("Swing Idle").apply {
                                id = "swing-status"
                            }
                            val field = TextField(value = "Initial").apply {
                                id = "swing-field"
                            }
                            val btn = Button("Apply").apply {
                                id = "swing-apply-btn"
                                onClick = {
                                    label.text = "Applied: ${field.value}"
                                }
                            }
                            addChild(label)
                            addChild(field)
                            addChild(btn)
                        }
                    }
                    FragmentApplication(initialFragment = frag, title = "Swing Test App")
                }

                expect(swingRunnerImpl.backendName).toBe("swing-junit")
                appCtrl.findById("swing-status").assertTextEquals("Swing Idle")
                appCtrl.findById("swing-field").performTypeText("UpdatedSwingValue")
                appCtrl.findById("swing-apply-btn").performClick()
                appCtrl.findById("swing-status").assertTextEquals("Applied: UpdatedSwingValue")
            }

            it("captures genuine Swing headless offscreen screenshots and valid PNG files") {
                val appCtrl = launchApp(visualOnWindows = false) {
                    ScreenshotVerificationHelper.buildStyledSampleFragmentApp("Swing Headless PNG").apply {
                        val bar = MenuBar().apply {
                            addChild(
                                Menu("File").apply {
                                    addChild(MenuItem("Export"))
                                }
                            )
                        }
                        mainWindow.setMenuBar(bar)
                    }
                }

                val buffer = appCtrl.captureScreenshot(width = 480, height = 320)
                val pngBytes = appCtrl.captureScreenshotPng(width = 480, height = 320)
                ScreenshotVerificationHelper.assertValidNativePngScreenshot(
                    buffer = buffer,
                    pngBytes = pngBytes,
                    expectedW = 480,
                    expectedH = 320,
                    contextLabel = "Swing"
                )

                val tmpFile = Files.createTempFile("swing-jest-shot-", ".png").toFile()
                try {
                    val savedPath = appCtrl.takeScreenshot(tmpFile.absolutePath, width = 480, height = 320)
                    assertEquals(tmpFile.absolutePath.replace('\\', '/'), savedPath)
                    val diskBytes = File(savedPath!!).readBytes()
                    assertTrue(ScreenshotVerificationHelper.hasPngMagicHeader(diskBytes))
                    val diskMeta = LibPngCodec.probePngMetadata(diskBytes)
                    assertEquals(ImageFormat.PNG, diskMeta.format)
                    assertEquals(480, diskMeta.width)
                    assertEquals(320, diskMeta.height)
                } finally {
                    tmpFile.delete()
                }
            }
        }

        assertTrue(report.allPassed)
    }

    @Test
    fun testPlatformDefaultRunnerSelectsSwingNativeRunner() {
        JestElectricNativeRegistry.resetToDefault()
        val defaultRunner = createPlatformDefaultJestElectricRunner()
        assertNotEquals(DefaultJestElectricRunnerNativeImpl::class, defaultRunner::class)
        assertTrue(defaultRunner is SwingJUnitRunnerNativeImpl)
        assertTrue(JestElectricNativeRegistry.current() is SwingJUnitRunnerNativeImpl)
        assertEquals(BackendTarget.Swing, defaultRunner.backendTarget)
        assertTrue(GenericUI.activeFactory is SwingNativeFactory)
        assertNotNull(GenericUI.activeFactory)
    }
}
