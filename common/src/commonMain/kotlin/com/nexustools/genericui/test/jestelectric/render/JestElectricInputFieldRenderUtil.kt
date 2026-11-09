package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.form.ColorPicker
import com.nexustools.genericui.component.form.DatePicker
import com.nexustools.genericui.component.form.NumberField
import com.nexustools.genericui.component.form.PasswordField
import com.nexustools.genericui.component.form.SearchField
import com.nexustools.genericui.component.form.TextArea
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.form.TimePicker
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.enums.bootstrap.BootstrapType

/**
 * Renders form input field controls ([TextField], [PasswordField], [SearchField],
 * [NumberField], [TextArea], [ColorPicker], [DatePicker], [TimePicker]) using a native OS
 * sunken input frame + [BootstrapType] tinting via [JestElectricControlTintRenderUtil],
 * ignoring aesthetic `ComponentStyle` overrides on system targets.
 */
object JestElectricInputFieldRenderUtil {

    fun canRender(comp: AbstractComponent): Boolean =
        comp is TextField || comp is TextArea

    fun render(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: AbstractComponent
    ): Boolean {
        if (w <= 0 || h <= 0) return true
        return when (comp) {
            is ColorPicker -> {
                renderColorPicker(ctx, x, y, w, h, comp)
                true
            }
            is DatePicker -> {
                renderDateOrTimePicker(ctx, x, y, w, h, comp, badgeText = "📅")
                true
            }
            is TimePicker -> {
                renderDateOrTimePicker(ctx, x, y, w, h, comp, badgeText = "⏱")
                true
            }
            is NumberField -> {
                renderNumberField(ctx, x, y, w, h, comp)
                true
            }
            is SearchField -> {
                renderSearchField(ctx, x, y, w, h, comp)
                true
            }
            is PasswordField -> {
                renderTextField(ctx, x, y, w, h, comp)
                true
            }
            is TextField -> {
                renderTextField(ctx, x, y, w, h, comp)
                true
            }
            is TextArea -> {
                renderTextArea(ctx, x, y, w, h, comp)
                true
            }
            else -> false
        }
    }

