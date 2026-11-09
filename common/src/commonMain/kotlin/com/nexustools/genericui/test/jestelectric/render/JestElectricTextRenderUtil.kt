package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.typography.Badge
import com.nexustools.genericui.component.typography.Blockquote
import com.nexustools.genericui.component.typography.CodeBlock
import com.nexustools.genericui.component.typography.Heading
import com.nexustools.genericui.component.typography.Label
import com.nexustools.genericui.component.typography.MarkdownView
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.enums.bootstrap.BootstrapFontSize
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.bootstrap.FontWeight
import com.nexustools.genericui.enums.bootstrap.TextAlignment
import com.nexustools.genericui.enums.bootstrap.TextOverflow
import com.nexustools.genericui.enums.bootstrap.TextTransform
import com.nexustools.genericui.enums.css.CssFontStyle
import com.nexustools.genericui.enums.css.CssTextDecoration
import com.nexustools.genericui.enums.css.CssWhiteSpace
import com.nexustools.genericui.enums.css.CssWordBreak
import com.nexustools.genericui.style.color.RgbaColor
import kotlin.math.roundToInt

/**
 * Renders typography and text components ([Label], [Text], [Heading], [Badge], [Blockquote],
 * [CodeBlock], [MarkdownView]) into a [JestElectricRenderContext].
 *
 * Honors `ComponentStyle` typography and text color properties (`color`, `fontFamily`, `fontSize`,
 * `fontWeight`, `fontStyle`, `lineHeight`, `letterSpacing`, `textAlign`, `textDecoration`,
 * `textTransform`, `textOverflow`, `whiteSpace`, `wordBreak`) alongside `bootstrap.textColorType`,
 * `bootstrap.fontSize`, `bootstrap.fontWeight`, and `bootstrap.textAlignment`.
 */
object JestElectricTextRenderUtil {

