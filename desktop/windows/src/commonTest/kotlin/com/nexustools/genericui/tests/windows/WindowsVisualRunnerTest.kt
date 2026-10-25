package com.nexustools.genericui.tests.windows

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.windows.Win32NativeWindowHost
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Self-tests for `:tests:desktop:windows` (`WindowsVisualAppRunnerNativeImpl` and `WindowsJestElectricBootstrap`).
 */
class WindowsVisualRunnerTest {

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
        }

        assertTrue(report.allPassed)
    }
}