    private fun renderTextField(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: TextField
    ) {
        val fh = h.coerceAtMost(28)
        val fy = y + (h - fh) / 2
        val effectiveType = comp.borderType?.takeIf { it != BootstrapType.NONE }
            ?: comp.type.takeIf { it != BootstrapType.NONE }

        val offscreen = JestElectricControlTintRenderUtil.renderNativeControlSpriteOffscreen(
            w = w,
            h = fh,
            isDark = ctx.isDark,
            isSunken = true,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled
        )
        if (effectiveType != null) {
            JestElectricControlTintRenderUtil.tintOffscreenControlBuffer(
                offscreen = offscreen,
                tintColor = ctx.resolveBootstrapColor(effectiveType),
                isDark = ctx.isDark,
                outline = true,
                isHovered = comp.isHovered,
                isPressed = comp.isActivePressed,
                isEnabled = comp.isEnabled
            )
        }
        JestElectricControlTintRenderUtil.blitOffscreenBuffer(
            target = ctx.buffer,
            dstX = x,
            dstY = fy,
            offscreen = offscreen,
            clipX = ctx.clipX,
            clipY = ctx.clipY,
            clipW = ctx.clipW,
            clipH = ctx.clipH
        )

        val display = when {
            comp is PasswordField && comp.value.isNotEmpty() -> "•".repeat(comp.value.length.coerceAtMost(24))
            comp.value.isNotEmpty() -> comp.value
            comp.placeholder.isNotEmpty() -> comp.placeholder
            else -> ""
        }
        val fg = when {
            !comp.isEnabled -> ctx.textMuted
            comp.value.isNotEmpty() -> ctx.textPrimary
            else -> ctx.textMuted
        }
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = x + 8,
            y = fy + (fh - 7) / 2,
            maxW = (w - 16).coerceAtLeast(1),
            text = display,
            color = fg,
            scale = 1
        )
    }

    private fun renderSearchField(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: SearchField
    ) {
        val fh = h.coerceAtMost(28)
        val fy = y + (h - fh) / 2
        renderTextField(ctx, x, y, w, h, comp)
        // Draw search magnifier icon on the right side
        if (w >= 40) {
            val cx = x + w - 12
            val cy = fy + fh / 2 - 1
            JestElectricControlTintRenderUtil.drawCircle(ctx.buffer, cx, cy, 3, ctx.textMuted)
            JestElectricControlTintRenderUtil.drawLine(ctx.buffer, cx + 2, cy + 2, cx + 5, cy + 5, ctx.textMuted)
        }
    }

    private fun renderNumberField(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: NumberField
    ) {
        val fh = h.coerceAtMost(28)
        val fy = y + (h - fh) / 2
        val spinW = 16.coerceAtMost((w / 4).coerceAtLeast(12))
        val inputW = (w - spinW).coerceAtLeast(12)
        renderTextField(ctx, x, y, inputW, h, comp)
        // Native up/down stepper buttons on the right
        val halfH = (fh / 2).coerceAtLeast(6)
        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = x + inputW,
            y = fy,
            w = spinW,
            h = halfH,
            bootstrapType = comp.type,
            labelText = "^",
            centerText = true
        )
        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = x + inputW,
            y = fy + halfH,
            w = spinW,
            h = fh - halfH,
            bootstrapType = comp.type,
            labelText = "▾",
            centerText = true
        )
    }

    private fun renderColorPicker(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: ColorPicker
    ) {
        val fh = h.coerceAtMost(28)
        val fy = y + (h - fh) / 2
        renderTextField(ctx, x, y, w, h, comp)
        // Render live color swatch preview inside the input frame
        val swatchW = 18.coerceAtMost((w - 8).coerceAtLeast(8))
        val swatchH = (fh - 8).coerceAtLeast(8)
        val sx = (x + w - swatchW - 5).coerceAtLeast(x + 4)
        val sy = fy + (fh - swatchH) / 2
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, sx, sy, swatchW, swatchH, comp.selectedColor)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, sx, sy, swatchW, swatchH, ctx.borderStrong, 1)
    }

    private fun renderDateOrTimePicker(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: TextField,
        badgeText: String
    ) {
        val fh = h.coerceAtMost(28)
        val fy = y + (h - fh) / 2
        renderTextField(ctx, x, y, w, h, comp)
        if (w >= 48) {
            val iconX = x + w - 16
            JestElectricControlTintRenderUtil.drawVLine(ctx.buffer, iconX - 4, fy + 3, fy + fh - 3, ctx.borderSubtle)
            JestElectricControlTintRenderUtil.drawText(
                ctx.buffer,
                iconX,
                fy + (fh - 7) / 2,
                if (badgeText == "📅") "#" else ":",
                ctx.textSecondary,
                1
            )
        }
    }

    private fun renderTextArea(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: TextArea
    ) {
        val effectiveType = comp.borderType?.takeIf { it != BootstrapType.NONE }
            ?: comp.type.takeIf { it != BootstrapType.NONE }

        val offscreen = JestElectricControlTintRenderUtil.renderNativeControlSpriteOffscreen(
            w = w,
            h = h,
            isDark = ctx.isDark,
            isSunken = true,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled
        )
        if (effectiveType != null) {
            JestElectricControlTintRenderUtil.tintOffscreenControlBuffer(
                offscreen = offscreen,
                tintColor = ctx.resolveBootstrapColor(effectiveType),
                isDark = ctx.isDark,
                outline = true,
                isHovered = comp.isHovered,
                isPressed = comp.isActivePressed,
                isEnabled = comp.isEnabled
            )
        }
        JestElectricControlTintRenderUtil.blitOffscreenBuffer(
            target = ctx.buffer,
            dstX = x,
            dstY = y,
            offscreen = offscreen,
            clipX = ctx.clipX,
            clipY = ctx.clipY,
            clipW = ctx.clipW,
            clipH = ctx.clipH
        )

        val display = comp.value.ifEmpty { comp.placeholder }
        val fg = if (comp.value.isNotEmpty()) ctx.textPrimary else ctx.textMuted
        JestElectricControlTintRenderUtil.drawWrappedText(
            buf = ctx.buffer,
            x = x + 8,
            y = y + 8,
            maxW = (w - 16).coerceAtLeast(1),
            maxH = (h - 16).coerceAtLeast(8),
            text = display,
            color = fg,
            scale = 1
        )
    }
}
