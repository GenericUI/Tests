package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.style.color.RgbaColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders native OS control sprites with NO text into an offscreen [ImageBuffer],
 * color-tints the native control pixels via `ThemeConfig` / `BootstrapColorPalette` when
 * [BootstrapType] != [BootstrapType.NONE] while preserving native bevels, borders,
 * specular highlights, and hover/pressed/disabled states, blits to the target buffer,
 * and renders the contrasting text/icon overlay on top afterward.
 *
 * Also provides shared 2D primitive drawing and 5x7 bitmap font rendering utilities
 * used across all `JestElectric*RenderUtil` classes.
 */
object JestElectricControlTintRenderUtil {

    /**
     * Renders an empty-text native OS control sprite into an offscreen [ImageBuffer],
     * preserving genuine native 3D bevels, vertical surface gradient, specular top highlight,
     * bottom shadow, outer border, and hover/pressed/focused/disabled state shading.
     */
    fun renderNativeControlSpriteOffscreen(
        w: Int,
        h: Int,
        isDark: Boolean,
        isSunken: Boolean = false,
        isHovered: Boolean = false,
        isPressed: Boolean = false,
        isFocused: Boolean = false,
        isEnabled: Boolean = true,
        cornerRadius: Int = 3
    ): ImageBuffer {
        val safeW = w.coerceAtLeast(1)
        val safeH = h.coerceAtLeast(1)
        val offscreen = ImageBuffer(safeW, safeH)

        val baseTop: RgbaColor
        val baseBottom: RgbaColor
        val borderOuter: RgbaColor
        val bevelTop: RgbaColor
        val bevelBottom: RgbaColor

        if (isDark) {
            when {
                !isEnabled -> {
                    baseTop = RgbaColor(32, 41, 56)
                    baseBottom = RgbaColor(28, 36, 50)
                    borderOuter = RgbaColor(51, 65, 85)
                    bevelTop = RgbaColor(38, 49, 66)
                    bevelBottom = RgbaColor(24, 31, 44)
                }
                isSunken -> {
                    baseTop = RgbaColor(13, 20, 36)
                    baseBottom = RgbaColor(17, 25, 44)
                    borderOuter = if (isFocused) RgbaColor(56, 189, 248) else RgbaColor(64, 78, 99)
                    bevelTop = RgbaColor(9, 14, 26)
                    bevelBottom = RgbaColor(30, 41, 59)
                }
                isPressed -> {
                    baseTop = RgbaColor(26, 36, 52)
                    baseBottom = RgbaColor(34, 46, 66)
                    borderOuter = RgbaColor(82, 98, 122)
                    bevelTop = RgbaColor(20, 28, 42)
                    bevelBottom = RgbaColor(44, 58, 80)
                }
                isHovered -> {
                    baseTop = RgbaColor(52, 68, 94)
                    baseBottom = RgbaColor(38, 51, 72)
                    borderOuter = RgbaColor(94, 112, 138)
                    bevelTop = RgbaColor(68, 86, 116)
                    bevelBottom = RgbaColor(28, 38, 56)
                }
                else -> {
                    baseTop = RgbaColor(45, 59, 82)
                    baseBottom = RgbaColor(33, 44, 63)
                    borderOuter = if (isFocused) RgbaColor(56, 189, 248) else RgbaColor(71, 85, 105)
                    bevelTop = RgbaColor(58, 74, 101)
                    bevelBottom = RgbaColor(24, 33, 49)
                }
            }
        } else {
            when {
                !isEnabled -> {
                    baseTop = RgbaColor(236, 240, 245)
                    baseBottom = RgbaColor(228, 234, 241)
                    borderOuter = RgbaColor(203, 213, 225)
                    bevelTop = RgbaColor(244, 247, 250)
                    bevelBottom = RgbaColor(218, 225, 234)
                }
                isSunken -> {
                    baseTop = RgbaColor(248, 250, 252)
                    baseBottom = RgbaColor(255, 255, 255)
                    borderOuter = if (isFocused) RgbaColor(37, 99, 235) else RgbaColor(148, 163, 184)
                    bevelTop = RgbaColor(226, 232, 240)
                    bevelBottom = RgbaColor(255, 255, 255)
                }
                isPressed -> {
                    baseTop = RgbaColor(208, 216, 228)
                    baseBottom = RgbaColor(222, 229, 238)
                    borderOuter = RgbaColor(120, 136, 158)
                    bevelTop = RgbaColor(192, 202, 216)
                    bevelBottom = RgbaColor(235, 240, 246)
                }
                isHovered -> {
                    baseTop = RgbaColor(250, 252, 255)
                    baseBottom = RgbaColor(232, 238, 247)
                    borderOuter = RgbaColor(128, 146, 170)
                    bevelTop = RgbaColor(255, 255, 255)
                    bevelBottom = RgbaColor(212, 221, 234)
                }
                else -> {
                    baseTop = RgbaColor(245, 248, 252)
                    baseBottom = RgbaColor(224, 231, 241)
                    borderOuter = if (isFocused) RgbaColor(37, 99, 235) else RgbaColor(148, 163, 184)
                    bevelTop = RgbaColor(255, 255, 255)
                    bevelBottom = RgbaColor(206, 216, 229)
                }
            }
        }

        // Fill vertical native gradient body
        val denom = (safeH - 1).coerceAtLeast(1)
        for (py in 0 until safeH) {
            val t = py.toFloat() / denom.toFloat()
            val rowColor = RgbaColor(
                r = (baseTop.r + (baseBottom.r - baseTop.r) * t).roundToInt().coerceIn(0, 255),
                g = (baseTop.g + (baseBottom.g - baseTop.g) * t).roundToInt().coerceIn(0, 255),
                b = (baseTop.b + (baseBottom.b - baseTop.b) * t).roundToInt().coerceIn(0, 255),
                a = 255
            )
            drawHLine(offscreen, 0, safeW, py, rowColor)
        }

        // Inner bevel highlights (top/left specular highlight + bottom/right bevel shadow)
        if (safeW >= 4 && safeH >= 4) {
            drawHLine(offscreen, 1, safeW - 1, 1, bevelTop)
            drawVLine(offscreen, 1, 1, safeH - 1, bevelTop)
            drawHLine(offscreen, 1, safeW - 1, safeH - 2, bevelBottom)
            drawVLine(offscreen, safeW - 2, 1, safeH - 1, bevelBottom)
        }

        // Outer native control frame border
        drawRectBorder(offscreen, 0, 0, safeW, safeH, borderOuter, 1)

        // Subtle corner rounding on the 4 extreme corner pixels when cornerRadius >= 2
        if (cornerRadius >= 2 && safeW >= 6 && safeH >= 6) {
            val cornerFade = RgbaColor(borderOuter.r, borderOuter.g, borderOuter.b, 96)
            offscreen.setPixel(0, 0, cornerFade)
            offscreen.setPixel(safeW - 1, 0, cornerFade)
            offscreen.setPixel(0, safeH - 1, cornerFade)
            offscreen.setPixel(safeW - 1, safeH - 1, cornerFade)
        }

        return offscreen
    }

