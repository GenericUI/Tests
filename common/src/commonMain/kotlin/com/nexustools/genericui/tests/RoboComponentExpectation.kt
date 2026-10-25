package com.nexustools.genericui.tests

import com.nexustools.genericui.component.form.CheckBox

/**
 * Jest + Testing-Library style matcher for GenericUI [Component] instances
 * (`expectComponent(node).toBeVisible()`, `toBeEnabled()`, `toHaveText(...)`, `toHaveClass(...)`, etc.).
 */
class RoboComponentExpectation(
    private val component: Component?,
    private val inverted: Boolean = false
) {
    /**
     * Negates subsequent component assertions.
     */
    val not: RoboComponentExpectation
        get() = RoboComponentExpectation(component, !inverted)

    private fun checkCondition(condition: Boolean, positiveMessage: () -> String, negativeMessage: () -> String) {
        if (!inverted && !condition) {
            throw AssertionError(positiveMessage())
        }
        if (inverted && condition) {
            throw AssertionError(negativeMessage())
        }
    }

    private fun requireComponent(): Component =
        component ?: throw AssertionError("Expected component to exist in tree, but it was <null>.")

    fun toExist() {
        checkCondition(
            condition = component != null,
            positiveMessage = { "Expected component to exist, but it was <null>." },
            negativeMessage = { "Expected component not to exist, but found <$component>." }
        )
    }

    fun toBeVisible() {
        val existsAndVisible = component != null && component.isVisible
        checkCondition(
            condition = existsAndVisible,
            positiveMessage = { "Expected component <${component?.id ?: component}> to be visible." },
            negativeMessage = { "Expected component <${component?.id ?: component}> not to be visible." }
        )
    }

    fun toBeHidden() {
        val hidden = component == null || !component.isVisible
        checkCondition(
            condition = hidden,
            positiveMessage = { "Expected component <${component?.id ?: component}> to be hidden." },
            negativeMessage = { "Expected component <${component?.id ?: component}> not to be hidden." }
        )
    }

    fun toBeEnabled() {
        val node = requireComponent()
        checkCondition(
            condition = node.isEnabled,
            positiveMessage = { "Expected component <${node.id ?: node}> to be enabled." },
            negativeMessage = { "Expected component <${node.id ?: node}> not to be enabled." }
        )
    }

    fun toBeDisabled() {
        val node = requireComponent()
        checkCondition(
            condition = !node.isEnabled,
            positiveMessage = { "Expected component <${node.id ?: node}> to be disabled." },
            negativeMessage = { "Expected component <${node.id ?: node}> not to be disabled." }
        )
    }

    fun toHaveText(expected: String) {
        val node = requireComponent()
        val actualText = RoboComponentTreeFinder.extractText(node)
        checkCondition(
            condition = actualText == expected,
            positiveMessage = { "Expected component <${node.id ?: node}> to have text <$expected>, but got <$actualText>." },
            negativeMessage = { "Expected component <${node.id ?: node}> not to have text <$expected>." }
        )
    }

    fun toContainText(substring: String, ignoreCase: Boolean = true) {
        val node = requireComponent()
        val actualText = RoboComponentTreeFinder.extractText(node)
        checkCondition(
            condition = actualText.contains(substring, ignoreCase = ignoreCase),
            positiveMessage = { "Expected component <${node.id ?: node}> text <$actualText> to contain <$substring>." },
            negativeMessage = { "Expected component <${node.id ?: node}> text <$actualText> not to contain <$substring>." }
        )
    }

    fun toHaveClass(cssClass: String) {
        val node = requireComponent()
        checkCondition(
            condition = node.hasClass(cssClass),
            positiveMessage = { "Expected component <${node.id ?: node}> classes <${node.getClass()}> to include <$cssClass>." },
            negativeMessage = { "Expected component <${node.id ?: node}> classes <${node.getClass()}> not to include <$cssClass>." }
        )
    }

    fun toHaveTooltip(expected: String, substring: Boolean = true) {
        val node = requireComponent()
        val tooltip = node.tooltipText ?: ""
        val matches = if (substring) tooltip.contains(expected, ignoreCase = true) else tooltip == expected
        checkCondition(
            condition = matches,
            positiveMessage = { "Expected component <${node.id ?: node}> tooltip <$tooltip> to match <$expected>." },
            negativeMessage = { "Expected component <${node.id ?: node}> tooltip <$tooltip> not to match <$expected>." }
        )
    }

    fun toBeChecked() {
        val node = requireComponent()
        val checked = (node as? CheckBox)?.isChecked == true
        checkCondition(
            condition = checked,
            positiveMessage = { "Expected checkbox/switch component <${node.id ?: node}> to be checked." },
            negativeMessage = { "Expected checkbox/switch component <${node.id ?: node}> not to be checked." }
        )
    }
}
