package com.nexustools.genericui.tests

import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.component.form.Select
import com.nexustools.genericui.component.form.Slider

/**
 * Robolectric / Compose-Testing style interaction handle for a queried GenericUI [Component].
 *
 * Supports fluent assertions (`assertExists()`, `assertIsVisible()`, `assertTextEquals(...)`, etc.)
 * and simulated user actions (`performClick()`, `performHover()`, `performFocus()`, `performTypeText(...)`).
 */
class RoboNodeInteraction(
    val component: Component?,
    private val runnerImpl: JestElectricRunnerNativeImpl,
    private val selectorDescription: String
) {
    private fun requireNode(): Component =
        component ?: throw AssertionError("No component matched selector: $selectorDescription")

    fun assertExists(): RoboNodeInteraction {
        requireNode()
        return this
    }

    fun assertDoesNotExist(): RoboNodeInteraction {
        if (component != null) {
            throw AssertionError("Expected no component for selector [$selectorDescription], but found <$component>.")
        }
        return this
    }

    fun assertIsVisible(): RoboNodeInteraction {
        RoboComponentExpectation(requireNode()).toBeVisible()
        return this
    }

    fun assertIsHidden(): RoboNodeInteraction {
        RoboComponentExpectation(component).toBeHidden()
        return this
    }

    fun assertIsEnabled(): RoboNodeInteraction {
        RoboComponentExpectation(requireNode()).toBeEnabled()
        return this
    }

    fun assertIsDisabled(): RoboNodeInteraction {
        RoboComponentExpectation(requireNode()).toBeDisabled()
        return this
    }

    fun assertTextEquals(expected: String): RoboNodeInteraction {
        RoboComponentExpectation(requireNode()).toHaveText(expected)
        return this
    }

    fun assertTextContains(substring: String, ignoreCase: Boolean = true): RoboNodeInteraction {
        RoboComponentExpectation(requireNode()).toContainText(substring, ignoreCase)
        return this
    }

    fun assertHasClass(cssClass: String): RoboNodeInteraction {
        RoboComponentExpectation(requireNode()).toHaveClass(cssClass)
        return this
    }

    fun assertHasTooltip(expected: String, substring: Boolean = true): RoboNodeInteraction {
        RoboComponentExpectation(requireNode()).toHaveTooltip(expected, substring)
        return this
    }

    fun performClick(): RoboNodeInteraction {
        val node = requireNode()
        if (!runnerImpl.performNativeClick(node)) {
            throw AssertionError("Failed to performClick() on disabled or hidden component [$selectorDescription].")
        }
        return this
    }

    fun performHover(): RoboNodeInteraction {
        val node = requireNode()
        if (!runnerImpl.performNativeHover(node)) {
            throw AssertionError("Failed to performHover() on hidden component [$selectorDescription].")
        }
        return this
    }

    fun performFocus(): RoboNodeInteraction {
        val node = requireNode()
        if (!runnerImpl.performNativeFocus(node)) {
            throw AssertionError("Failed to performFocus() on disabled or hidden component [$selectorDescription].")
        }
        return this
    }

    fun performTypeText(text: String): RoboNodeInteraction {
        val node = requireNode()
        if (!runnerImpl.performNativeTypeText(node, text)) {
            throw AssertionError("Failed to performTypeText('$text') on component [$selectorDescription].")
        }
        return this
    }

    fun performSelectValue(value: String): RoboNodeInteraction {
        val node = requireNode()
        val select = node as? Select
            ?: throw AssertionError("Component [$selectorDescription] is not a Select widget.")
        if (!select.isEnabled || !select.isVisible) {
            throw AssertionError("Cannot select value on disabled or hidden Select [$selectorDescription].")
        }
        select.selectedValue = value
        select.onSelectionChanged?.invoke(value)
        runnerImpl.pumpUiEvents(16L)
        return this
    }

    fun performSetChecked(checked: Boolean): RoboNodeInteraction {
        val node = requireNode()
        val checkBox = node as? CheckBox
            ?: throw AssertionError("Component [$selectorDescription] is not a CheckBox or Switch widget.")
        if (!checkBox.isEnabled || !checkBox.isVisible) {
            throw AssertionError("Cannot change checked state on disabled or hidden CheckBox [$selectorDescription].")
        }
        checkBox.isChecked = checked
        runnerImpl.pumpUiEvents(16L)
        return this
    }

    fun performSetSliderValue(value: Double): RoboNodeInteraction {
        val node = requireNode()
        val slider = node as? Slider
            ?: throw AssertionError("Component [$selectorDescription] is not a Slider or RangeSlider widget.")
        if (!slider.isEnabled || !slider.isVisible) {
            throw AssertionError("Cannot change slider value on disabled or hidden Slider [$selectorDescription].")
        }
        slider.value = value
        slider.onValueChanged?.invoke(slider.value)
        runnerImpl.pumpUiEvents(16L)
        return this
    }
}