    /**
     * Tints an offscreen native control sprite in-place using [tintColor] while preserving
     * the native bevel luminance deltas, vertical gradient shading, border contrast, and
     * hover/pressed/disabled states.
     */
    fun tintOffscreenControlBuffer(
        offscreen: ImageBuffer,
        tintColor: RgbaColor,
        isDark: Boolean,
        outline: Boolean = false,
        isHovered: Boolean = false,
        isPressed: Boolean = false,
        isEnabled: Boolean = true
    ) {
        val w = offscreen.width
        val h = offscreen.height
        if (w <= 0 || h <= 0) return

        // State-adjusted target tint color
        val stateTint = when {
            !isEnabled -> RgbaColor(
                r = (tintColor.r * 0.65f + (if (isDark) 30 else 210) * 0.35f).roundToInt().coerceIn(0, 255),
                g = (tintColor.g * 0.65f + (if (isDark) 36 else 215) * 0.35f).roundToInt().coerceIn(0, 255),
                b = (tintColor.b * 0.65f + (if (isDark) 48 else 225) * 0.35f).roundToInt().coerceIn(0, 255),
                a = tintColor.a
            )
            isPressed -> RgbaColor(
                r = (tintColor.r * 0.82f).roundToInt().coerceIn(0, 255),
                g = (tintColor.g * 0.82f).roundToInt().coerceIn(0, 255),
                b = (tintColor.b * 0.82f).roundToInt().coerceIn(0, 255),
                a = tintColor.a
            )
            isHovered -> RgbaColor(
                r = (tintColor.r * 1.10f).roundToInt().coerceIn(0, 255),
                g = (tintColor.g * 1.10f).roundToInt().coerceIn(0, 255),
                b = (tintColor.b * 1.10f).roundToInt().coerceIn(0, 255),
                a = tintColor.a
            )
            else -> tintColor
        }

        // Sample mid-control native luminance as reference zero-point for bevel/gradient preservation
        val midSample = offscreen.getPixel(w / 2, h / 2)
        val midLuma = (midSample.r * 299 + midSample.g * 587 + midSample.b * 114) / 1000

        for (py in 0 until h) {
            for (px in 0 until w) {
                val src = offscreen.getPixel(px, py)
                if (src.a == 0) continue
                val isBorderPixel = px == 0 || px == w - 1 || py == 0 || py == h - 1
                val isInnerBevel = px == 1 || px == w - 2 || py == 1 || py == h - 2

                if (outline && !isHovered && !isPressed) {
                    if (isBorderPixel || isInnerBevel) {
                        val borderCol = if (isBorderPixel) {
                            stateTint
                        } else {
                            RgbaColor(
                                r = (src.r * 0.55f + stateTint.r * 0.45f).roundToInt().coerceIn(0, 255),
                                g = (src.g * 0.55f + stateTint.g * 0.45f).roundToInt().coerceIn(0, 255),
                                b = (src.b * 0.55f + stateTint.b * 0.45f).roundToInt().coerceIn(0, 255),
                                a = src.a
                            )
                        }
                        offscreen.setPixel(px, py, borderCol)
                    } else {
                        // Subtle 10% tint wash on outline interior
                        offscreen.setPixel(
                            px,
                            py,
                            RgbaColor(
                                r = (src.r * 0.90f + stateTint.r * 0.10f).roundToInt().coerceIn(0, 255),
                                g = (src.g * 0.90f + stateTint.g * 0.10f).roundToInt().coerceIn(0, 255),
                                b = (src.b * 0.90f + stateTint.b * 0.10f).roundToInt().coerceIn(0, 255),
                                a = src.a
                            )
                        )
                    }
                } else {
                    val srcLuma = (src.r * 299 + src.g * 587 + src.b * 114) / 1000
                    val lumaDelta = srcLuma - midLuma
                    val bevelOffset = (lumaDelta * 0.85f).roundToInt()
                    val tinted = if (isBorderPixel) {
                        RgbaColor(
                            r = (stateTint.r * 0.72f).roundToInt().coerceIn(0, 255),
                            g = (stateTint.g * 0.72f).roundToInt().coerceIn(0, 255),
                            b = (stateTint.b * 0.72f).roundToInt().coerceIn(0, 255),
                            a = src.a
                        )
                    } else {
                        RgbaColor(
                            r = (stateTint.r + bevelOffset).coerceIn(0, 255),
                            g = (stateTint.g + bevelOffset).coerceIn(0, 255),
                            b = (stateTint.b + bevelOffset).coerceIn(0, 255),
                            a = src.a
                        )
                    }
                    offscreen.setPixel(px, py, tinted)
                }
            }
        }
    }

