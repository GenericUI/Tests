package com.nexustools.genericui.tests

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.shell.Application

/**
 * Robolectric-inspired lifecycle and UI interaction controller for a GenericUI [Application]
 * or `FragmentApplication` (`create()`, `start()`, `resume()`, `visible()`, `setup()`,
 * `pause()`, `stop()`, `destroy()`, `findById(...)`, `findByText(...)`, `clickMenu(...)`).
 */
class RoboAppController<A : Application>(
    val app: A,
    private val runnerImpl: JestElectricRunnerNativeImpl,
    private val visualWindow: Boolean = true
) {
    private var launched: Boolean = false

    fun create(): RoboAppController<A> {
        runnerImpl.ensureBackendInitialized()
        return this
    }

    fun start(): RoboAppController<A> {
        if (!launched) {
            runnerImpl.launchVisualWindowForApp(app, app.mainWindow.getTitle())
            launched = true
        }
        return this
    }

    fun resume(): RoboAppController<A> {
        if (!launched) {
            start()
        }
        runnerImpl.pumpUiEvents(16L)
        return this
    }

    fun visible(): RoboAppController<A> {
        if (!launched) {
            start()
        }
        runnerImpl.pumpUiEvents(32L)
        return this
    }

    /**
     * Drives the application through `create()` -> `start()` -> `resume()` -> `visible()`.
     */
    fun setup(): RoboAppController<A> = create().start().resume().visible()

    fun pause(): RoboAppController<A> {
        runnerImpl.pumpUiEvents(16L)
        return this
    }

    fun stop(): RoboAppController<A> {
        runnerImpl.pumpUiEvents(16L)
        return this
    }

    fun destroy(): RoboAppController<A> {
        runnerImpl.closeVisualWindow()
        app.dispose()
        launched = false
        return this
    }

    /**
     * Advances the UI event loop and CSS/component transitions by [durationMs].
     */
    fun idleMainLooper(durationMs: Long = 32L): RoboAppController<A> {
        runnerImpl.pumpUiEvents(durationMs)
        return this
    }

    /**
     * Queries the application component tree for a node with `id == id`.
     */
    fun findById(id: String): RoboNodeInteraction {
        val node = findAllComponents().firstOrNull { it.id == id }
        return RoboNodeInteraction(node, runnerImpl, "id='$id'")
    }

    /**
     * Queries the application component tree for a node whose text matches [text].
     */
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

    /**
     * Queries the application component tree for the first component possessing [cssClass].
     */
    fun findByClass(cssClass: String): RoboNodeInteraction {
        val node = findAllComponents().firstOrNull { it.hasClass(cssClass) }
        return RoboNodeInteraction(node, runnerImpl, "class='$cssClass'")
    }

    /**
     * Queries the application component tree for the first component whose `tooltipText` matches [tooltip].
     */
    fun findByTooltip(tooltip: String, substring: Boolean = true): RoboNodeInteraction {
        val node = findAllComponents().firstOrNull { comp ->
            val t = comp.tooltipText ?: return@firstOrNull false
            if (substring) t.contains(tooltip, ignoreCase = true) else t == tooltip
        }
        return RoboNodeInteraction(node, runnerImpl, "tooltip='$tooltip'")
    }

    /**
     * Returns all [Button] instances currently mounted in the application tree.
     */
    fun findAllButtons(): List<Button> =
        findAllComponents().filterIsInstance<Button>()

    /**
     * Returns all [Component] instances currently reachable in the application tree.
     */
    fun findAllComponents(): List<Component> =
        RoboComponentTreeFinder.collectFromApplication(app)

    /**
     * Invokes a menu item matching [menuTitle] -> [itemLabel] in the application's menu bar.
     */
    fun clickMenu(menuTitle: String, itemLabel: String): RoboAppController<A> {
        if (!runnerImpl.performNativeMenuClick(app, menuTitle, itemLabel)) {
            throw AssertionError("Menu item '$menuTitle -> $itemLabel' was not found or is disabled.")
        }
        return this
    }
}
