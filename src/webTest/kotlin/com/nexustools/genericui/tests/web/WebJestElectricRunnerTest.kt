package com.nexustools.genericui.tests.web

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.web.WebNativeFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Self-test for the Web DOM runner (`WebJestElectricRunnerNativeImpl` and `WebJestElectricBootstrap`).
 */
class WebJestElectricRunnerTest {

    @AfterTest
    fun tearDownWebRunner() {
        JestElectricNativeRegistry.resetToDefault()
        GenericUI.activeFactory = null
    }

    @Test
    fun testWebRunnerExecutesFragmentSuite() {
        val runner = WebJestElectricBootstrap.install()
        assertEquals(BackendTarget.Web, runner.backendTarget)
        assertEquals("web-dom", runner.backendName)
        assertTrue(GenericUI.activeFactory is WebNativeFactory)

        val report = JestElectricRunner.describe(
            suiteName = "Web DOM Runner Verification",
            visualOnWindows = false,
            runnerImpl = runner
        ) {
            it("mounts FragmentApplication with WebNativeFactory and dispatches click") {
                val appCtrl = launchApp(visualOnWindows = false) {
                    val frag = object : Fragment() {
                        override fun onCreate(savedState: FragmentBundle?) {
                            val label = Text("Web Ready").apply { id = "web-status" }
                            val btn = Button("Go").apply {
                                id = "web-btn"
                                onClick = { label.text = "Web Clicked" }
                            }
                            addChild(label)
                            addChild(btn)
                        }
                    }
                    FragmentApplication(initialFragment = frag, title = "Web Runner Test")
                }

                appCtrl.findById("web-status").assertTextEquals("Web Ready")
                appCtrl.findById("web-btn").performClick()
                appCtrl.findById("web-status").assertTextEquals("Web Clicked")
            }
        }

        assertTrue(report.allPassed)
    }
}
