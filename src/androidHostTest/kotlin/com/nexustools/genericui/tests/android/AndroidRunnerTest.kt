package com.nexustools.genericui.tests.android

import com.nexustools.genericui.android.AndroidNativeFactory
import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.action.StyledButton
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.layout.StyledContainer
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.style.css.CssFilter
import com.nexustools.genericui.style.css.CssLength
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Host JUnit 4 self-test for the Android runner (`RoboElectricRunner` + `AndroidRobolectricRunnerNativeImpl` + `JestElectricRobolectricTestBase`).
 */
@RunWith(RoboElectricRunner::class)
class AndroidRunnerTest : JestElectricRobolectricTestBase() {

    @Test
    fun testAndroidBackendWithJestElectricRunner() {
        assertTrue(androidRunnerImpl.usesRoboElectricRunner)
        assertEquals(BackendTarget.Android, androidRunnerImpl.backendTarget)
        assertEquals("android-robolectric", androidRunnerImpl.backendName)
        assertTrue(GenericUI.activeFactory is AndroidNativeFactory)
        assertNotNull(RuntimeEnvironment.getApplication())
        assertTrue(JestElectricNativeRegistry.current() is AndroidRobolectricRunnerNativeImpl)

        val report = describeAndroidSuite("Android Host JestElectricRunner Suite") {
            it("mounts FragmentApplication with AndroidNativeFactory, real Android Views, and drives interactions") {
                var overlayRef: StyledContainer? = null
                var buttonRef: StyledButton? = null
                var fragRef: Fragment? = null
                val appCtrl = launchApp(visualOnWindows = false) {
                    val frag = object : Fragment() {
                        override fun onCreate(savedState: FragmentBundle?) {
                            fragRef = this
                            val label = Text("Android Idle").apply { id = "android-status" }
                            val input = TextField(value = "").apply { id = "android-input" }
                            val btn = Button("Submit").apply {
                                id = "android-submit-btn"
                                onClick = { label.text = "Android:${input.value}" }
                            }
                            val glassOverlay = StyledContainer().apply {
                                id = "android-glass-overlay"
                                backgroundColor = RgbaColor(18, 18, 22, 160)
                                setBackdropBlur(6)
                            }
                            val styledBtn = StyledButton("No Blur").apply {
                                id = "android-styled-btn"
                                getStyle().setBackdropFilter(CssFilter.blur(CssLength.px(6)))
                            }
                            overlayRef = glassOverlay
                            buttonRef = styledBtn
                            addChild(label)
                            addChild(input)
                            addChild(btn)
                            addChild(glassOverlay)
                            addChild(styledBtn)
                        }
                    }
                    FragmentApplication(initialFragment = frag, title = "Android Host Test")
                }

                val rootView = androidRunnerImpl.materializeAndroidViewHierarchy(appCtrl.app)
                assertNotNull(rootView)
                assertTrue(rootView.isAttachedToWindow)
                assertTrue((androidRunnerImpl.renderHostedAndroidBitmap()?.width ?: 0) > 0)

                val overlaySpec = overlayRef!!.resolveVisualSpec()
                assertEquals(6f, overlaySpec.backdropBlurPx)
                val styledBtnImpl = buttonRef!!.ensureNativeImpl() as? com.nexustools.genericui.android.AndroidStyledButtonImpl
                assertNotNull(styledBtnImpl)
                assertEquals(6f, styledBtnImpl.resolveCurrentVisuals().backdropBlurPx)

                appCtrl.findById("android-input").performTypeText("Material")
                appCtrl.findById("android-submit-btn").performClick()
                appCtrl.findById("android-status").assertTextEquals("Android:Material")

                val thumbImg = com.nexustools.genericui.component.media.Image(
                    src = "http://127.0.0.1:1/nonexistent.png"
                ).apply {
                    widthPx = 36
                    heightPx = 36
                }
                fragRef!!.addChild(thumbImg)
                val thumbImpl = thumbImg.ensureNativeImpl() as com.nexustools.genericui.android.AndroidImageImpl
                val (decodeW, decodeH) = thumbImpl.getResolvedDecodeTargetDimensions()
                assertTrue(
                    decodeW in 64..256 && decodeH in 64..256,
                    "Expected compact thumbnail decode dimensions <= 256, got ${decodeW}x${decodeH}"
                )
                fragRef!!.removeChild(thumbImg)
                thumbImg.dispose()

                val dbImpl = com.nexustools.genericui.android.AndroidDatabaseNativeImpl(
                    databaseName = "runner_test_db",
                    filePath = ":memory:",
                    context = RuntimeEnvironment.getApplication()
                )
                try {
                    val schema = com.nexustools.genericui.util.db.DatabaseTableSchema(
                        tableName = "test_items",
                        columns = listOf(
                            com.nexustools.genericui.util.db.DatabaseColumnSpec(
                                "itemType",
                                com.nexustools.genericui.util.db.DatabaseColumnType.STRING
                            ),
                            com.nexustools.genericui.util.db.DatabaseColumnSpec(
                                "count",
                                com.nexustools.genericui.util.db.DatabaseColumnType.INT64
                            )
                        )
                    )
                    kotlinx.coroutines.runBlocking {
                        assertTrue(dbImpl.defineTable(schema), "First defineTable call should succeed")
                        assertTrue(dbImpl.defineTable(schema), "Repeated defineTable call should succeed cleanly")
                        val inserted = dbImpl.putRecord(
                            "test_items",
                            com.nexustools.genericui.util.db.DatabaseRecord(key = "k1")
                                .withString("itemType", "show")
                                .withLong("count", 42L)
                        )
                        assertTrue(inserted, "putRecord should succeed after defineTable")
                        val rows = dbImpl.query(
                            com.nexustools.genericui.util.db.DatabaseQuery.from("test_items")
                        )
                        assertEquals(1, rows.size)
                        assertEquals("show", rows.first().getString("itemType"))
                        assertEquals(42L, rows.first().getLong("count"))
                    }
                } finally {
                    dbImpl.dispose()
                }
            }

            it("scales unpremultiplied translucent PNGs in AndroidBitmapImageCodecImpl without falling back to full resolution") {
                val codec = com.nexustools.genericui.android.AndroidBitmapImageCodecImpl(RuntimeEnvironment.getApplication())
                val srcW = 256
                val srcH = 256
                val srcBytes = ByteArray(srcW * srcH * 4)
                var i = 0
                while (i < srcBytes.size) {
                    srcBytes[i] = 200.toByte()
                    srcBytes[i + 1] = 100.toByte()
                    srcBytes[i + 2] = 50.toByte()
                    srcBytes[i + 3] = 128.toByte()
                    i += 4
                }
                val srcBuf = com.nexustools.genericui.image.ImageBuffer(
                    width = srcW,
                    height = srcH,
                    pixelFormat = com.nexustools.genericui.image.PixelFormat.RGBA_8888,
                    colorSpace = com.nexustools.genericui.image.ImageColorSpace.SRGB,
                    bytes = srcBytes
                )
                val pngBytes = codec.encode(
                    srcBuf,
                    com.nexustools.genericui.image.ImageEncodeOptions(
                        format = com.nexustools.genericui.image.ImageFormat.PNG
                    )
                )
                assertNotNull(pngBytes, "PNG encoding should produce non-empty bytes")
                val decoded = codec.decode(
                    com.nexustools.genericui.image.ImageSource.fromBytes(pngBytes),
                    com.nexustools.genericui.image.ImageDecodeOptions(
                        targetWidth = 32,
                        targetHeight = 32,
                        premultiplyAlpha = false
                    )
                )
                assertNotNull(decoded, "Decoded scaled translucent PNG should not be null")
                assertEquals(32, decoded.width, "Expected scaled width 32 rather than unscaled 256")
                assertEquals(32, decoded.height, "Expected scaled height 32 rather than unscaled 256")
            }
        }

        assertTrue(report.allPassed)

        // Verify resetToDefault() automatically selects AndroidRobolectricRunnerNativeImpl on :android
        JestElectricNativeRegistry.resetToDefault()
        val autoRunner = JestElectricNativeRegistry.current()
        assertTrue(autoRunner is AndroidRobolectricRunnerNativeImpl)
        val autoReport = JestElectricRunner.runSuite("Auto RoboElectricRunner Dispatch") {
            it("automatically uses RoboElectricRunner backend on Android") {
                expect(JestElectricNativeRegistry.current().backendTarget).toBe(BackendTarget.Android)
            }
        }
        assertTrue(autoReport.allPassed)
    }
}
