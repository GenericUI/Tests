package com.nexustools.genericui.tests.android

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import com.nexustools.genericui.android.AndroidBaseNativeImpl
import com.nexustools.genericui.android.AndroidNativeFactory
import com.nexustools.genericui.android.AndroidViewHandle
import com.nexustools.genericui.android.AndroidWindowImpl
import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.enums.ui.ThemeMode
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.ImageColorSpace
import com.nexustools.genericui.image.PixelFormat
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.tests.mobile.MobileSharedRunnerNativeImpl
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import java.io.ByteArrayOutputStream
import java.time.Duration

/**
 * Android Robolectric / JUnit runner implementation of `JestElectricRunnerNativeImpl`,
 * powered by [RoboElectricRunner] to execute tests against real Android `View` hierarchies,
 * attached `Activity`/`Window` roots, `Bitmap`/`Canvas` rasterization, and the main Android `Looper`.
 */
class AndroidRobolectricRunnerNativeImpl : MobileSharedRunnerNativeImpl(
    factoryProvider = { AndroidNativeFactory() },
    expectedTarget = BackendTarget.Android
) {
    val usesRoboElectricRunner: Boolean = true

    var viewportWidthPx: Int = 1080
    var viewportHeightPx: Int = 2400

    private var hostedActivityController: ActivityController<Activity>? = null
    private var hostedRootContainer: FrameLayout? = null
    private var hostedWindowView: View? = null

    override val backendTarget: BackendTarget
        get() = BackendTarget.Android

    override val backendName: String
        get() = "android-robolectric"

    private fun invokeOptionalProvider(className: String, methodName: String) {
        runCatching {
            val cls = Class.forName(className)
            val instance = cls.getField("INSTANCE").get(null)
            val method = cls.getMethod(methodName)
            method.invoke(instance)
        }
    }

    override fun ensureBackendInitialized() {
        runCatching { RuntimeEnvironment.getApplication() }.getOrNull()?.let { appCtx ->
            if (AndroidViewHandle.activeContext == null) {
                AndroidViewHandle.activeContext = appCtx
            }
            if (com.nexustools.genericui.android.AndroidPreferenceStoreImpl.applicationContext == null) {
                com.nexustools.genericui.android.AndroidPreferenceStoreImpl.applicationContext = appCtx
            }
        }
        if (GenericUI.activeFactory?.backendTarget != BackendTarget.Android) {
            GenericUI.initialize(AndroidNativeFactory())
        }
        invokeOptionalProvider("com.nexustools.genericui.ext.gpu.mobile.MobileGpuProvider", "install")
        invokeOptionalProvider("com.nexustools.genericui.ext.imageprocessing.context2d.SystemGpuImageBuffer", "installFactory")
        invokeOptionalProvider("com.nexustools.genericui.ext.canvas2d.mobile.MobileCanvas2DProvider", "install")
        invokeOptionalProvider("com.nexustools.genericui.ext.canvas3d.mobile.MobileCanvas3DProvider", "install")
        invokeOptionalProvider("com.nexustools.genericui.android.AndroidMediaProvider", "install")
        invokeOptionalProvider("com.nexustools.genericui.ext.mediacontrols.android.AndroidMediaControlsProvider", "register")
    }

    override fun launchVisualWindowForApp(app: Application, suiteName: String) {
        super.launchVisualWindowForApp(app, suiteName)
        materializeAndroidViewHierarchy(app)
        pumpUiEvents(32L)
    }

    /**
     * Ensures the root Android [View] hierarchy is materialized, attached to a Robolectric
     * [Activity] window when possible, and measured/laid out at `[widthPx] x [heightPx]`.
     */
    fun materializeAndroidViewHierarchy(
        app: Application? = activeApp,
        widthPx: Int = viewportWidthPx,
        heightPx: Int = viewportHeightPx
    ): View? {
        viewportWidthPx = widthPx.coerceAtLeast(1)
        viewportHeightPx = heightPx.coerceAtLeast(1)
        val ctx = AndroidViewHandle.activeContext
            ?: runCatching { RuntimeEnvironment.getApplication() }.getOrNull()?.also {
                AndroidViewHandle.activeContext = it
            }
            ?: return null
        val targetApp = app ?: activeApp ?: return null
        if (targetApp.appNativeImpl == null || targetApp.mainWindow.nativeImpl == null) {
            targetApp.run(GenericUI.requireFactory())
        }
        targetApp.mainWindow.fragmentContainer?.ensureActiveFragmentStartedAndResumed()
        (targetApp as? FragmentApplication)?.rootFragmentContainer?.ensureActiveFragmentStartedAndResumed()
        val windowImpl = targetApp.mainWindow.ensureNativeImpl() as? AndroidWindowImpl ?: return null
        val view = windowImpl.ensureAndroidView(ctx) ?: return null

        val root = hostedRootContainer ?: run {
            val activityCtrl = hostedActivityController ?: runCatching {
                Robolectric.buildActivity(Activity::class.java).setup()
            }.getOrNull()?.also { hostedActivityController = it }
            val hostCtx = activityCtrl?.get() ?: ctx
            FrameLayout(hostCtx).also { container ->
                hostedRootContainer = container
                activityCtrl?.get()?.setContentView(container)
            }
        }

        if (view.parent !== root) {
            (view.parent as? ViewGroup)?.removeView(view)
            root.removeAllViews()
            root.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        hostedWindowView = view
        idleAndroidMainLooper(0L)
        layoutHostedRoot()
        idleAndroidMainLooper(16L)
        return view
    }

    /**
     * Captures the real Android View tree rasterization via [View.draw] into a [Bitmap] when running
     * inside [RoboElectricRunner].
     */
    fun renderHostedAndroidBitmap(
        widthPx: Int = viewportWidthPx,
        heightPx: Int = viewportHeightPx
    ): Bitmap? {
        val root = hostedRootContainer ?: run {
            materializeAndroidViewHierarchy(activeApp, widthPx, heightPx)
            hostedRootContainer
        } ?: return null
        viewportWidthPx = widthPx.coerceAtLeast(1)
        viewportHeightPx = heightPx.coerceAtLeast(1)
        layoutHostedRoot()
        val bmp = Bitmap.createBitmap(viewportWidthPx, viewportHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (root.background == null && hostedWindowView?.background == null) {
            val currentApp = activeApp ?: Application.current
            val isDark = currentApp?.getResolvedTheme() == ThemeMode.DARK
            val bg = currentApp?.getThemeConfig()?.resolveEffectiveColors(isDark)?.background
                ?: if (isDark) RgbaColor(18, 18, 20) else RgbaColor(248, 249, 250)
            canvas.drawColor(Color.argb(255, bg.r, bg.g, bg.b))
        }
        root.draw(canvas)
        return bmp
    }

    override fun captureScreenshot(
        app: Application?,
        width: Int,
        height: Int
    ): ImageBuffer? {
        val bmp = captureAppBitmap(app, width, height) ?: return null
        return try {
            val w = bmp.width
            val h = bmp.height
            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            val rgba = ByteArray(w * h * 4)
            for (i in pixels.indices) {
                val argb = pixels[i]
                val off = i * 4
                rgba[off] = ((argb ushr 16) and 0xFF).toByte()
                rgba[off + 1] = ((argb ushr 8) and 0xFF).toByte()
                rgba[off + 2] = (argb and 0xFF).toByte()
                rgba[off + 3] = ((argb ushr 24) and 0xFF).toByte()
            }
            ImageBuffer(
                width = w,
                height = h,
                pixelFormat = PixelFormat.RGBA_8888,
                colorSpace = ImageColorSpace.SRGB,
                bytes = rgba
            )
        } finally {
            bmp.recycle()
        }
    }

    override fun captureScreenshotPng(
        app: Application?,
        width: Int,
        height: Int
    ): ByteArray? {
        val bmp = captureAppBitmap(app, width, height) ?: return null
        return try {
            val baos = ByteArrayOutputStream()
            if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)) return null
            baos.toByteArray()
        } finally {
            bmp.recycle()
        }
    }

    private fun captureAppBitmap(
        app: Application?,
        width: Int,
        height: Int
    ): Bitmap? {
        val targetApp = prepareAppForScreenshot(app, pumpMs = 0L) ?: return null
        val targetW = if (width > 0) width else viewportWidthPx
        val targetH = if (height > 0) height else viewportHeightPx
        materializeAndroidViewHierarchy(targetApp, targetW, targetH) ?: return null
        pumpUiEvents(16L)
        return renderHostedAndroidBitmap(targetW, targetH)
    }

    private fun layoutHostedRoot() {
        val root = hostedRootContainer ?: return
        val wSpec = View.MeasureSpec.makeMeasureSpec(viewportWidthPx, View.MeasureSpec.EXACTLY)
        val hSpec = View.MeasureSpec.makeMeasureSpec(viewportHeightPx, View.MeasureSpec.EXACTLY)
        root.measure(wSpec, hSpec)
        root.layout(0, 0, viewportWidthPx, viewportHeightPx)
    }

    private fun idleAndroidMainLooper(durationMs: Long) {
        runCatching {
            val looper = Looper.getMainLooper()
            val shadow = Shadows.shadowOf(looper)
            if (durationMs > 0L) {
                shadow.idleFor(Duration.ofMillis(durationMs))
            }
            shadow.idle()
        }
    }

    override fun pumpUiEvents(durationMs: Long) {
        idleAndroidMainLooper(durationMs)
        super.pumpUiEvents(durationMs)
        idleAndroidMainLooper(0L)
        layoutHostedRoot()
        idleAndroidMainLooper(0L)
    }

    override fun performNativeClick(component: AbstractComponent): Boolean {
        if (!component.isEnabled || !component.isVisible) return false
        val baseImpl = component.ensureNativeImpl() as? AndroidBaseNativeImpl
        val view = baseImpl?.nativeWidget?.androidView
        if (baseImpl != null && view != null) {
            AndroidViewHandle.runOnMainThread {
                if (component is CheckBox && view !is CompoundButton) {
                    component.isChecked = !component.isChecked
                }
                if (!view.performClick()) {
                    baseImpl.dispatchClick()
                }
            }
            pumpUiEvents(16L)
            return true
        }
        return super.performNativeClick(component)
    }

    override fun closeVisualWindow() {
        hostedRootContainer?.removeAllViews()
        hostedRootContainer = null
        hostedWindowView = null
        runCatching {
            hostedActivityController?.pause()?.stop()?.destroy()
        }
        hostedActivityController = null
        super.closeVisualWindow()
    }
}
