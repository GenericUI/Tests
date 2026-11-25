package com.nexustools.genericui.tests.ios

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
import com.nexustools.genericui.ios.IosNativeFactory
import com.nexustools.genericui.ipados.IpadosMenuBarBridge
import com.nexustools.genericui.ipados.IpadosNativeFactory
import com.nexustools.genericui.ipados.IpadosWindowImpl
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.tests.ScreenshotVerificationHelper
import com.nexustools.genericui.tests.createPlatformDefaultJestElectricRunner
import com.nexustools.genericui.tests.ipados.IpadosJestElectricBootstrap
import com.nexustools.genericui.tests.ipados.IpadosJestElectricRunnerNativeImpl
import com.nexustools.genericui.window.TitleBar
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Self-tests for Apple UIKit test runners covering BOTH [BackendTarget.IOS] (`IosNativeFactory`)
 * and [BackendTarget.IPadOS] (`IpadosNativeFactory`).
 */
class IosAndIpadosJestElectricRunnerTest {

    @AfterTest
    fun tearDownAppleRunners() {
        IpadosMenuBarBridge.registerAndRebuild(null)
        JestElectricNativeRegistry.resetToDefault()
        GenericUI.activeFactory = null
    }

    @Test
    fun testIosAndIpadosBootstrapsAndMultiTargetRunnerExecution() {
        val iosRunner = IosJestElectricBootstrap.install()
        assertEquals(BackendTarget.IOS, iosRunner.backendTarget)
        assertEquals("ios-uikit", iosRunner.backendName)
        assertEquals(BackendTarget.IOS, GenericUI.activeFactory?.backendTarget)
        assertTrue(GenericUI.activeFactory is IosNativeFactory)
        assertTrue(JestElectricNativeRegistry.forTarget(BackendTarget.IPadOS) is IpadosJestElectricRunnerNativeImpl)

        val ipadosRunner = IpadosJestElectricBootstrap.install()
        assertEquals(BackendTarget.IPadOS, ipadosRunner.backendTarget)
        assertEquals("ipados-uikit", ipadosRunner.backendName)
        assertEquals(BackendTarget.IPadOS, GenericUI.activeFactory?.backendTarget)
        assertTrue(GenericUI.activeFactory is IpadosNativeFactory)
        assertTrue(JestElectricNativeRegistry.forTarget(BackendTarget.IOS) is IosJestElectricRunnerNativeImpl)

        val reports = JestElectricRunner.runSuiteAcrossTargets(
            suiteName = "Apple UIKit Verification",
            targets = listOf(BackendTarget.IOS, BackendTarget.IPadOS),
            visualOnWindows = false
        ) {
            it("mounts FragmentApplication with target-accurate NativeFactory and UI interactions") {
                val activeFactory = GenericUI.requireFactory()
                val isIpados = activeFactory.backendTarget == BackendTarget.IPadOS
                val appCtrl = launchApp(visualOnWindows = false) {
                    val frag = object : Fragment() {
                        override fun onCreate(savedState: FragmentBundle?) {
                            val titleText = Text("Idle").apply { id = "apple-status" }
                            val input = TextField(value = "", placeholder = "Workspace").apply {
                                id = "apple-input"
                            }
                            val applyBtn = Button("Apply").apply {
                                id = "apple-apply-btn"
                                onClick = {
                                    titleText.text = "${activeFactory.backendName}:${input.value}"
                                }
                            }
                            addChild(titleText)
                            addChild(input)
                            addChild(applyBtn)
                        }
                    }
                    FragmentApplication(initialFragment = frag, title = "Apple Target Test").apply {
                        if (isIpados) {
                            mainWindow.setTitleBar(
                                TitleBar().apply {
                                    addLeading(Button("Sidebar").apply { id = "ipados-sidebar-btn" })
                                }
                            )
                            mainWindow.setMenuBar(
                                MenuBar().apply {
                                    addChild(
                                        Menu("File").apply {
                                            addChild(MenuItem("New Window"))
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                appCtrl.findById("apple-input").performTypeText("Canvas")
                appCtrl.findById("apple-apply-btn").performClick()
                appCtrl.findById("apple-status").assertTextEquals("${activeFactory.backendName}:Canvas")

                if (isIpados) {
                    expect(activeFactory.supportsMultipleWindows).toBe(true)
                    expect(activeFactory.supportsSystemMenuBar).toBe(true)
                    expect(appCtrl.app.mainWindow.titleCapabilities).toBe(IpadosWindowImpl.DEFAULT_TITLE_CAPABILITIES)
                    assertTrue(IpadosMenuBarBridge.lastBuiltMenus.isNotEmpty())
                } else {
                    assertFalse(activeFactory.supportsMultipleWindows)
                    assertFalse(activeFactory.supportsSystemMenuBar)
                }
            }

            it("captures genuine UIKit CALayer PNG screenshots on iOS and iPadOS") {
                val activeFactory = GenericUI.requireFactory()
                val appCtrl = launchApp(visualOnWindows = false) {
                    ScreenshotVerificationHelper.buildStyledSampleFragmentApp("UIKit ${activeFactory.backendName}")
                }
                val buffer = appCtrl.captureScreenshot(width = 390, height = 844)
                val pngBytes = appCtrl.captureScreenshotPng(width = 390, height = 844)
                ScreenshotVerificationHelper.assertValidNativePngScreenshot(
                    buffer = buffer,
                    pngBytes = pngBytes,
                    expectedW = 390,
                    expectedH = 844,
                    contextLabel = activeFactory.backendName
                )
            }
        }

        assertEquals(2, reports.size)
        assertTrue(reports.getValue(BackendTarget.IOS).allPassed)
        assertTrue(reports.getValue(BackendTarget.IPadOS).allPassed)
    }

    @Test
    fun testPlatformDefaultRunnerSelectsIosNativeRunner() {
        JestElectricNativeRegistry.resetToDefault()
        val defaultRunner = createPlatformDefaultJestElectricRunner()
        assertNotEquals(DefaultJestElectricRunnerNativeImpl::class, defaultRunner::class)
        assertTrue(defaultRunner is IosJestElectricRunnerNativeImpl)
        assertTrue(JestElectricNativeRegistry.current() is IosJestElectricRunnerNativeImpl)
        assertEquals(BackendTarget.IOS, defaultRunner.backendTarget)
        assertTrue(GenericUI.activeFactory is IosNativeFactory)
    }
}
