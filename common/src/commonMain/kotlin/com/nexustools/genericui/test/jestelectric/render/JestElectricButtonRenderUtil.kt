package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.action.ButtonGroup
import com.nexustools.genericui.component.action.FloatingActionButton
import com.nexustools.genericui.component.action.IconButton
import com.nexustools.genericui.component.action.SplitDropdownButton
import com.nexustools.genericui.component.action.StyledButton
import com.nexustools.genericui.component.action.ToggleButton
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.ui.Orientation
import com.nexustools.genericui.style.color.RgbaColor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders action button components ([Button], [IconButton], [ToggleButton],
 * [SplitDropdownButton], [FloatingActionButton], [StyledButton], [ButtonGroup]).
 *
 * - On non-web system targets: renders 100% unmodified native OS button chrome when
 *   [BootstrapType.NONE], or offscreen empty-text native button + `ThemeConfig` tint +
 *   contrasting text overlay when `BootstrapType != NONE`, ignoring aesthetic & typography
 *   `ComponentStyle` overrides on standard buttons.
 * - Honors full `ComponentStyle` on [StyledButton], `:web`, or when
 *   [JestElectricRenderContext.isHostedInCustomStyleEngine] is `true`.
 */
object JestElectricButtonRenderUtil {

    fun canRender(comp: AbstractComponent): Boolean =
        comp is Button ||
            comp is SplitDropdownButton ||
            comp is ButtonGroup

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
            is FloatingActionButton -> {
                renderFloatingActionButton(ctx, x, y, w, h, comp)
                true
            }
            is SplitDropdownButton -> {
                renderSplitDropdownButton(ctx, x, y, w, h, comp)
                true
            }
            is ButtonGroup -> {
                renderButtonGroup(ctx, x, y, w, h, comp, depth)
                true
            }
            is Button -> {
                renderButton(ctx, x, y, w, h, comp)
                true
            }
            else -> false
        }
    }

    private fun renderButton(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Button
    ) {
        val bh = h.coerceAtMost(32)
        val by = y + (h - bh) / 2
        val rawLabel = buildButtonLabel(comp)
        val effectiveType = when {
            comp is ToggleButton && comp.isSelected && (comp.type == null || comp.type == BootstrapType.NONE) ->
                BootstrapType.PRIMARY
            else -> comp.type
        }
        val effectiveOutline = when {
            comp is ToggleButton -> !comp.isSelected
            else -> comp.outline
        }
        val isPressed = comp.isActivePressed || (comp is ToggleButton && comp.isSelected)

        if (ctx.allowsFullComponentStyleOnControl(comp)) {
            renderCustomStyledOrTintedButton(
                ctx = ctx,
                x = x,
                y = by,
                w = w,
                h = bh,
                comp = comp,
                effectiveType = effectiveType,
                effectiveOutline = effectiveOutline,
                isPressed = isPressed,
                rawLabel = rawLabel
            )
        } else {
            // Standard system button: 100% unmodified native OS button when BootstrapType.NONE,
            // or offscreen empty-text native button + ThemeConfig tint + contrasting text overlay
            // when BootstrapType != NONE. Ignores aesthetic & typography ComponentStyle overrides.
            JestElectricControlTintRenderUtil.renderTintedNativeControl(
                ctx = ctx,
                x = x,
                y = by,
                w = w,
                h = bh,
                bootstrapType = effectiveType,
                outline = effectiveOutline,
                isSunken = false,
                isHovered = comp.isHovered,
                isPressed = isPressed,
                isFocused = comp.isFocused,
                isEnabled = comp.isEnabled,
                labelText = rawLabel,
                centerText = true,
                textScale = 1
            )
        }
    }

    private fun renderCustomStyledOrTintedButton(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Button,
        effectiveType: BootstrapType?,
        effectiveOutline: Boolean,
        isPressed: Boolean,
        rawLabel: String
    ) {
        val styledBtn = comp as? StyledButton
        val customBg = when {
            !comp.isEnabled && styledBtn?.disabledBackgroundColor != null -> styledBtn.disabledBackgroundColor
            isPressed && styledBtn?.activeBackgroundColor != null -> styledBtn.activeBackgroundColor
            comp.isHovered && styledBtn?.hoverBackgroundColor != null -> styledBtn.hoverBackgroundColor
            comp.isFocused && styledBtn?.focusBackgroundColor != null -> styledBtn.focusBackgroundColor
            else -> comp.style.backgroundColor ?: comp.computedStyle?.backgroundColor
        }
        val customBorder = when {
            !comp.isEnabled && styledBtn?.disabledBorderColor != null -> styledBtn.disabledBorderColor
            isPressed && styledBtn?.activeBorderColor != null -> styledBtn.activeBorderColor
            comp.isHovered && styledBtn?.hoverBorderColor != null -> styledBtn.hoverBorderColor
            comp.isFocused && styledBtn?.focusBorderColor != null -> styledBtn.focusBorderColor
            else -> comp.style.borderColor
                ?: comp.computedStyle?.borderColor
                ?: comp.borderType?.takeIf { it != BootstrapType.NONE }?.let { ctx.resolveBootstrapColor(it) }
        }
        val customFg = when {
            !comp.isEnabled && styledBtn?.disabledTextColor != null -> styledBtn.disabledTextColor
            isPressed && styledBtn?.activeTextColor != null -> styledBtn.activeTextColor
            comp.isHovered && styledBtn?.hoverTextColor != null -> styledBtn.hoverTextColor
            comp.isFocused && styledBtn?.focusTextColor != null -> styledBtn.focusTextColor
            else -> comp.style.color
                ?: comp.computedStyle?.textColor
                ?: comp.textColorType?.takeIf { it != BootstrapType.NONE }?.let { ctx.resolveBootstrapColor(it) }
        }

        if (customBg == null && customBorder == null) {
            // Fall back to native control + BootstrapType tinting while honoring customFg & typography
            val transformed = JestElectricTextRenderUtil.applyTextTransform(rawLabel, comp)
            JestElectricControlTintRenderUtil.renderTintedNativeControl(
                ctx = ctx,
                x = x,
                y = y,
                w = w,
                h = h,
                bootstrapType = effectiveType,
                outline = effectiveOutline,
                isSunken = false,
                isHovered = comp.isHovered,
                isPressed = isPressed,
                isFocused = comp.isFocused,
                isEnabled = comp.isEnabled,
                labelText = transformed,
                centerText = true,
                textScale = JestElectricTextRenderUtil.resolveScale(comp, 1),
                explicitTextColor = customFg
            )
            return
        }

        val fillBg = customBg
            ?: ctx.resolveBootstrapTintOrNull(effectiveType)
            ?: ctx.bgElevated
        val borderCol = customBorder ?: ctx.borderStrong
        val borderThickness = (comp.style.borderWidthPx ?: comp.computedStyle?.borderWidthPx ?: 1f)
            .roundToInt()
            .coerceIn(1, 4)

        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, fillBg)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, borderCol, borderThickness)

        val fg = customFg ?: when {
            !comp.isEnabled -> ctx.textMuted
            effectiveType != null && effectiveType != BootstrapType.NONE -> ctx.resolveBootstrapForeground(effectiveType)
            else -> ctx.textPrimary
        }
        val transformed = JestElectricTextRenderUtil.applyTextTransform(rawLabel, comp)
        val scale = JestElectricTextRenderUtil.resolveScale(comp, 1)
        val bold = JestElectricTextRenderUtil.resolveBold(comp, defaultBold = false)
        val italic = JestElectricTextRenderUtil.resolveItalic(comp)
        val letterSpacing = JestElectricTextRenderUtil.resolveLetterSpacingPx(comp)
        val textW = JestElectricControlTintRenderUtil.measureTextWidth(transformed, scale, letterSpacing)
        val tx = (x + (w - textW) / 2).coerceAtLeast(x + 4)
        val ty = y + (h - 7 * scale) / 2
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = tx,
            y = ty,
            maxW = (w - 8).coerceAtLeast(1),
            text = transformed,
            color = fg,
            scale = scale,
            bold = bold,
            italic = italic,
            letterSpacingPx = letterSpacing
        )
    }

    private fun renderFloatingActionButton(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: FloatingActionButton
    ) {
        val radius = (min(w, h) / 2 - 2).coerceAtLeast(10)
        val cx = x + w / 2
        val cy = y + h / 2
        val bg = ctx.resolveBootstrapColor(comp.type)
        val fg = ctx.resolveBootstrapForeground(comp.type)
        // Subtle drop shadow circle
        JestElectricControlTintRenderUtil.fillCircle(ctx.buffer, cx + 1, cy + 2, radius, ctx.borderSubtle)
        JestElectricControlTintRenderUtil.fillCircle(ctx.buffer, cx, cy, radius, bg)
        JestElectricControlTintRenderUtil.drawCircle(ctx.buffer, cx, cy, radius, ctx.borderStrong)
        val glyph = comp.text.ifBlank { "+" }.take(2)
        val textW = JestElectricControlTintRenderUtil.measureTextWidth(glyph, 1)
        JestElectricControlTintRenderUtil.drawText(ctx.buffer, cx - textW / 2, cy - 3, glyph, fg, 1, bold = true)
    }

    private fun renderSplitDropdownButton(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: SplitDropdownButton
    ) {
        val bh = h.coerceAtMost(32)
        val by = y + (h - bh) / 2
        val caretW = 22.coerceAtMost((w / 3).coerceAtLeast(14))
        val mainW = (w - caretW).coerceAtLeast(12)

        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = x,
            y = by,
            w = mainW,
            h = bh,
            bootstrapType = comp.type,
            outline = comp.outline,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled,
            labelText = comp.label.ifEmpty { "Action" },
            centerText = true
        )
        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = x + mainW,
            y = by,
            w = caretW,
            h = bh,
            bootstrapType = comp.type,
            outline = comp.outline,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled,
            labelText = "▾",
            centerText = true
        )
    }

    private fun renderButtonGroup(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: ButtonGroup,
        depth: Int
    ) {
        val visible = comp.children.filter { it.isVisible }
        if (visible.isEmpty()) return
        if (comp.orientation == Orientation.VERTICAL) {
            val eachH = (h / visible.size).coerceAtLeast(20)
            var cy = y
            for (child in visible) {
                if (cy >= y + h) break
                ctx.drawChild(x, cy, w, eachH.coerceAtMost(y + h - cy), child, depth + 1)
                cy += eachH
            }
        } else {
            val eachW = (w / visible.size).coerceAtLeast(28)
            var cx = x
            for (child in visible) {
                if (cx >= x + w) break
                ctx.drawChild(cx, y, eachW.coerceAtMost(x + w - cx), h, child, depth + 1)
                cx += eachW
            }
        }
    }

    private fun buildButtonLabel(comp: Button): String {
        if (comp.text.isNotEmpty()) return comp.text
        val icon = comp.icon
        if (!icon.isNullOrBlank()) {
            val shortIcon = icon.substringAfterLast('/').substringBeforeLast('.').take(6)
            return if (comp is IconButton) "★" else shortIcon.ifEmpty { "Btn" }
        }
        return if (comp is IconButton) "★" else "Btn"
    }
}
