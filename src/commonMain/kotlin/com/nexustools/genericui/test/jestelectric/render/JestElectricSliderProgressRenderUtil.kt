package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.feedback.ProgressBar
import com.nexustools.genericui.component.feedback.Spinner
import com.nexustools.genericui.component.form.RangeSlider
import com.nexustools.genericui.component.form.Slider
import com.nexustools.genericui.component.form.StyledSlider
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.style.theme.ThemeConfig
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders slider and progress controls ([Slider], [StyledSlider], [RangeSlider], [ProgressBar], [Spinner])
 * using native OS track/bar chrome or styled visual specs while strictly ignoring [BootstrapType] on
 * non-web system targets.
 */
object JestElectricSliderProgressRenderUtil {

    fun canRender(comp: AbstractComponent): Boolean =
        comp is StyledSlider ||
            comp is RangeSlider ||
            comp is Slider ||
            comp is ProgressBar ||
            comp is Spinner

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
            is StyledSlider -> {
                renderStyledSlider(ctx, x, y, w, h, comp)
                true
            }
            is RangeSlider -> {
                renderRangeSlider(ctx, x, y, w, h, comp)
                true
            }
            is Slider -> {
                renderSlider(ctx, x, y, w, h, comp)
                true
            }
            is ProgressBar -> {
                renderProgressBar(ctx, x, y, w, h, comp)
                true
            }
            is Spinner -> {
                renderSpinner(ctx, x, y, w, h, comp)
                true
            }
            else -> false
        }
    }

    private fun renderStyledSlider(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: StyledSlider
    ) {
        val spec = comp.resolveVisualSpec()
        JestElectricContainerChromeRenderUtil.renderStyledVisualSurface(
            ctx = ctx,
            x = x,
            y = y,
            w = w,
            h = h,
            comp = comp,
            spec = spec,
            allowBootstrapTint = ctx.allowsBootstrapTypeOnComponent(comp)
        )
        val trackH = 6
        val ty = y + (h - trackH) / 2
        val trackW = (w - 8).coerceAtLeast(8)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 4, ty, trackW, trackH, ctx.borderSubtle)
        val pct = comp.normalizedFraction.toFloat().coerceIn(0f, 1f)
        val fillW = (trackW * pct).roundToInt()
        val allowBs = ctx.allowsBootstrapTypeOnComponent(comp)
        val fillColor = if (allowBs && comp.type != BootstrapType.NONE) {
            ctx.resolveBootstrapColor(comp.type)
        } else {
            ctx.accent
        }
        if (fillW > 0) {
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 4, ty, fillW, trackH, fillColor)
        }
        val knobSize = 12.coerceAtMost(h)
        val knobX = (x + 4 + fillW - knobSize / 2).coerceIn(x + 2, (x + w - knobSize - 2).coerceAtLeast(x + 2))
        val knobY = y + (h - knobSize) / 2
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, knobX, knobY, knobSize, knobSize, ctx.bgElevated)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, knobX, knobY, knobSize, knobSize, ctx.borderStrong, 1)
    }

    private fun renderSlider(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Slider
    ) {
        val trackH = 6
        val ty = y + (h - trackH) / 2
        val trackW = (w - 8).coerceAtLeast(8)

        // Native sunken track
        val trackOffscreen = JestElectricControlTintRenderUtil.renderNativeControlSpriteOffscreen(
            w = trackW,
            h = trackH,
            isDark = ctx.isDark,
            isSunken = true,
            isEnabled = comp.isEnabled
        )
        JestElectricControlTintRenderUtil.blitOffscreenBuffer(
            target = ctx.buffer,
            dstX = x + 4,
            dstY = ty,
            offscreen = trackOffscreen,
            clipX = ctx.clipX,
            clipY = ctx.clipY,
            clipW = ctx.clipW,
            clipH = ctx.clipH
        )

        val span = (comp.max - comp.min).takeIf { abs(it) > 1e-6 } ?: 100.0
        val pct = ((comp.value - comp.min) / span).toFloat().coerceIn(0f, 1f)
        val fillW = (trackW * pct).roundToInt()
        val fillColor = ctx.accent

        if (fillW > 0) {
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 4, ty + 1, fillW, trackH - 2, fillColor)
        }

        // Native thumb button ignoring BootstrapType
        val thumbW = 10
        val thumbH = 14.coerceAtMost(h)
        val thumbX = (x + 4 + fillW - thumbW / 2).coerceIn(x + 2, (x + w - thumbW - 2).coerceAtLeast(x + 2))
        val thumbY = y + (h - thumbH) / 2
        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = thumbX,
            y = thumbY,
            w = thumbW,
            h = thumbH,
            bootstrapType = BootstrapType.NONE,
            isHovered = comp.isHovered,
            isPressed = comp.isActivePressed,
            isFocused = comp.isFocused,
            isEnabled = comp.isEnabled
        )
    }

    private fun renderRangeSlider(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: RangeSlider
    ) {
        val trackH = 6
        val ty = y + (h - trackH) / 2
        val trackW = (w - 8).coerceAtLeast(8)

        val trackOffscreen = JestElectricControlTintRenderUtil.renderNativeControlSpriteOffscreen(
            w = trackW,
            h = trackH,
            isDark = ctx.isDark,
            isSunken = true,
            isEnabled = comp.isEnabled
        )
        JestElectricControlTintRenderUtil.blitOffscreenBuffer(
            target = ctx.buffer,
            dstX = x + 4,
            dstY = ty,
            offscreen = trackOffscreen,
            clipX = ctx.clipX,
            clipY = ctx.clipY,
            clipW = ctx.clipW,
            clipH = ctx.clipH
        )

        val span = (comp.max - comp.min).takeIf { abs(it) > 1e-6 } ?: 100.0
        val p0 = ((comp.value - comp.min) / span).toFloat().coerceIn(0f, 1f)
        val p1 = ((comp.upperValue - comp.min) / span).toFloat().coerceIn(p0, 1f)
        val x0 = x + 4 + (trackW * p0).roundToInt()
        val x1 = x + 4 + (trackW * p1).roundToInt()
        val fillColor = ctx.accent

        JestElectricControlTintRenderUtil.fillRect(
            ctx.buffer,
            x0,
            ty + 1,
            (x1 - x0).coerceAtLeast(4),
            trackH - 2,
            fillColor
        )
        val thumbW = 8
        val thumbH = 14.coerceAtMost(h)
        val thumbY = y + (h - thumbH) / 2
        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = x0 - thumbW / 2,
            y = thumbY,
            w = thumbW,
            h = thumbH,
            bootstrapType = BootstrapType.NONE,
            isEnabled = comp.isEnabled
        )
        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = x1 - thumbW / 2,
            y = thumbY,
            w = thumbW,
            h = thumbH,
            bootstrapType = BootstrapType.NONE,
            isEnabled = comp.isEnabled
        )
    }

    private fun renderProgressBar(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: ProgressBar
    ) {
        val bh = h.coerceIn(10, 22)
        val by = y + (h - bh) / 2

        // Native sunken progress trough
        val troughOffscreen = JestElectricControlTintRenderUtil.renderNativeControlSpriteOffscreen(
            w = w,
            h = bh,
            isDark = ctx.isDark,
            isSunken = true,
            isEnabled = comp.isEnabled
        )
        JestElectricControlTintRenderUtil.blitOffscreenBuffer(
            target = ctx.buffer,
            dstX = x,
            dstY = by,
            offscreen = troughOffscreen,
            clipX = ctx.clipX,
            clipY = ctx.clipY,
            clipW = ctx.clipW,
            clipH = ctx.clipH
        )

        val rawProgress = if (comp.progress > 1f) comp.progress / 100f else comp.progress
        val pct = if (comp.isIndeterminate || comp.isBusy) 0.6f else rawProgress.coerceIn(0f, 1f)
        val fillW = ((w - 2) * pct).roundToInt()
        val systemFillColor = ctx.accent
        if (fillW > 0) {
            val fillOffscreen = JestElectricControlTintRenderUtil.renderNativeControlSpriteOffscreen(
                w = fillW,
                h = (bh - 2).coerceAtLeast(2),
                isDark = ctx.isDark,
                isSunken = false,
                isEnabled = comp.isEnabled
            )
            JestElectricControlTintRenderUtil.tintOffscreenControlBuffer(
                offscreen = fillOffscreen,
                tintColor = systemFillColor,
                isDark = ctx.isDark,
                outline = false,
                isEnabled = comp.isEnabled
            )
            JestElectricControlTintRenderUtil.blitOffscreenBuffer(
                target = ctx.buffer,
                dstX = x + 1,
                dstY = by + 1,
                offscreen = fillOffscreen,
                clipX = ctx.clipX,
                clipY = ctx.clipY,
                clipW = ctx.clipW,
                clipH = ctx.clipH
            )
        }
        val lbl = comp.label ?: "${(pct * 100).roundToInt()}%"
        if (lbl.isNotEmpty() && bh >= 14) {
            val fg = if (pct >= 0.5f) ThemeConfig.autoContrastForeground(systemFillColor) else ctx.textPrimary
            val tw = JestElectricControlTintRenderUtil.measureTextWidth(lbl, 1)
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = x + (w - tw).coerceAtLeast(4) / 2,
                y = by + (bh - 7) / 2,
                maxW = (w - 8).coerceAtLeast(1),
                text = lbl,
                color = fg,
                scale = 1
            )
        }
    }

    private fun renderSpinner(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Spinner
    ) {
        val r = (min(w, h) / 2 - 2).coerceIn(5, 10)
        val cx = x + r + 4
        val cy = y + h / 2
        val spinColor = ctx.accent
        JestElectricControlTintRenderUtil.drawCircle(ctx.buffer, cx, cy, r, ctx.borderSubtle)
        // Active spinner arc (270 degrees)
        for (deg in 0 until 270 step 4) {
            val rad = deg * PI / 180.0
            val px = cx + (r * cos(rad)).roundToInt()
            val py = cy + (r * sin(rad)).roundToInt()
            ctx.buffer.setPixel(px, py, spinColor)
        }
        val lbl = comp.label
        if (!lbl.isNullOrEmpty()) {
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = cx + r + 6,
                y = y + (h - 7) / 2,
                maxW = (w - (r * 2 + 12)).coerceAtLeast(1),
                text = lbl,
                color = ctx.textSecondary,
                scale = 1
            )
        }
    }
}
