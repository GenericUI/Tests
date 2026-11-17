package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.action.DropdownButton
import com.nexustools.genericui.component.action.SplitDropdownButton
import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.component.form.ComboBox
import com.nexustools.genericui.component.form.RadioButton
import com.nexustools.genericui.component.form.RadioGroup
import com.nexustools.genericui.component.form.Select
import com.nexustools.genericui.component.form.Switch
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.style.color.RgbaColor

/**
 * Renders toggle and selection controls ([CheckBox], [RadioButton], [RadioGroup],
 * [Switch], [Select], [ComboBox], [DropdownButton]) using native OS control chrome
 * + [BootstrapType] tinting via [JestElectricControlTintRenderUtil], ignoring
 * aesthetic `ComponentStyle` overrides on system targets.
 */
object JestElectricToggleSelectionRenderUtil {

    fun canRender(comp: AbstractComponent): Boolean =
        comp is Switch ||
            comp is CheckBox ||
            comp is RadioButton ||
            comp is RadioGroup ||
            comp is ComboBox ||
            comp is Select ||
            (comp is DropdownButton && comp !is SplitDropdownButton)

    fun render(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: AbstractComponent,
        depth: Int = 0
    ): Boolean {
        if (w <= 0 || h <= 0) return true
        return when (comp) {
            is Switch -> {
                renderSwitch(ctx, x, y, w, h, comp)
                true
            }
            is CheckBox -> {
                renderCheckBox(ctx, x, y, w, h, comp)
                true
            }
            is RadioButton -> {
                renderRadioButton(ctx, x, y, w, h, comp)
                true
            }
            is RadioGroup -> {
                renderRadioGroup(ctx, x, y, w, h, comp, depth)
                true
            }
            is ComboBox -> {
                renderComboBox(ctx, x, y, w, h, comp)
                true
            }
            is Select -> {
                renderSelect(ctx, x, y, w, h, comp)
                true
            }
            is DropdownButton -> {
                renderDropdownButton(ctx, x, y, w, h, comp)
                true
            }
            else -> false
        }
    }

