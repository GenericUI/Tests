package com.nexustools.genericui.tests

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentContainer
import com.nexustools.genericui.component.shell.Window
import com.nexustools.genericui.core.NativeFactory
import com.nexustools.genericui.enums.fragment.FragmentState

/**
 * Robolectric-inspired standalone lifecycle and UI interaction controller for a GenericUI [Fragment].
 *
 * Drives the exact Android Activity-like lifecycle (`INITIALIZED` -> `CREATED` -> `STARTED` ->
 * `RESUMED` -> `PAUSED` -> `STOPPED` -> `DESTROYED`) and provides component tree queries.
 */
class RoboFragmentController<F : Fragment>(
    val fragment: F,
    private val runnerImpl: JestElectricRunnerNativeImpl
) {
    private var autoResumeFactoryEnabled: Boolean = false
    private val hostWindow: Window = Window(title = "FragmentTestWindow")
    private val hostContainer: FragmentContainer = object : FragmentContainer(null, hostWindow) {
        override fun resolveActiveFactory(): NativeFactory? =
            if (autoResumeFactoryEnabled) HeadlessLifecycleMarkerFactory else null
    }

    init {
        hostWindow.setContent(hostContainer)
    }

    fun create(): RoboFragmentController<F> {
        runnerImpl.ensureBackendInitialized()
        if (fragment.state == FragmentState.INITIALIZED || fragment.state == FragmentState.DESTROYED) {
            autoResumeFactoryEnabled = false
            hostContainer.setFragment(fragment)
        }
        return this
    }

    fun start(): RoboFragmentController<F> {
        if (fragment.state == FragmentState.INITIALIZED || fragment.state == FragmentState.DESTROYED) {
            create()
        }
        if (fragment.state == FragmentState.CREATED || fragment.state == FragmentState.STOPPED) {
            fragment.onStart()
        }
        return this
    }

    fun resume(): RoboFragmentController<F> {
        if (fragment.state == FragmentState.INITIALIZED || fragment.state == FragmentState.DESTROYED) {
            create()
        }
        if (fragment.state != FragmentState.RESUMED) {
            autoResumeFactoryEnabled = true
            hostContainer.removeChild(fragment)
            hostContainer.replaceFragment(fragment)
        }
        runnerImpl.pumpUiEvents(16L)
        return this
    }

    /**
     * Drives the fragment through `create()` -> `start()` -> `resume()`.
     */
    fun setup(): RoboFragmentController<F> = create().start().resume()

    fun pause(): RoboFragmentController<F> {
        if (fragment.state == FragmentState.RESUMED) {
            fragment.onPause()
        }
        return this
    }

    fun stop(): RoboFragmentController<F> {
        if (fragment.state == FragmentState.RESUMED || fragment.state == FragmentState.STARTED || fragment.state == FragmentState.PAUSED) {
            autoResumeFactoryEnabled = false
            val placeholder = object : Fragment() {}
            hostContainer.pushFragment(placeholder)
        }
        return this
    }

    fun destroy(): RoboFragmentController<F> {
        autoResumeFactoryEnabled = false
        val placeholder = object : Fragment() {}
        hostContainer.setFragment(placeholder)
        return this
    }

    /**
     * Flushes fragment timers (`setTimeout` / `setInterval`) and advances transitions by [durationMs].
     */
    fun idleMainLooper(durationMs: Long = 32L): RoboFragmentController<F> {
        fragment.flushTimeouts(durationMs.toInt())
        fragment.stepTransitions(durationMs.toFloat())
        runnerImpl.pumpUiEvents(durationMs)
        return this
    }

    fun findById(id: String): RoboNodeInteraction {
        val node = findAllComponents().firstOrNull { it.id == id }
        return RoboNodeInteraction(node, runnerImpl, "id='$id'")
    }

    fun findByText(
        text: String,
        substring: Boolean = false,
        ignoreCase: Boolean = true
    ): RoboNodeInteraction {
        val node = findAllComponents().firstOrNull { comp ->
            val extracted = RoboComponentTreeFinder.extractText(comp)
            if (extracted.isEmpty()) {
                false
            } else if (substring) {
                extracted.contains(text, ignoreCase = ignoreCase)
            } else {
                extracted.equals(text, ignoreCase = ignoreCase)
            }
        }
        return RoboNodeInteraction(node, runnerImpl, "text='$text' (substring=$substring)")
    }

    fun findByClass(cssClass: String): RoboNodeInteraction {
        val node = findAllComponents().firstOrNull { it.hasClass(cssClass) }
        return RoboNodeInteraction(node, runnerImpl, "class='$cssClass'")
    }

    fun findByTooltip(tooltip: String, substring: Boolean = true): RoboNodeInteraction {
        val node = findAllComponents().firstOrNull { comp ->
            val t = comp.tooltipText ?: return@firstOrNull false
            if (substring) t.contains(tooltip, ignoreCase = true) else t == tooltip
        }
        return RoboNodeInteraction(node, runnerImpl, "tooltip='$tooltip'")
    }

    fun findAllButtons(): List<Button> =
        findAllComponents().filterIsInstance<Button>()

    fun findAllComponents(): List<Component> =
        RoboComponentTreeFinder.collectAllComponents(fragment)
}