    /**
     * Blits an offscreen [ImageBuffer] onto [target] at ([dstX], [dstY]), honoring the clipping rectangle.
     */
    fun blitOffscreenBuffer(
        target: ImageBuffer,
        dstX: Int,
        dstY: Int,
        offscreen: ImageBuffer,
        clipX: Int = 0,
        clipY: Int = 0,
        clipW: Int = target.width,
        clipH: Int = target.height
    ) {
        val minX = max(0, clipX)
        val minY = max(0, clipY)
        val maxX = min(target.width, clipX + clipW)
        val maxY = min(target.height, clipY + clipH)

        for (sy in 0 until offscreen.height) {
            val ty = dstY + sy
            if (ty < minY || ty >= maxY) continue
            for (sx in 0 until offscreen.width) {
                val tx = dstX + sx
                if (tx < minX || tx >= maxX) continue
                val px = offscreen.getPixel(sx, sy)
                if (px.a >= 250) {
                    target.setPixel(tx, ty, RgbaColor(px.r, px.g, px.b, 255))
                } else if (px.a > 0) {
                    val cur = target.getPixel(tx, ty)
                    val a = px.a
                    val inv = 255 - a
                    target.setPixel(
                        tx,
                        ty,
                        RgbaColor(
                            r = (cur.r * inv + px.r * a) / 255,
                            g = (cur.g * inv + px.g * a) / 255,
                            b = (cur.b * inv + px.b * a) / 255,
                            a = 255
                        )
                    )
                }
            }
        }
    }

