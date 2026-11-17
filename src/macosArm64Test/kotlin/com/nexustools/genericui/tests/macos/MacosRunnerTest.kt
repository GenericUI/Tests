package com.nexustools.genericui.tests.macos

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.macos.MacosNativeFactory
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
        }

        assertTrue(report.allPassed)
    }
}
