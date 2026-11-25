@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nexustools.genericui.tests.linux

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.gtk.GtkNativeFactory
import com.nexustools.genericui.kde.KdeNativeFactory
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.tests.ScreenshotVerificationHelper
import com.nexustools.genericui.tests.createPlatformDefaultJestElectricRunner
import com.nexustools.genericui.tests.gtk.GtkJestElectricBootstrap
import com.nexustools.genericui.tests.gtk.GtkJestElectricRunnerNativeImpl
import com.nexustools.genericui.tests.kde.KdeJestElectricBootstrap
import com.nexustools.genericui.tests.kde.KdeJestElectricRunnerNativeImpl
import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.posix.remove
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Self-tests verifying both GTK4 and KDE Plasma test runners coexisting cleanly in `linuxMain`.
 */
class LinuxGtkAndKdeRunnerTest {

    @AfterTest
    fun tearDownLinuxRunners() {
        JestElectricNativeRegistry.resetToDefault()
        GenericUI.activeFactory = null
    }

    @Test
    fun testGtkAndKdeRunnersSwitchCleanlyWithoutClassCollision() {
        val gtkRunner = GtkJestElectricBootstrap.install()
        assertEquals(BackendTarget.Gtk, gtkRunner.backendTarget)
        assertEquals("linux-gtk4", gtkRunner.backendName)
        assertTrue(GenericUI.activeFactory is GtkNativeFactory)
        assertSame(gtkRunner, GtkJestElectricBootstrap.installIfEnabled())

        val kdeRunner = KdeJestElectricBootstrap.install()
        assertEquals(BackendTarget.Kde, kdeRunner.backendTarget)
        assertEquals("linux-kde", kdeRunner.backendName)
        assertTrue(GenericUI.activeFactory is KdeNativeFactory)
        assertSame(kdeRunner, KdeJestElectricBootstrap.installIfEnabled())
        assertTrue(JestElectricNativeRegistry.forTarget(BackendTarget.Gtk) is GtkJestElectricRunnerNativeImpl)
        assertTrue(JestElectricNativeRegistry.forTarget(BackendTarget.Kde) is KdeJestElectricRunnerNativeImpl)

        val reports = JestElectricRunner.runSuiteAcrossTargets(
            suiteName = "Linux GTK4 + KDE Verification",
            targets = listOf(BackendTarget.Gtk, BackendTarget.Kde),
            visualOnWindows = false,
            perTestTimeoutMs = 15_000L
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

            it("captures genuine native offscreen PNG screenshots on both GTK4 and KDE") {
                val activeTarget = GenericUI.requireFactory().backendTarget
                val appCtrl = launchApp(visualOnWindows = false) {
                    ScreenshotVerificationHelper.buildStyledSampleFragmentApp("Linux ${activeTarget.id} PNG")
                }
                val buffer = appCtrl.captureScreenshot(width = 480, height = 320)
                val pngBytes = appCtrl.captureScreenshotPng(width = 480, height = 320)
                ScreenshotVerificationHelper.assertValidNativePngScreenshot(
                    buffer = buffer,
                    pngBytes = pngBytes,
                    expectedW = 480,
                    expectedH = 320,
                    contextLabel = "Linux-${activeTarget.id}"
                )

                val tmpDir = getenv("TMPDIR")?.toKString()?.trimEnd('/')?.ifBlank { null } ?: "/tmp"
                val outPath = "$tmpDir/genericui_linux_${activeTarget.id}_shot.png"
                try {
                    val saved = appCtrl.takeScreenshotPng(outPath, width = 480, height = 320)
                    assertEquals(outPath, saved)
                } finally {
                    remove(outPath)
                }
            }
        }

        assertEquals(2, reports.size)
        assertTrue(reports.getValue(BackendTarget.Gtk).allPassed)
        assertTrue(reports.getValue(BackendTarget.Kde).allPassed)
    }

    @Test
    fun testPlatformDefaultRunnerSelectsGtkNativeRunner() {
        JestElectricNativeRegistry.resetToDefault()
        val defaultRunner = createPlatformDefaultJestElectricRunner()
        assertNotEquals(DefaultJestElectricRunnerNativeImpl::class, defaultRunner::class)
        assertTrue(defaultRunner is GtkJestElectricRunnerNativeImpl)
        assertTrue(JestElectricNativeRegistry.current() is GtkJestElectricRunnerNativeImpl)
        assertEquals(BackendTarget.Gtk, defaultRunner.backendTarget)
        assertTrue(GenericUI.activeFactory is GtkNativeFactory)
    }
}