    /**
     * High-level pipeline that:
     * 1. Renders an empty-text native control sprite into an offscreen buffer.
     * 2. Tints the offscreen buffer via `ThemeConfig` / `BootstrapColorPalette` when
     *    `bootstrapType != null && bootstrapType != BootstrapType.NONE`.
     * 3. Blits the offscreen buffer into [ctx.buffer].
     * 4. Renders the contrasting text/icon overlay on top afterward.
     */
    fun renderTintedNativeControl(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        bootstrapType: BootstrapType?,
        outline: Boolean = false,
        isSunken: Boolean = false,
        isHovered: Boolean = false,
        isPressed: Boolean = false,
        isFocused: Boolean = false,
        isEnabled: Boolean = true,
        labelText: String = "",
        centerText: Boolean = true,
        textScale: Int = 1,
        textPaddingLeft: Int = 8,
        explicitTextColor: RgbaColor? = null
    ) {
        if (w <= 0 || h <= 0) return
        val offscreen = renderNativeControlSpriteOffscreen(
            w = w,
            h = h,
            isDark = ctx.isDark,
            isSunken = isSunken,
            isHovered = isHovered,
            isPressed = isPressed,
            isFocused = isFocused,
            isEnabled = isEnabled
        )

        val tint = ctx.resolveBootstrapTintOrNull(bootstrapType)
        if (tint != null) {
            tintOffscreenControlBuffer(
                offscreen = offscreen,
                tintColor = tint,
                isDark = ctx.isDark,
                outline = outline,
                isHovered = isHovered,
                isPressed = isPressed,
                isEnabled = isEnabled
            )
        }

        blitOffscreenBuffer(
            target = ctx.buffer,
            dstX = x,
            dstY = y,
            offscreen = offscreen,
            clipX = ctx.clipX,
            clipY = ctx.clipY,
            clipW = ctx.clipW,
            clipH = ctx.clipH
        )

        if (labelText.isNotEmpty()) {
            val fg = explicitTextColor ?: when {
                !isEnabled -> ctx.textMuted
                tint != null && outline && !isHovered && !isPressed -> tint
                tint != null -> ctx.resolveBootstrapForeground(bootstrapType)
                else -> ctx.textPrimary
            }
            val textW = measureTextWidth(labelText, textScale)
            val tx = if (centerText) {
                (x + (w - textW) / 2).coerceAtLeast(x + 4)
            } else {
                x + textPaddingLeft
            }
            val ty = y + (h - 7 * textScale) / 2
            drawTextClipped(ctx.buffer, tx, ty, (w - (tx - x) - 4).coerceAtLeast(1), labelText, fg, textScale)
        }
    }

