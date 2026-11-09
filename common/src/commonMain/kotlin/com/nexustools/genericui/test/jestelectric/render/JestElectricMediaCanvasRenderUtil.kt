package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.AbstractContainer
import com.nexustools.genericui.style.color.RgbaColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renders media, canvas, WebView, code-editor, chart, sparkline, node-graph,
 * audio/video player, and GPU/physics/spectrum extension surfaces into a
 * [JestElectricRenderContext] without requiring direct `:extensions:*` module imports.
 */
object JestElectricMediaCanvasRenderUtil {

    fun isExtensionCanvasOrSurface(comp: AbstractComponent): Boolean {
        val tag = comp.htmlTag.lowercase()
        val cls = comp.componentClassPrefix.lowercase() + " " + comp.getClass().lowercase()
        val id = (comp.id ?: "").lowercase()
        return tag.contains("canvas") ||
            tag.contains("codeeditor") ||
            tag.contains("curveeditor") ||
            tag.contains("webview") ||
            tag.contains("video") ||
            tag.contains("audio") ||
            tag.contains("chart") ||
            tag.contains("sparkline") ||
            tag.contains("nodegraph") ||
            cls.contains("canvas") ||
            cls.contains("code-editor") ||
            cls.contains("webview") ||
            cls.contains("video-player") ||
            cls.contains("audio-player") ||
            cls.contains("chart") ||
            cls.contains("sparkline") ||
            id.contains("canvas") ||
            id.contains("code-editor") ||
            id.contains("webview")
    }

    fun canRender(comp: AbstractComponent): Boolean = isExtensionCanvasOrSurface(comp)

