package com.nexustools.genericui.tests

import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.shell.Application

/**
 * Receiver scope for individual `test(...)` / `it(...)` blocks inside a `JestElectricRunner` suite.
 *
 * Combines Jest-style `expect(...)` / `expectComponent(...)` matchers with Robolectric-style
 * `launchApp(...)`, `launchFragment(...)`, `pumpMainLooper(...)`, and `advanceTimersByTime(...)`.
 */
class JestTestScope(
    val runnerImpl: JestElectricRunnerNativeImpl,
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
    fun expectComponent(component: Component?): RoboComponentExpectation =
        RoboComponentExpectation(component)

    /**
     * Creates a component expectation from a [RoboNodeInteraction] query result.
     */
    fun expectNode(interaction: RoboNodeInteraction): RoboComponentExpectation =
        RoboComponentExpectation(interaction.component)

    /**
     * Instantiates and launches a GenericUI [Application] (or `FragmentApplication`) via [RoboAppController.setup].
     *
     * On `:desktop:windows` when [visualOnWindows] is `true`, this opens the real Win32 native window
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
     * Advances fragment timers (`setTimeout` / `setInterval`) and CSS transitions by [deltaMs].
     */
    fun advanceTimersByTime(deltaMs: Long) {
        for (fragController in activeFragmentControllers) {
            fragController.idleMainLooper(deltaMs)
        }
        for (appController in activeAppControllers) {
            appController.idleMainLooper(deltaMs)
        }
        runnerImpl.pumpUiEvents(deltaMs)
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
    }
}