    fun canRender(comp: AbstractComponent): Boolean =
        comp is Badge ||
            comp is Heading ||
            comp is Blockquote ||
            comp is CodeBlock ||
            comp is Text ||
            comp is Label ||
            comp is MarkdownView

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
            is Badge -> {
                renderBadge(ctx, x, y, w, h, comp)
                true
            }
            is Heading -> {
                renderHeading(ctx, x, y, w, h, comp)
                true
            }
            is Blockquote -> {
                renderBlockquote(ctx, x, y, w, h, comp)
                true
            }
            is CodeBlock -> {
                renderCodeBlock(ctx, x, y, w, h, comp)
                true
            }
            is MarkdownView -> {
                renderMarkdownView(ctx, x, y, w, h, comp)
                true
            }
            is Label -> {
                renderLabelOrText(ctx, x, y, w, h, comp)
                true
            }
            else -> false
        }
    }

    private fun renderBadge(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Badge
    ) {
        val rawText = applyTextTransform(comp.text, comp)
        val scale = resolveScale(comp, defaultScale = 1)
        val bold = resolveBold(comp, defaultBold = true)
        val italic = resolveItalic(comp)
        val letterSpacing = resolveLetterSpacingPx(comp)
        val textW = JestElectricControlTintRenderUtil.measureTextWidth(rawText, scale, letterSpacing)
        val bw = (textW + 14).coerceAtMost(w)
        val bh = h.coerceIn(16, 24)
        val by = y + (h - bh) / 2

        val bgType = comp.backgroundType?.takeIf { it != BootstrapType.NONE }
            ?: comp.type.takeIf { it != BootstrapType.NONE }
        val bg = when {
            ctx.allowsFullComponentStyleOnContainer(comp) && comp.style.backgroundColor != null ->
                comp.style.backgroundColor!!
            bgType != null -> ctx.resolveBootstrapColor(bgType)
            else -> ctx.bgElevated
        }
        val fg = resolveTextColor(
            ctx = ctx,
            comp = comp,
            fallback = if (bgType != null) ctx.resolveBootstrapForeground(bgType) else ctx.textPrimary
        )

        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, by, bw, bh, bg)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, by, bw, bh, ctx.borderSubtle, 1)
        val tx = x + ((bw - textW) / 2).coerceAtLeast(4)
        val ty = by + (bh - 7 * scale) / 2
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = tx,
            y = ty,
            maxW = (bw - 8).coerceAtLeast(1),
            text = rawText,
            color = fg,
            scale = scale,
            bold = bold,
            italic = italic,
            letterSpacingPx = letterSpacing
        )
        drawTextDecorationLine(ctx, tx, ty, minOf(textW, bw - 8), scale, fg, resolveTextDecoration(comp))
    }

    private fun renderHeading(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Heading
    ) {
        val defaultScale = if (comp.level <= 2 && h >= 22) 2 else 1
        val scale = resolveScale(comp, defaultScale = defaultScale)
        val bold = resolveBold(comp, defaultBold = comp.level <= 4)
        val italic = resolveItalic(comp)
        val letterSpacing = resolveLetterSpacingPx(comp)
        val fg = resolveTextColor(ctx, comp, ctx.textPrimary)
        val transformed = applyTextTransform(comp.text, comp)
        val textW = JestElectricControlTintRenderUtil.measureTextWidth(transformed, scale, letterSpacing)
        val tx = resolveAlignedX(x, w, textW, comp, pad = 2)
        val ty = y + ((h - 7 * scale) / 2).coerceAtLeast(0)
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = tx,
            y = ty,
            maxW = (w - 4).coerceAtLeast(1),
            text = transformed,
            color = fg,
            scale = scale,
            bold = bold,
            italic = italic,
            letterSpacingPx = letterSpacing,
            ellipsis = shouldEllipsize(comp)
        )
        drawTextDecorationLine(ctx, tx, ty, minOf(textW, w - 4), scale, fg, resolveTextDecoration(comp))
    }

    private fun renderBlockquote(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Blockquote
    ) {
        val barColor = comp.textColorType?.takeIf { it != BootstrapType.NONE }?.let { ctx.resolveBootstrapColor(it) }
            ?: ctx.accent
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 2, y + 2, 3, (h - 4).coerceAtLeast(4), barColor)
        val fg = resolveTextColor(ctx, comp, ctx.textPrimary)
        val scale = resolveScale(comp, defaultScale = 1)
        val bold = resolveBold(comp, defaultBold = false)
        val italic = resolveItalic(comp) || comp.style.fontStyle == null
        val letterSpacing = resolveLetterSpacingPx(comp)
        val quoteText = applyTextTransform(comp.text, comp)
        val citation = comp.footerCitation
        val quoteH = if (!citation.isNullOrBlank() && h >= 28) h - 14 else h
        JestElectricControlTintRenderUtil.drawWrappedText(
            buf = ctx.buffer,
            x = x + 10,
            y = y + 3,
            maxW = (w - 14).coerceAtLeast(1),
            maxH = (quoteH - 4).coerceAtLeast(8),
            text = quoteText,
            color = fg,
            scale = scale,
            bold = bold,
            italic = italic,
            lineHeightPx = resolveLineHeightPx(comp, scale),
            letterSpacingPx = letterSpacing
        )
        if (!citation.isNullOrBlank() && h >= 24) {
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = x + 12,
                y = y + h - 11,
                maxW = (w - 16).coerceAtLeast(1),
                text = "- $citation",
                color = ctx.textMuted,
                scale = 1,
                italic = true
            )
        }
    }

    private fun renderCodeBlock(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: CodeBlock
    ) {
        val codeBg = if (ctx.isDark) RgbaColor(11, 17, 30) else RgbaColor(236, 242, 249)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, codeBg)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        val fg = resolveTextColor(ctx, comp, ctx.textSecondary)
        val scale = resolveScale(comp, defaultScale = 1)
        val lineH = (resolveLineHeightPx(comp, scale) ?: (11 * scale)).coerceAtLeast(9)
        val lines = comp.text.lines()
        var cy = y + 5
        for (line in lines) {
            if (cy + 7 * scale > y + h - 3) break
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = x + 6,
                y = cy,
                maxW = (w - 12).coerceAtLeast(1),
                text = line,
                color = fg,
                scale = scale,
                ellipsis = false
            )
            cy += lineH
        }
    }

    private fun renderLabelOrText(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Label
    ) {
        val fg = resolveTextColor(ctx, comp, ctx.textPrimary)
        val scale = resolveScale(comp, defaultScale = 1)
        val bold = resolveBold(comp, defaultBold = false)
        val italic = resolveItalic(comp)
        val letterSpacing = resolveLetterSpacingPx(comp)
        val lineHeightPx = resolveLineHeightPx(comp, scale)
        val transformed = applyTextTransform(comp.text, comp)
        val noWrap = isNoWrap(comp)
        val decoration = resolveTextDecoration(comp)

        if (!noWrap && h >= 22 && transformed.length * (6 * scale + letterSpacing) > w) {
            JestElectricControlTintRenderUtil.drawWrappedText(
                buf = ctx.buffer,
                x = x + 2,
                y = y + 3,
                maxW = (w - 4).coerceAtLeast(1),
                maxH = (h - 4).coerceAtLeast(8),
                text = transformed,
                color = fg,
                scale = scale,
                bold = bold,
                italic = italic,
                lineHeightPx = lineHeightPx,
                letterSpacingPx = letterSpacing
            )
        } else {
            val textW = JestElectricControlTintRenderUtil.measureTextWidth(transformed, scale, letterSpacing)
            val tx = resolveAlignedX(x, w, textW, comp, pad = 2)
            val ty = y + ((h - 7 * scale) / 2).coerceAtLeast(0)
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = tx,
                y = ty,
                maxW = (w - 4).coerceAtLeast(1),
                text = transformed,
                color = fg,
                scale = scale,
                bold = bold,
                italic = italic,
                letterSpacingPx = letterSpacing,
                ellipsis = shouldEllipsize(comp)
            )
            drawTextDecorationLine(ctx, tx, ty, minOf(textW, w - 4), scale, fg, decoration)
        }
    }

    private fun renderMarkdownView(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: MarkdownView
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        val baseFg = resolveTextColor(ctx, comp, ctx.textPrimary)
        val lines = comp.markdown.lines()
        var cy = y + 6
        var inCodeFence = false

        for (rawLine in lines) {
            if (cy + 10 > y + h - 4) break
            val trimmed = rawLine.trim()
            if (trimmed.startsWith("```")) {
                inCodeFence = !inCodeFence
                continue
            }
            if (inCodeFence) {
                val codeBg = if (ctx.isDark) RgbaColor(15, 23, 42) else RgbaColor(236, 242, 249)
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 6, cy - 1, (w - 12).coerceAtLeast(1), 11, codeBg)
                JestElectricControlTintRenderUtil.drawTextClipped(
                    buf = ctx.buffer,
                    x = x + 10,
                    y = cy + 1,
                    maxW = (w - 20).coerceAtLeast(1),
                    text = rawLine,
                    color = ctx.textSecondary,
                    scale = 1
                )
                cy += 11
                continue
            }
            when {
                trimmed.isEmpty() -> cy += 5
                trimmed.startsWith("# ") -> {
                    JestElectricControlTintRenderUtil.drawTextClipped(
                        buf = ctx.buffer,
                        x = x + 8,
                        y = cy,
                        maxW = (w - 16).coerceAtLeast(1),
                        text = trimmed.removePrefix("# "),
                        color = ctx.accent,
                        scale = 1,
                        bold = true
                    )
                    JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x + 8, x + w - 8, cy + 9, ctx.borderSubtle)
                    cy += 14
                }
                trimmed.startsWith("## ") || trimmed.startsWith("### ") -> {
                    val clean = trimmed.trimStart('#', ' ')
                    JestElectricControlTintRenderUtil.drawTextClipped(
                        buf = ctx.buffer,
                        x = x + 8,
                        y = cy,
                        maxW = (w - 16).coerceAtLeast(1),
                        text = clean,
                        color = baseFg,
                        scale = 1,
                        bold = true
                    )
                    cy += 12
                }
                trimmed.startsWith("> ") -> {
                    JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 8, cy, 2, 9, ctx.accent)
                    JestElectricControlTintRenderUtil.drawTextClipped(
                        buf = ctx.buffer,
                        x = x + 14,
                        y = cy,
                        maxW = (w - 22).coerceAtLeast(1),
                        text = trimmed.removePrefix("> "),
                        color = ctx.textSecondary,
                        scale = 1,
                        italic = true
                    )
                    cy += 11
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    JestElectricControlTintRenderUtil.fillCircle(ctx.buffer, x + 11, cy + 3, 2, ctx.accent)
                    JestElectricControlTintRenderUtil.drawTextClipped(
                        buf = ctx.buffer,
                        x = x + 18,
                        y = cy,
                        maxW = (w - 26).coerceAtLeast(1),
                        text = trimmed.substring(2).replace("**", "").replace("`", ""),
                        color = baseFg,
                        scale = 1
                    )
                    cy += 11
                }
                else -> {
                    val clean = trimmed.replace("**", "").replace("`", "")
                    JestElectricControlTintRenderUtil.drawTextClipped(
                        buf = ctx.buffer,
                        x = x + 8,
                        y = cy,
                        maxW = (w - 16).coerceAtLeast(1),
                        text = clean,
                        color = baseFg,
                        scale = 1
                    )
                    cy += 11
                }
            }
        }
    }

    // =========================================================================
    // Typography & Text Color Property Resolution Helpers
    // =========================================================================

    fun resolveTextColor(
        ctx: JestElectricRenderContext,
        comp: AbstractComponent,
        fallback: RgbaColor
    ): RgbaColor {
        if (!comp.isEnabled) return ctx.textMuted
        comp.style.color?.let { return it }
        comp.computedStyle?.textColor?.let { return it }
        val textType = comp.textColorType ?: comp.bootstrap.textColorType
        if (textType != null && textType != BootstrapType.NONE) {
            return ctx.resolveBootstrapColor(textType)
        }
        return fallback
    }

    fun resolveScale(comp: AbstractComponent, defaultScale: Int = 1): Int {
        val stylePx = comp.style.fontSize?.resolvePx() ?: comp.computedStyle?.fontSizePx
        if (stylePx != null) {
            return if (stylePx >= 22f) 2 else 1
        }
        val bsFontSize = comp.bootstrap.fontSize
        if (bsFontSize != null) {
            return when (bsFontSize) {
                BootstrapFontSize.FS_1, BootstrapFontSize.FS_2, BootstrapFontSize.FS_3 -> 2
                else -> 1
            }
        }
        return defaultScale
    }

    fun resolveBold(comp: AbstractComponent, defaultBold: Boolean = false): Boolean {
        val numeric = comp.style.fontWeightNumeric ?: comp.computedStyle?.fontWeightNumeric
        if (numeric != null) return numeric >= 600
        val weight = comp.style.fontWeight ?: comp.bootstrap.fontWeight ?: comp.fontWeight
        if (weight != null) {
            return weight == FontWeight.SEMIBOLD || weight == FontWeight.BOLD || weight == FontWeight.BOLDER
        }
        return defaultBold
    }

    fun resolveItalic(comp: AbstractComponent): Boolean {
        val fs = comp.style.fontStyle ?: comp.computedStyle?.fontStyle
        return fs == CssFontStyle.ITALIC || fs == CssFontStyle.OBLIQUE
    }

    fun resolveLetterSpacingPx(comp: AbstractComponent): Int {
        val px = comp.style.letterSpacing?.resolvePx() ?: comp.computedStyle?.letterSpacing?.resolvePx()
        val fontBonus = if (comp.style.fontFamily?.toCssString()?.contains("monospace", ignoreCase = true) == true ||
            comp.computedStyle?.fontFamily?.contains("monospace", ignoreCase = true) == true
        ) {
            1
        } else {
            0
        }
        return ((px?.roundToInt() ?: 0) + fontBonus).coerceIn(-1, 8)
    }

    fun resolveLineHeightPx(comp: AbstractComponent, scale: Int): Int? {
        val px = comp.style.lineHeight?.resolvePx(referencePx = 14f * scale)
            ?: comp.computedStyle?.lineHeight?.resolvePx(referencePx = 14f * scale)
        return px?.roundToInt()?.coerceAtLeast(8 * scale)
    }

    fun resolveTextDecoration(comp: AbstractComponent): CssTextDecoration =
        comp.style.textDecoration ?: comp.computedStyle?.textDecoration ?: CssTextDecoration.NONE

    fun applyTextTransform(raw: String, comp: AbstractComponent): String {
        val transform = comp.style.textTransform
            ?: comp.computedStyle?.textTransform
            ?: comp.bootstrap.textTransform
            ?: comp.textTransform
            ?: TextTransform.NONE
        return when (transform) {
            TextTransform.NONE -> raw
            TextTransform.UPPERCASE -> raw.uppercase()
            TextTransform.LOWERCASE -> raw.lowercase()
            TextTransform.CAPITALIZE -> raw.split(' ').joinToString(" ") { word ->
                if (word.isEmpty()) word else word[0].uppercaseChar() + word.substring(1)
            }
        }
    }

    fun resolveAlignedX(x: Int, w: Int, textW: Int, comp: AbstractComponent, pad: Int = 2): Int {
        val align = comp.style.textAlign
            ?: comp.bootstrap.textAlignment
            ?: comp.textAlignment
            ?: comp.computedStyle?.textAlign?.let { TextAlignment.fromCssClass("text-$it") }
            ?: TextAlignment.START
        return when (align) {
            TextAlignment.CENTER -> (x + (w - textW) / 2).coerceAtLeast(x + pad)
            TextAlignment.END -> (x + w - textW - pad).coerceAtLeast(x + pad)
            TextAlignment.START, TextAlignment.JUSTIFY -> x + pad
        }
    }

    fun isNoWrap(comp: AbstractComponent): Boolean {
        val overflow = comp.style.textOverflow
            ?: comp.computedStyle?.textOverflow
            ?: comp.bootstrap.textOverflow
        if (overflow == TextOverflow.NOWRAP || overflow == TextOverflow.ELLIPSIS) return true
        val ws = comp.style.whiteSpace
            ?: comp.computedStyle?.whiteSpace?.let { CssWhiteSpace.fromCss(it) }
        if (ws == CssWhiteSpace.NOWRAP) return true
        val wb = comp.style.wordBreak ?: comp.computedStyle?.wordBreak
        return wb == CssWordBreak.KEEP_ALL
    }

    fun shouldEllipsize(comp: AbstractComponent): Boolean {
        val overflow = comp.style.textOverflow
            ?: comp.computedStyle?.textOverflow
            ?: comp.bootstrap.textOverflow
        return overflow != TextOverflow.WRAP
    }

    private fun drawTextDecorationLine(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        textW: Int,
        scale: Int,
        color: RgbaColor,
        decoration: CssTextDecoration
    ) {
        if (textW <= 0 || decoration == CssTextDecoration.NONE) return
        when (decoration) {
            CssTextDecoration.UNDERLINE ->
                JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + textW, y + 7 * scale + 1, color)
            CssTextDecoration.OVERLINE ->
                JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + textW, y - 1, color)
            CssTextDecoration.LINE_THROUGH ->
                JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + textW, y + (7 * scale) / 2, color)
            CssTextDecoration.NONE -> Unit
        }
    }
}
