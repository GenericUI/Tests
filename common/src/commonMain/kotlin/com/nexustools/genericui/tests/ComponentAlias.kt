package com.nexustools.genericui.tests

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.core.AbstractComponent

/**
 * Convenience typealias mapping [Component] to [AbstractComponent] for test ergonomics.
 */
typealias Component = AbstractComponent

/**
 * Convenience typealias mapping [Checkbox] to [CheckBox] for test ergonomics.
 */
typealias Checkbox = CheckBox

/**
 * Programmatically triggers a click on this [Button] if it is enabled.
 */
fun Button.click() {
    if (isEnabled) {
        onClick?.invoke()
    }
}
