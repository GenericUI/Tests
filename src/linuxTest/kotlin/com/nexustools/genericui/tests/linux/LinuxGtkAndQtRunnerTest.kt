package com.nexustools.genericui.tests.linux

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.gtk.GtkNativeFactory
import com.nexustools.genericui.qt.QtNativeFactory
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.tests.gtk.GtkJestElectricBootstrap
import com.nexustools.genericui.tests.qt.QtJestElectricBootstrap
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Self-tests verifying both GTK4 and Qt6 test runners coexisting cleanly in `linuxMain`.
 */
class LinuxGtkAndQtRunnerTest {

    @AfterTest
    fun tearDownLinuxRunners() {
        JestElectricNativeRegistry.resetToDefault()
        GenericUI.activeFactory = null
    }

    @Test
    fun testGtkAndQtRunnersSwitchCleanlyWithoutClassCollision() {
        val gtkRunner = GtkJestElectricBootstrap.install()
        assertEquals(BackendTarget.Gtk, gtkRunner.backendTarget)
        assertEquals("linux-gtk4", gtkRunner.backendName)
        assertTrue(GenericUI.activeFactory is GtkNativeFactory)

        val qtRunner = QtJestElectricBootstrap.install()
        assertEquals(BackendTarget.Qt, qtRunner.backendTarget)
        assertEquals("linux-qt6", qtRunner.backendName)
        assertTrue(GenericUI.activeFactory is QtNativeFactory)

        val reports = JestElectricRunner.runSuiteAcrossTargets(
            suiteName = "Linux GTK4 + Qt6 Verification",
            targets = listOf(BackendTarget.Gtk, BackendTarget.Qt),
            visualOnWindows = false
        ) {
            it("initializes matching Linux NativeFactory and updates component state") {
                val activeTarget = GenericUI.requireFactory().backendTarget
                val appCtrl = launchApp(visualOnWindows = false) {
                    val frag = object : Fragment() {
                        override fun onCreate(savedState: FragmentBundle?) {
                            val label = Text("Init").apply { id = "linux-label" }
                            val btn = Button("Ping").apply {
                                id = "linux-btn"
                                onClick = { label.text = "Pong:${activeTarget.id}" }
                            }
                            addChild(label)
                            addChild(btn)
                        }
                    }
                    FragmentApplication(initialFragment = frag, title = "Linux Test")
                }

                appCtrl.findById("linux-btn").performClick()
                appCtrl.findById("linux-label").assertTextEquals("Pong:${activeTarget.id}")
            }
        }

        assertEquals(2, reports.size)
        assertTrue(reports.getValue(BackendTarget.Gtk).allPassed)
        assertTrue(reports.getValue(BackendTarget.Qt).allPassed)
    }
}