    // =========================================================================
    // Shared 2D Primitive & 5x7 Bitmap Font Rendering Routines
    // =========================================================================

    fun fillRect(buf: ImageBuffer, x: Int, y: Int, w: Int, h: Int, color: RgbaColor) {
        val x0 = x.coerceIn(0, buf.width)
        val y0 = y.coerceIn(0, buf.height)
        val x1 = (x + w).coerceIn(0, buf.width)
        val y1 = (y + h).coerceIn(0, buf.height)
        if (x1 <= x0 || y1 <= y0) return
        val bytes = buf.bytes
        val r = color.r.toByte()
        val g = color.g.toByte()
        val b = color.b.toByte()
        val a = 0xFF.toByte()
        val stride = buf.width
        for (py in y0 until y1) {
            var idx = (py * stride + x0) * 4
            for (px in x0 until x1) {
                bytes[idx] = r
                bytes[idx + 1] = g
                bytes[idx + 2] = b
                bytes[idx + 3] = a
                idx += 4
            }
        }
    }

    fun fillRectAlpha(buf: ImageBuffer, x: Int, y: Int, w: Int, h: Int, color: RgbaColor) {
        val x0 = x.coerceIn(0, buf.width)
        val y0 = y.coerceIn(0, buf.height)
        val x1 = (x + w).coerceIn(0, buf.width)
        val y1 = (y + h).coerceIn(0, buf.height)
        val alpha = color.a.coerceIn(0, 255)
        val inv = 255 - alpha
        for (py in y0 until y1) {
            for (px in x0 until x1) {
                val cur = buf.getPixel(px, py)
                buf.setPixel(
                    px,
                    py,
                    RgbaColor(
                        r = (cur.r * inv + color.r * alpha) / 255,
                        g = (cur.g * inv + color.g * alpha) / 255,
                        b = (cur.b * inv + color.b * alpha) / 255,
                        a = 255
                    )
                )
            }
        }
    }

    fun drawHLine(buf: ImageBuffer, x0: Int, x1: Int, y: Int, color: RgbaColor) {
        if (y !in 0 until buf.height) return
        val start = min(x0, x1).coerceIn(0, buf.width)
        val end = max(x0, x1).coerceIn(0, buf.width)
        for (px in start until end) {
            buf.setPixel(px, y, color)
        }
    }

    fun drawVLine(buf: ImageBuffer, x: Int, y0: Int, y1: Int, color: RgbaColor) {
        if (x !in 0 until buf.width) return
        val start = min(y0, y1).coerceIn(0, buf.height)
        val end = max(y0, y1).coerceIn(0, buf.height)
        for (py in start until end) {
            buf.setPixel(x, py, color)
        }
    }

    fun drawRectBorder(buf: ImageBuffer, x: Int, y: Int, w: Int, h: Int, color: RgbaColor, thickness: Int = 1) {
        for (t in 0 until thickness) {
            drawHLine(buf, x + t, x + w - t, y + t, color)
            drawHLine(buf, x + t, x + w - t, y + h - 1 - t, color)
            drawVLine(buf, x + t, y + t, y + h - t, color)
            drawVLine(buf, x + w - 1 - t, y + t, y + h - t, color)
        }
    }

