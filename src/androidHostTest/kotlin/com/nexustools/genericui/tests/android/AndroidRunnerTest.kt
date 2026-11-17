package com.nexustools.genericui.tests.android

import com.nexustools.genericui.android.AndroidNativeFactory
import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Host JUnit 4 self-test for the Android runner (`AndroidRobolectricRunnerNativeImpl` and `JestElectricRobolectricTestBase`).
 */
class AndroidRunnerTest : JestElectricRobolectricTestBase() {

    @Test
    fun testAndroidBackendWithJestElectricRunner() {
        assertEquals(BackendTarget.Android, androidRunnerImpl.backendTarget)
        assertEquals("android-robolectric", androidRunnerImpl.backendName)
        assertTrue(GenericUI.activeFactory is AndroidNativeFactory)

        val report = describeAndroidSuite("Android Host JestElectricRunner Suite") {
            it("mounts FragmentApplication with AndroidNativeFactory and drives interactions") {
                val appCtrl = launchApp(visualOnWindows = false) {
                    val frag = object : Fragment() {
                        override fun onCreate(savedState: FragmentBundle?) {
                            val label = Text("Android Idle").apply { id = "android-status" }
                            val input = TextField(value = "").apply { id = "android-input" }
                            val btn = Button("Submit").apply {
                                id = "android-submit-btn"
                                onClick = { label.text = "Android:${input.value}" }
                            }
                            addChild(label)
                            addChild(input)
                            addChild(btn)
                        }
                    }
                    FragmentApplication(initialFragment = frag, title = "Android Host Test")
                }

                appCtrl.findById("android-input").performTypeText("Material")
                appCtrl.findById("android-submit-btn").performClick()
                appCtrl.findById("android-status").assertTextEquals("Android:Material")
            }
        }

        assertTrue(report.allPassed)
    }
}
