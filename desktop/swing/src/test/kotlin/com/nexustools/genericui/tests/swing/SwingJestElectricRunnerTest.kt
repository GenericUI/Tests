package com.nexustools.genericui.tests.swing

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Text
import org.junit.Test
import kotlin.test.assertTrue

/**
 * JUnit 4 self-test for `:tests:desktop:swing` (`SwingJUnitRunnerNativeImpl` & `JestElectricSwingTestBase`).
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
        }

        assertTrue(report.allPassed)
    }
}
