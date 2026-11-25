package com.nexustools.genericui.tests

import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.event.InputFocusManager
import com.nexustools.genericui.event.PopupMouseCaptureManager

/**
 * Receiver scope for individual `test(...)` / `it(...)` blocks inside a `JestElectricRunner` suite.
 *
 * Combines Jest-style `expect(...)` / `expectComponent(...)` matchers with Robolectric-style
 * `launchApp(...)`, `launchFragment(...)`, `pumpMainLooper(...)`, and `advanceTimersByTime(...)`.
 */
class JestTestScope(
    internal val runnerImpl: JestElectricRunnerNativeImpl,
    val visualOnWindows: Boolean = true
) {
    private val activeAppControllers = mutableListOf<RoboAppController<*>>()
    private val activeFragmentControllers = mutableListOf<RoboFragmentController<*>>()

    /**
     * Creates a Jest-style value expectation (`expect(value).toBe(...)`).
     */
    fun <T> expect(actual: T): JestExpectation<T> = JestExpectation(actual)

    /**
     * Creates a component expectation (`expectComponent(button).toBeVisible()`).
     */
    fun expectComponent(component: AbstractComponent?): RoboComponentExpectation =
        RoboComponentExpectation(component)

    /**
     * Creates a component expectation from a [RoboNodeInteraction] query result.
     */
    fun expectNode(interaction: RoboNodeInteraction): RoboComponentExpectation =
        RoboComponentExpectation(interaction.component)

    /**
     * Instantiates and launches a GenericUI [Application] (or `FragmentApplication`) via [RoboAppController.setup].
     *
     * On Windows when [visualOnWindows] is `true`, this opens the real Win32 native window
     * on screen so the developer can watch the automated interactions execute live.
     */
    fun <A : Application> launchApp(
        visualOnWindows: Boolean = this.visualOnWindows,
        factory: () -> A
    ): RoboAppController<A> {
        val app = factory()
        val controller = RoboAppController(app, runnerImpl, visualWindow = visualOnWindows)
        controller.setup()
        activeAppControllers.add(controller)
        return controller
    }

    /**
     * Instantiates and drives a standalone [Fragment] to the `RESUMED` state via [RoboFragmentController.setup].
     */
    fun <F : Fragment> launchFragment(factory: () -> F): RoboFragmentController<F> {
        val fragment = factory()
        val controller = RoboFragmentController(fragment, runnerImpl)
        controller.setup()
        activeFragmentControllers.add(controller)
        return controller
    }

    /**
     * Pumps pending UI events and advances transitions by [durationMs].
     */
    fun pumpMainLooper(durationMs: Long = 32L) {
        runnerImpl.pumpUiEvents(durationMs)
    }

    /**
     * Advances fragment timers (`setTimeout` / `setInterval`), `UiTimerScheduler`, [Animator],
     * and CSS transitions by [deltaMs].
     */
    fun advanceTimersByTime(deltaMs: Long) {
        for (fragController in activeFragmentControllers) {
            fragController.fragment.flushTimeouts(deltaMs.toInt())
        }
        runnerImpl.pumpUiEvents(deltaMs)
    }

    /**
     * Captures a rendered screenshot of [app] (or the most recently launched application) into an [com.nexustools.genericui.image.ImageBuffer].
     */
    fun captureScreenshot(
        app: Application? = activeAppControllers.lastOrNull()?.app,
        width: Int = 1280,
        height: Int = 800
    ): com.nexustools.genericui.image.ImageBuffer? =
        runnerImpl.captureScreenshot(app, width, height)

    /**
     * Captures a rendered screenshot of [app] (or the most recently launched application) encoded as PNG bytes.
     */
    fun captureScreenshotPng(
        app: Application? = activeAppControllers.lastOrNull()?.app,
        width: Int = 1280,
        height: Int = 800
    ): ByteArray? =
        runnerImpl.captureScreenshotPng(app, width, height)

    /**
     * Captures a rendered screenshot of [app] (or the most recently launched application) and saves it as a
     * PNG file at [fileNameOrPath] (or `<repoRoot>/examples/screenshots/<fileNameOrPath>`).
     */
    fun takeScreenshotPng(
        fileNameOrPath: String,
        app: Application? = activeAppControllers.lastOrNull()?.app,
        width: Int = 1280,
        height: Int = 800
    ): String? =
        runnerImpl.saveScreenshotPng(fileNameOrPath, app, width, height)

    /**
     * Captures a rendered screenshot of [app] (or the most recently launched application) and saves it as a
     * PNG (when `.png`) or Baseline DCT + Huffman JPEG file at [fileNameOrPath] (or `<repoRoot>/examples/screenshots/<fileNameOrPath>`).
     */
    fun takeScreenshot(
        fileNameOrPath: String,
        app: Application? = activeAppControllers.lastOrNull()?.app,
        quality: Int = 90,
        width: Int = 1280,
        height: Int = 800
    ): String? = if (fileNameOrPath.trim().endsWith(".png", ignoreCase = true)) {
        runnerImpl.saveScreenshotPng(fileNameOrPath, app, width, height)
    } else {
        runnerImpl.saveScreenshotJpeg(fileNameOrPath, app, quality, width, height)
    }

    internal fun cleanupTestResources() {
        for (fragController in activeFragmentControllers.asReversed()) {
            runCatching { fragController.destroy() }
        }
        activeFragmentControllers.clear()
        for (appController in activeAppControllers.asReversed()) {
            runCatching { appController.destroy() }
        }
        activeAppControllers.clear()
        PopupMouseCaptureManager.clear()
        InputFocusManager.reset()
    }
}