    fun drawLine(buf: ImageBuffer, x0: Int, y0: Int, x1: Int, y1: Int, color: RgbaColor) {
        var cx = x0
        var cy = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            buf.setPixel(cx, cy, color)
            buf.setPixel(cx + 1, cy, color)
            if (cx == x1 && cy == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                cx += sx
            }
            if (e2 <= dx) {
                err += dx
                cy += sy
            }
        }
    }

    fun drawCircle(buf: ImageBuffer, cx: Int, cy: Int, r: Int, color: RgbaColor) {
        for (deg in 0 until 360 step 4) {
            val rad = deg * PI / 180.0
            val px = cx + (r * cos(rad)).roundToInt()
            val py = cy + (r * sin(rad)).roundToInt()
            buf.setPixel(px, py, color)
        }
    }

    fun fillCircle(buf: ImageBuffer, cx: Int, cy: Int, r: Int, color: RgbaColor) {
        val r2 = r * r
        for (dy in -r..r) {
            for (dx in -r..r) {
                if (dx * dx + dy * dy <= r2) {
                    buf.setPixel(cx + dx, cy + dy, color)
                }
            }
        }
    }

    fun measureTextWidth(text: String, scale: Int = 1, letterSpacingPx: Int = 0): Int {
        if (text.isEmpty()) return 0
        val step = 6 * scale + letterSpacingPx
        return text.length * step
    }

    fun drawText(
        buf: ImageBuffer,
        x: Int,
        y: Int,
        text: String,
        color: RgbaColor,
        scale: Int = 1,
        bold: Boolean = false,
        italic: Boolean = false,
        letterSpacingPx: Int = 0
    ) {
        var curX = x
        val step = (6 * scale + letterSpacingPx).coerceAtLeast(4)
        for (ch in text) {
            drawGlyph(buf, curX, y, ch, color, scale, bold = bold, italic = italic)
            curX += step
        }
    }

    fun drawTextClipped(
        buf: ImageBuffer,
        x: Int,
        y: Int,
        maxW: Int,
        text: String,
        color: RgbaColor,
        scale: Int = 1,
        bold: Boolean = false,
        italic: Boolean = false,
        letterSpacingPx: Int = 0,
        ellipsis: Boolean = true
    ) {
        if (maxW <= 0 || text.isEmpty()) return
        val step = (6 * scale + letterSpacingPx).coerceAtLeast(4)
        val maxChars = (maxW / step).coerceAtLeast(1)
        val clipped = if (text.length > maxChars) {
            if (ellipsis && maxChars > 2) text.take(maxChars - 2) + ".." else text.take(maxChars)
        } else {
            text
        }
        drawText(buf, x, y, clipped, color, scale, bold = bold, italic = italic, letterSpacingPx = letterSpacingPx)
    }

    fun drawWrappedText(
        buf: ImageBuffer,
        x: Int,
        y: Int,
        maxW: Int,
        maxH: Int,
        text: String,
        color: RgbaColor,
        scale: Int = 1,
        bold: Boolean = false,
        italic: Boolean = false,
        lineHeightPx: Int? = null,
        letterSpacingPx: Int = 0
    ) {
        if (maxW <= 0 || maxH <= 0 || text.isEmpty()) return
        val step = (6 * scale + letterSpacingPx).coerceAtLeast(4)
        val lineH = (lineHeightPx ?: (9 * scale)).coerceAtLeast(7 * scale + 1)
        val maxChars = (maxW / step).coerceAtLeast(1)
        if (text.length <= maxChars || maxH < lineH * 2) {
            drawTextClipped(buf, x, y, maxW, text, color, scale, bold = bold, italic = italic, letterSpacingPx = letterSpacingPx)
            return
        }
        var curY = y
        var remaining = text
        while (remaining.isNotEmpty() && curY + 7 * scale <= y + maxH) {
            val chunk = if (remaining.length <= maxChars) {
                val c = remaining
                remaining = ""
                c
            } else {
                val splitAt = remaining.lastIndexOf(' ', maxChars).takeIf { it > maxChars / 2 } ?: maxChars
                val c = remaining.substring(0, splitAt).trim()
                remaining = remaining.substring(splitAt).trimStart()
                c
            }
            drawText(buf, x, curY, chunk, color, scale, bold = bold, italic = italic, letterSpacingPx = letterSpacingPx)
            curY += lineH
        }
    }

    fun drawGlyph(
        buf: ImageBuffer,
        x: Int,
        y: Int,
        ch: Char,
        color: RgbaColor,
        scale: Int,
        bold: Boolean = false,
        italic: Boolean = false
    ) {
        val rows = glyph5x7(ch)
        for (ry in 0 until 7) {
            val bits = rows[ry]
            if (bits == 0) continue
            val italicShift = if (italic) ((6 - ry) / 3) else 0
            for (rx in 0 until 5) {
                if (((bits ushr (4 - rx)) and 1) != 0) {
                    val baseX = x + rx * scale + italicShift
                    val baseY = y + ry * scale
                    if (scale <= 1) {
                        buf.setPixel(baseX, baseY, color)
                        if (bold) {
                            buf.setPixel(baseX + 1, baseY, color)
                        }
                    } else {
                        for (sy in 0 until scale) {
                            for (sx in 0 until scale) {
                                buf.setPixel(baseX + sx, baseY + sy, color)
                                if (bold && sx == scale - 1) {
                                    buf.setPixel(baseX + sx + 1, baseY + sy, color)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun glyph5x7(ch: Char): IntArray = when (ch) {
        'A', 'a' -> intArrayOf(0x0E, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11)
        'B', 'b' -> intArrayOf(0x1E, 0x11, 0x11, 0x1E, 0x11, 0x11, 0x1E)
        'C', 'c' -> intArrayOf(0x0E, 0x11, 0x10, 0x10, 0x10, 0x11, 0x0E)
        'D', 'd' -> intArrayOf(0x1C, 0x12, 0x11, 0x11, 0x11, 0x12, 0x1C)
        'E', 'e' -> intArrayOf(0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x1F)
        'F', 'f' -> intArrayOf(0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x10)
        'G', 'g' -> intArrayOf(0x0E, 0x11, 0x10, 0x17, 0x11, 0x11, 0x0F)
        'H', 'h' -> intArrayOf(0x11, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11)
        'I', 'i' -> intArrayOf(0x0E, 0x04, 0x04, 0x04, 0x04, 0x04, 0x0E)
        'J', 'j' -> intArrayOf(0x07, 0x02, 0x02, 0x02, 0x02, 0x12, 0x0C)
        'K', 'k' -> intArrayOf(0x11, 0x12, 0x14, 0x18, 0x14, 0x12, 0x11)
        'L', 'l' -> intArrayOf(0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x1F)
        'M', 'm' -> intArrayOf(0x11, 0x1B, 0x15, 0x15, 0x11, 0x11, 0x11)
        'N', 'n' -> intArrayOf(0x11, 0x19, 0x15, 0x13, 0x11, 0x11, 0x11)
        'O', 'o' -> intArrayOf(0x0E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E)
        'P', 'p' -> intArrayOf(0x1E, 0x11, 0x11, 0x1E, 0x10, 0x10, 0x10)
        'Q', 'q' -> intArrayOf(0x0E, 0x11, 0x11, 0x11, 0x15, 0x12, 0x0D)
        'R', 'r' -> intArrayOf(0x1E, 0x11, 0x11, 0x1E, 0x14, 0x12, 0x11)
        'S', 's' -> intArrayOf(0x0F, 0x10, 0x10, 0x0E, 0x01, 0x01, 0x1E)
        'T', 't' -> intArrayOf(0x1F, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04)
        'U', 'u' -> intArrayOf(0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E)
        'V', 'v', '▾', '▼' -> intArrayOf(0x11, 0x11, 0x11, 0x11, 0x11, 0x0A, 0x04)
        'W', 'w' -> intArrayOf(0x11, 0x11, 0x11, 0x15, 0x15, 0x1B, 0x11)
        'X', 'x', '✕', '×' -> intArrayOf(0x11, 0x11, 0x0A, 0x04, 0x0A, 0x11, 0x11)
        'Y', 'y' -> intArrayOf(0x11, 0x11, 0x0A, 0x04, 0x04, 0x04, 0x04)
        'Z', 'z' -> intArrayOf(0x1F, 0x01, 0x02, 0x04, 0x08, 0x10, 0x1F)
        '0' -> intArrayOf(0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E)
        '1' -> intArrayOf(0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E)
        '2' -> intArrayOf(0x0E, 0x11, 0x01, 0x02, 0x04, 0x08, 0x1F)
        '3' -> intArrayOf(0x1F, 0x02, 0x04, 0x02, 0x01, 0x11, 0x0E)
        '4' -> intArrayOf(0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02)
        '5' -> intArrayOf(0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E)
        '6' -> intArrayOf(0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E)
        '7' -> intArrayOf(0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08)
        '8' -> intArrayOf(0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E)
        '9' -> intArrayOf(0x0E, 0x11, 0x11, 0x0F, 0x01, 0x02, 0x0C)
        ':' -> intArrayOf(0x00, 0x04, 0x04, 0x00, 0x04, 0x04, 0x00)
        '.' -> intArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x06)
        ',' -> intArrayOf(0x00, 0x00, 0x00, 0x00, 0x06, 0x04, 0x08)
        '-', '—', '–' -> intArrayOf(0x00, 0x00, 0x00, 0x1F, 0x00, 0x00, 0x00)
        '+' -> intArrayOf(0x00, 0x04, 0x04, 0x1F, 0x04, 0x04, 0x00)
        '/' -> intArrayOf(0x01, 0x02, 0x02, 0x04, 0x08, 0x08, 0x10)
        '(' -> intArrayOf(0x02, 0x04, 0x08, 0x08, 0x08, 0x04, 0x02)
        ')' -> intArrayOf(0x08, 0x04, 0x02, 0x02, 0x02, 0x04, 0x08)
        '[', '{' -> intArrayOf(0x0E, 0x08, 0x08, 0x08, 0x08, 0x08, 0x0E)
        ']', '}' -> intArrayOf(0x0E, 0x02, 0x02, 0x02, 0x02, 0x02, 0x0E)
        '<' -> intArrayOf(0x02, 0x04, 0x08, 0x10, 0x08, 0x04, 0x02)
        '>', '▶', '▸' -> intArrayOf(0x08, 0x04, 0x02, 0x01, 0x02, 0x04, 0x08)
        '%' -> intArrayOf(0x19, 0x19, 0x02, 0x04, 0x08, 0x13, 0x13)
        '#' -> intArrayOf(0x0A, 0x1F, 0x0A, 0x0A, 0x1F, 0x0A, 0x00)
        '@' -> intArrayOf(0x0E, 0x11, 0x17, 0x15, 0x17, 0x10, 0x0E)
        '*', '•', '✦', '★' -> intArrayOf(0x00, 0x04, 0x15, 0x0E, 0x15, 0x04, 0x00)
        '=' -> intArrayOf(0x00, 0x00, 0x1F, 0x00, 0x1F, 0x00, 0x00)
        '_' -> intArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1F)
        '|' -> intArrayOf(0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04)
        '!' -> intArrayOf(0x04, 0x04, 0x04, 0x04, 0x04, 0x00, 0x04)
        '?' -> intArrayOf(0x0E, 0x11, 0x01, 0x02, 0x04, 0x00, 0x04)
        '"', '\'' -> intArrayOf(0x0A, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00)
        '&' -> intArrayOf(0x0C, 0x12, 0x14, 0x08, 0x15, 0x12, 0x0D)
        '$' -> intArrayOf(0x04, 0x0F, 0x14, 0x0E, 0x05, 0x1E, 0x04)
        ';' -> intArrayOf(0x00, 0x04, 0x04, 0x00, 0x04, 0x04, 0x08)
        '\\' -> intArrayOf(0x10, 0x08, 0x08, 0x04, 0x02, 0x02, 0x01)
        '~' -> intArrayOf(0x00, 0x00, 0x08, 0x15, 0x02, 0x00, 0x00)
        '^' -> intArrayOf(0x04, 0x0A, 0x11, 0x00, 0x00, 0x00, 0x00)
        '`' -> intArrayOf(0x08, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00)
        ' ' -> intArrayOf(0, 0, 0, 0, 0, 0, 0)
        else -> intArrayOf(0x0E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E)
    }
}