    fun render(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: AbstractComponent,
        depth: Int
    ): Boolean {
        if (!isExtensionCanvasOrSurface(comp)) return false
        if (w <= 0 || h <= 0) return true

        val tag = comp.htmlTag.lowercase()
        val id = (comp.id ?: "").lowercase()
        val cls = (comp.componentClassPrefix + " " + comp.getClass()).lowercase()

        // If the component is a composite container with child controls (e.g., WebView or composite player),
        // render its children when non-empty.
        if (comp is AbstractContainer && comp.children.isNotEmpty() && !tag.contains("canvas")) {
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
            JestElectricContainerChromeRenderUtil.renderVerticalChildren(
                ctx = ctx,
                x = x + 6,
                y = y + 6,
                w = (w - 12).coerceAtLeast(1),
                h = (h - 12).coerceAtLeast(1),
                children = comp.children,
                depth = depth + 1,
                gap = 6
            )
            return true
        }

        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, RgbaColor(13, 19, 33))
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.accent, 1)

        when {
            tag.contains("codeeditor") || id.contains("code-editor") || cls.contains("code-editor") -> {
                val gutterW = 36.coerceAtMost(w / 4)
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 1, y + 1, gutterW, h - 2, RgbaColor(20, 29, 47))
                JestElectricControlTintRenderUtil.drawVLine(ctx.buffer, x + gutterW, y + 1, y + h - 1, ctx.borderSubtle)
                val sampleLines = listOf(
                    "1" to ("@MainAppFragment" to RgbaColor(250, 204, 21)),
                    "2" to ("class ShowcaseView : Fragment() {" to RgbaColor(56, 189, 248)),
                    "3" to ("    val theme = ThemeMode.DARK" to RgbaColor(167, 139, 250)),
                    "4" to ("    val accent = \"#38BDF8\"" to RgbaColor(74, 222, 128)),
                    "5" to ("    fun render(canvas: Canvas2D) {" to RgbaColor(244, 114, 182)),
                    "6" to ("        canvas.strokeStyle = accent" to RgbaColor(226, 232, 240)),
                    "7" to ("        canvas.strokeRect(12f, 12f, 320f, 180f)" to RgbaColor(226, 232, 240)),
                    "8" to ("    }" to RgbaColor(56, 189, 248)),
                    "9" to ("}" to RgbaColor(56, 189, 248))
                )
                var ly = y + 8
                for ((ln, pair) in sampleLines) {
                    if (ly + 10 > y + h - 4) break
                    JestElectricControlTintRenderUtil.drawText(ctx.buffer, x + 8, ly, ln, RgbaColor(100, 116, 139), 1)
                    JestElectricControlTintRenderUtil.drawTextClipped(
                        buf = ctx.buffer,
                        x = x + gutterW + 8,
                        y = ly,
                        maxW = (w - gutterW - 14).coerceAtLeast(1),
                        text = pair.first,
                        color = pair.second,
                        scale = 1
                    )
                    ly += 14
                }
            }
            id.contains("histogram") -> {
                JestElectricControlTintRenderUtil.drawText(ctx.buffer, x + 8, y + 6, "RGB Luminance Histogram", ctx.textSecondary, 1)
                val barAreaH = (h - 24).coerceAtLeast(20)
                val barCount = ((w - 16) / 4).coerceAtLeast(8)
                for (i in 0 until barCount) {
                    val norm = i.toFloat() / barCount
                    val wave = (sin(norm * PI) * 0.75 + cos(norm * PI * 3.0) * 0.2 + 0.2).toFloat().coerceIn(0.08f, 0.95f)
                    val bh = (barAreaH * wave).roundToInt()
                    val bx = x + 8 + i * 4
                    val by = y + h - 6 - bh
                    val col = when (i % 3) {
                        0 -> RgbaColor(239, 68, 68)
                        1 -> RgbaColor(34, 197, 94)
                        else -> RgbaColor(59, 130, 246)
                    }
                    JestElectricControlTintRenderUtil.fillRect(ctx.buffer, bx, by, 3, bh, col)
                }
            }
            id.contains("filter") || id.contains("preview") -> {
                for (py in 1 until h - 1) {
                    val v = py.toFloat() / h
                    for (px in 1 until w - 1) {
                        val u = px.toFloat() / w
                        val r = (40 + 180 * u).toInt().coerceIn(0, 255)
                        val g = (50 + 160 * (1f - v)).toInt().coerceIn(0, 255)
                        val b = (90 + 150 * sin((u + v) * PI).toFloat()).toInt().coerceIn(0, 255)
                        ctx.buffer.setPixel(x + px, y + py, RgbaColor(r, g, b))
                    }
                }
                val cx = x + w / 2
                val cy = y + h / 2
                val rad = (min(w, h) / 4).coerceAtLeast(12)
                JestElectricControlTintRenderUtil.drawCircle(ctx.buffer, cx, cy, rad, RgbaColor.WHITE)
                JestElectricControlTintRenderUtil.drawText(ctx.buffer, x + 10, y + 10, "GPU Shader Pipeline Preview", RgbaColor.WHITE, 1)
            }
            id.contains("spectrum") || id.contains("visualizer") || cls.contains("visualizer") || tag.contains("audio") -> {
                JestElectricControlTintRenderUtil.drawText(
                    ctx.buffer,
                    x + 10,
                    y + 8,
                    "Real-Time 64-Band FFT Spectrum & Waveform",
                    ctx.accent,
                    1
                )
                val bars = 32
                val barW = ((w - 24) / bars).coerceAtLeast(4)
                val maxBarH = (h - 36).coerceAtLeast(24)
                for (i in 0 until bars) {
                    val t = i.toFloat() / bars
                    val mag = (sin(t * PI) * 0.7 + cos(t * PI * 4) * 0.25 + 0.2).toFloat().coerceIn(0.1f, 0.95f)
                    val bh = (maxBarH * mag).roundToInt()
                    val bx = x + 12 + i * barW
                    val by = y + h - 8 - bh
                    val c = RgbaColor(
                        r = (56 + 180 * t).toInt().coerceIn(0, 255),
                        g = (189 - 60 * t).toInt().coerceIn(0, 255),
                        b = 248
                    )
                    JestElectricControlTintRenderUtil.fillRect(ctx.buffer, bx, by, (barW - 2).coerceAtLeast(2), bh, c)
                }
            }
            id.contains("physics") || id.contains("arena") -> {
                JestElectricControlTintRenderUtil.drawText(ctx.buffer, x + 10, y + 8, "Box2D World2D Rigid-Body Arena", ctx.accent, 1)
                val groundY = y + h - 22
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 16, groundY, w - 32, 6, RgbaColor(74, 222, 128))
                val boxW = 28
                val boxH = 18
                for (row in 0 until 4) {
                    val count = 4 - row
                    val startX = x + w / 2 - (count * (boxW + 4)) / 2
                    val by = groundY - (row + 1) * (boxH + 2)
                    for (col in 0 until count) {
                        val bx = startX + col * (boxW + 4)
                        if (by > y + 20) {
                            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, bx, by, boxW, boxH, RgbaColor(56, 189, 248, 90))
                            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, bx, by, boxW, boxH, RgbaColor(56, 189, 248), 1)
                        }
                    }
                }
                JestElectricControlTintRenderUtil.drawCircle(ctx.buffer, x + w / 3, groundY - 36, 14, RgbaColor(250, 204, 21))
            }
            tag.contains("video") || cls.contains("video") -> {
                val cx = x + w / 2
                val cy = y + h / 2
                JestElectricControlTintRenderUtil.fillCircle(ctx.buffer, cx, cy, 18, ctx.accent)
                JestElectricControlTintRenderUtil.drawText(ctx.buffer, cx - 3, cy - 3, ">", RgbaColor.WHITE, 1, bold = true)
                JestElectricControlTintRenderUtil.drawTextClipped(
                    buf = ctx.buffer,
                    x = x + 10,
                    y = y + h - 16,
                    maxW = (w - 20).coerceAtLeast(1),
                    text = "00:00 / 03:45 [HD]",
                    color = ctx.textSecondary,
                    scale = 1
                )
            }
            tag.contains("webview") || cls.contains("webview") -> {
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 1, y + 1, w - 2, 22.coerceAtMost(h), ctx.bgElevated)
                JestElectricControlTintRenderUtil.drawTextClipped(
                    buf = ctx.buffer,
                    x = x + 8,
                    y = y + 8,
                    maxW = (w - 16).coerceAtLeast(1),
                    text = "https://genericui.nexustools.com",
                    color = ctx.textSecondary,
                    scale = 1
                )
            }
            else -> {
                JestElectricControlTintRenderUtil.fillRect(
                    ctx.buffer,
                    x + 2,
                    y + 2,
                    w - 4,
                    h - 4,
                    if (ctx.isDark) RgbaColor(18, 25, 42) else RgbaColor.WHITE
                )
                val gridCol = if (ctx.isDark) RgbaColor(30, 41, 59) else RgbaColor(226, 232, 240)
                var gx = x + 24
                while (gx < x + w - 4) {
                    JestElectricControlTintRenderUtil.drawVLine(ctx.buffer, gx, y + 2, y + h - 2, gridCol)
                    gx += 24
                }
                var gy = y + 24
                while (gy < y + h - 4) {
                    JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x + 2, x + w - 2, gy, gridCol)
                    gy += 24
                }
                if (w > 120 && h > 70) {
                    JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x + 24, y + 20, w / 3, h / 2, ctx.accent, 2)
                    JestElectricControlTintRenderUtil.drawCircle(
                        ctx.buffer,
                        x + (w * 2) / 3,
                        y + h / 2,
                        min(w, h) / 4,
                        RgbaColor(244, 114, 182)
                    )
                    JestElectricControlTintRenderUtil.drawLine(
                        ctx.buffer,
                        x + 20,
                        y + h - 18,
                        x + w - 20,
                        y + 20,
                        RgbaColor(74, 222, 128)
                    )
                }
            }
        }
        return true
    }
}