    private fun renderCheckBox(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: CheckBox
    ) {
        val boxSize = 14
        val by = y + (h - boxSize) / 2
        val activeType = if (comp.isChecked) {
            comp.type.takeIf { it != BootstrapType.NONE } ?: BootstrapType.PRIMARY
        } else {
            BootstrapType.NONE
        }

        // Render empty-text native checkbox box into offscreen buffer, tint if checked, and blit
        val offscreen = JestElectricControlTintRenderUtil.renderNativeControlSpriteOffscreen(
            w = boxSize,
            h = boxSize,
            isDark = ctx.isDark,
            isSunken = !comp.isChecked,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled,
            cornerRadius = 2
        )
        if (comp.isChecked) {
            val tint = ctx.resolveBootstrapColor(activeType)
            JestElectricControlTintRenderUtil.tintOffscreenControlBuffer(
                offscreen = offscreen,
                tintColor = tint,
                isDark = ctx.isDark,
                outline = false,
                isHovered = comp.isHovered,
                isPressed = comp.isActivePressed,
                isEnabled = comp.isEnabled
            )
        }
        JestElectricControlTintRenderUtil.blitOffscreenBuffer(
            target = ctx.buffer,
            dstX = x + 2,
            dstY = by,
            offscreen = offscreen,
            clipX = ctx.clipX,
            clipY = ctx.clipY,
            clipW = ctx.clipW,
            clipH = ctx.clipH
        )

        if (comp.isChecked) {
            val markFg = ctx.resolveBootstrapForeground(activeType)
            JestElectricControlTintRenderUtil.drawLine(ctx.buffer, x + 5, by + 7, x + 8, by + 10, markFg)
            JestElectricControlTintRenderUtil.drawLine(ctx.buffer, x + 8, by + 10, x + 13, by + 4, markFg)
        }

        val labelFg = if (comp.isEnabled) ctx.textPrimary else ctx.textMuted
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = x + 22,
            y = y + (h - 7) / 2,
            maxW = (w - 24).coerceAtLeast(1),
            text = comp.label,
            color = labelFg,
            scale = 1
        )
    }

    private fun renderSwitch(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Switch
    ) {
        val sw = 28
        val sh = 14
        val sy = y + (h - sh) / 2
        val activeType = if (comp.isChecked) {
            comp.type.takeIf { it != BootstrapType.NONE } ?: BootstrapType.PRIMARY
        } else {
            BootstrapType.NONE
        }

        val offscreen = JestElectricControlTintRenderUtil.renderNativeControlSpriteOffscreen(
            w = sw,
            h = sh,
            isDark = ctx.isDark,
            isSunken = !comp.isChecked,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled,
            cornerRadius = 6
        )
        if (comp.isChecked) {
            JestElectricControlTintRenderUtil.tintOffscreenControlBuffer(
                offscreen = offscreen,
                tintColor = ctx.resolveBootstrapColor(activeType),
                isDark = ctx.isDark,
                outline = false,
                isHovered = comp.isHovered,
                isPressed = comp.isActivePressed,
                isEnabled = comp.isEnabled
            )
        }
        JestElectricControlTintRenderUtil.blitOffscreenBuffer(
            target = ctx.buffer,
            dstX = x + 2,
            dstY = sy,
            offscreen = offscreen,
            clipX = ctx.clipX,
            clipY = ctx.clipY,
            clipW = ctx.clipW,
            clipH = ctx.clipH
        )

        val knobX = if (comp.isChecked) x + 2 + sw - 12 else x + 4
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, knobX, sy + 2, 10, sh - 4, RgbaColor.WHITE)
        val labelFg = if (comp.isEnabled) ctx.textPrimary else ctx.textMuted
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = x + 36,
            y = y + (h - 7) / 2,
            maxW = (w - 38).coerceAtLeast(1),
            text = comp.label,
            color = labelFg,
            scale = 1
        )
    }

    private fun renderRadioButton(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: RadioButton
    ) {
        val cy = y + h / 2
        val cx = x + 9
        val activeColor = comp.type.takeIf { it != BootstrapType.NONE }?.let { ctx.resolveBootstrapColor(it) }
            ?: ctx.accent
        JestElectricControlTintRenderUtil.fillCircle(ctx.buffer, cx, cy, 6, ctx.bgInput)
        JestElectricControlTintRenderUtil.drawCircle(
            ctx.buffer,
            cx,
            cy,
            6,
            if (comp.isChecked) activeColor else ctx.borderStrong
        )
        if (comp.isChecked) {
            JestElectricControlTintRenderUtil.fillCircle(ctx.buffer, cx, cy, 3, activeColor)
        }
        val labelFg = if (comp.isEnabled) ctx.textPrimary else ctx.textMuted
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = x + 22,
            y = y + (h - 7) / 2,
            maxW = (w - 24).coerceAtLeast(1),
            text = comp.label,
            color = labelFg,
            scale = 1
        )
    }

    private fun renderRadioGroup(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: RadioGroup,
        depth: Int
    ) {
        val visible = comp.children.filter { it.isVisible }
        if (visible.isEmpty()) return
        var cy = y
        val eachH = (h / visible.size.coerceAtLeast(1)).coerceIn(18, 28)
        for (child in visible) {
            if (cy + 14 > y + h) break
            ctx.drawChild(x, cy, w, eachH, child, depth + 1)
            cy += eachH + 4
        }
    }

    private fun renderSelect(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Select
    ) {
        val sh = h.coerceAtMost(28)
        val sy = y + (h - sh) / 2
        val selectedLabel = comp.options.firstOrNull { it.value == comp.selectedValue }?.label
            ?: comp.selectedValue
            ?: comp.options.firstOrNull()?.label
            ?: "Select..."

        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = x,
            y = sy,
            w = w,
            h = sh,
            bootstrapType = comp.type,
            outline = true,
            isSunken = false,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled,
            labelText = selectedLabel,
            centerText = false,
            textPaddingLeft = 8
        )
        // Dropdown caret button on right side
        val caretX = (x + w - 18).coerceAtLeast(x + 4)
        JestElectricControlTintRenderUtil.drawVLine(ctx.buffer, caretX - 3, sy + 3, sy + sh - 3, ctx.borderSubtle)
        JestElectricControlTintRenderUtil.drawText(
            ctx.buffer,
            caretX,
            sy + (sh - 7) / 2,
            "▾",
            ctx.textSecondary,
            1
        )
    }

    private fun renderComboBox(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: ComboBox
    ) {
        val sh = h.coerceAtMost(28)
        val sy = y + (h - sh) / 2
        val selectedLabel = comp.options.firstOrNull { it.value == comp.selectedValue }?.label
            ?: comp.selectedValue
            ?: comp.options.firstOrNull()?.label
            ?: "ComboBox..."

        // Editable sunken input portion on the left + native button caret on the right
        val btnW = 20.coerceAtMost((w / 3).coerceAtLeast(14))
        val inputW = (w - btnW).coerceAtLeast(12)
        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = x,
            y = sy,
            w = inputW,
            h = sh,
            bootstrapType = comp.type,
            outline = true,
            isSunken = true,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled,
            labelText = selectedLabel,
            centerText = false,
            textPaddingLeft = 8
        )
        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = x + inputW,
            y = sy,
            w = btnW,
            h = sh,
            bootstrapType = comp.type,
            outline = false,
            isSunken = false,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled,
            labelText = "▾",
            centerText = true
        )
    }

    private fun renderDropdownButton(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: DropdownButton
    ) {
        val bh = h.coerceAtMost(30)
        val by = y + (h - bh) / 2
        val textWithCaret = "${comp.label.ifEmpty { "Dropdown" }} ▾"
        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = x,
            y = by,
            w = w,
            h = bh,
            bootstrapType = comp.type,
            outline = comp.outline,
            isSunken = false,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled,
            labelText = textWithCaret,
            centerText = true
        )
    }
}
