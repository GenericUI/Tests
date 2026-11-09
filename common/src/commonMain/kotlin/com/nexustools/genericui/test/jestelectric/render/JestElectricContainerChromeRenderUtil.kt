package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.action.Toolbar
import com.nexustools.genericui.component.content.Table
import com.nexustools.genericui.component.dock.DockWorkspace
import com.nexustools.genericui.component.feedback.Alert
import com.nexustools.genericui.component.feedback.ProgressBar
import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.component.form.NumberField
import com.nexustools.genericui.component.form.Select
import com.nexustools.genericui.component.form.Slider
import com.nexustools.genericui.component.form.TextArea
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.fragment.FragmentContainer
import com.nexustools.genericui.component.fragment.helper.DockWorkspaceFragment
import com.nexustools.genericui.component.fragment.helper.TabbedFragment
import com.nexustools.genericui.component.layout.ColumnContainer
import com.nexustools.genericui.component.layout.Divider
import com.nexustools.genericui.component.layout.FlexBox
import com.nexustools.genericui.component.layout.HStack
import com.nexustools.genericui.component.layout.ScrollView
import com.nexustools.genericui.component.layout.StyledContainer
import com.nexustools.genericui.component.layout.VStack
import com.nexustools.genericui.component.lazy.LazyGrid
import com.nexustools.genericui.component.lazy.LazyGridTree
import com.nexustools.genericui.component.lazy.LazyList
import com.nexustools.genericui.component.lazy.TreeView
import com.nexustools.genericui.component.shell.MdiContainer
import com.nexustools.genericui.component.shell.MenuBar
import com.nexustools.genericui.component.shell.SplitPane
import com.nexustools.genericui.component.surface.Accordion
import com.nexustools.genericui.component.surface.AccordionItem
import com.nexustools.genericui.component.surface.Card
import com.nexustools.genericui.component.surface.CollapsiblePanel
import com.nexustools.genericui.component.system.StatusBar
import com.nexustools.genericui.component.tabs.TabContainer
import com.nexustools.genericui.component.typography.Badge
import com.nexustools.genericui.component.typography.Heading
import com.nexustools.genericui.component.typography.Label
import com.nexustools.genericui.component.typography.MarkdownView
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.AbstractContainer
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.bootstrap.FlexDirection
import com.nexustools.genericui.enums.bootstrap.FlexWrap
import com.nexustools.genericui.style.color.RgbaColor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders layout and surface containers ([StyledContainer], [Card], [CollapsiblePanel],
 * [Accordion], [AccordionItem], [Alert], [Divider], [ColumnContainer], [HStack], [VStack],
 * [FlexBox], [FragmentContainer], and generic [AbstractContainer] instances).
 *
 * - On non-web system targets: renders native system surface or [BootstrapType] tint on standard
 *   containers while ignoring aesthetic `ComponentStyle` overrides (`backgroundColor`, `borderColor`,
 *   `borderRadiusPx`, `boxShadow`).
 * - Honors full `ComponentStyle` background/border/radius/shadow ONLY on [StyledContainer], `:web`,
 *   or when [JestElectricRenderContext.isHostedInCustomStyleEngine] is `true`.
 * - Honors `FlexWrap.WRAP` (`comp.bootstrap.flexWrap == FlexWrap.WRAP || comp.style.flexWrap == FlexWrap.WRAP`)
 *   to wrap flex children across multiple lines during both layout measurement and placement.
 */
object JestElectricContainerChromeRenderUtil {

    fun canRender(comp: AbstractComponent): Boolean =
        comp is Divider ||
            comp is Alert ||
            comp is Card ||
            comp is CollapsiblePanel ||
            comp is Accordion ||
            comp is AccordionItem ||
            comp is ColumnContainer ||
            comp is FragmentContainer ||
            comp is StyledContainer ||
            comp is HStack ||
            comp is VStack ||
            comp is FlexBox ||
            (comp is AbstractContainer && !JestElectricMediaCanvasRenderUtil.isExtensionCanvasOrSurface(comp))

    fun render(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: AbstractComponent,
        depth: Int
    ): Boolean {
        if (w <= 0 || h <= 0) return true
        return when (comp) {
            is Divider -> {
                renderDivider(ctx, x, y, w, h, comp)
                true
            }
            is Alert -> {
                renderAlert(ctx, x, y, w, h, comp)
                true
            }
            is Card -> {
                renderCard(ctx, x, y, w, h, comp, depth)
                true
            }
            is AccordionItem -> {
                renderCollapsiblePanel(ctx, x, y, w, h, comp, depth)
                true
            }
            is CollapsiblePanel -> {
                renderCollapsiblePanel(ctx, x, y, w, h, comp, depth)
                true
            }
            is Accordion -> {
                renderContainerChromeBackgroundAndBorder(ctx, x, y, w, h, comp)
                renderVerticalChildren(ctx, x, y, w, h, comp.children, depth + 1, gap = 6)
                true
            }
            is ColumnContainer -> {
                renderContainerChromeBackgroundAndBorder(ctx, x, y, w, h, comp)
                renderColumnContainer(ctx, x, y, w, h, comp, depth)
                true
            }
            is FragmentContainer -> {
                comp.currentFragment?.let { activeFrag ->
                    ctx.drawChild(x, y, w, h, activeFrag, depth + 1)
                }
                true
            }
            is HStack -> {
                renderContainerChromeBackgroundAndBorder(ctx, x, y, w, h, comp)
                val pad = resolveContainerInset(comp)
                val gap = comp.gapPx.coerceIn(2, 20)
                val wrap = shouldWrapFlexChildren(comp)
                renderHorizontalChildren(
                    ctx = ctx,
                    x = x + pad,
                    y = y + pad,
                    w = (w - pad * 2).coerceAtLeast(1),
                    h = (h - pad * 2).coerceAtLeast(1),
                    children = comp.children,
                    depth = depth + 1,
                    gap = gap,
                    wrap = wrap
                )
                true
            }
            is VStack -> {
                renderContainerChromeBackgroundAndBorder(ctx, x, y, w, h, comp)
                val pad = resolveContainerInset(comp)
                val gap = comp.gapPx.coerceIn(2, 20)
                renderVerticalChildren(
                    ctx = ctx,
                    x = x + pad,
                    y = y + pad,
                    w = (w - pad * 2).coerceAtLeast(1),
                    h = (h - pad * 2).coerceAtLeast(1),
                    children = comp.children,
                    depth = depth + 1,
                    gap = gap
                )
                true
            }
            is FlexBox -> {
                renderContainerChromeBackgroundAndBorder(ctx, x, y, w, h, comp)
                val pad = resolveContainerInset(comp)
                val gap = (comp.style.gap?.resolvePx()?.roundToInt()
                    ?: comp.computedStyle?.gap?.resolvePx()?.roundToInt()
                    ?: 6).coerceIn(2, 24)
                val dir = comp.flexDirection
                val wrap = shouldWrapFlexChildren(comp)
                if (dir == FlexDirection.ROW || dir == FlexDirection.ROW_REVERSE) {
                    renderHorizontalChildren(
                        ctx = ctx,
                        x = x + pad,
                        y = y + pad,
                        w = (w - pad * 2).coerceAtLeast(1),
                        h = (h - pad * 2).coerceAtLeast(1),
                        children = comp.children,
                        depth = depth + 1,
                        gap = gap,
                        wrap = wrap
                    )
                } else {
                    renderVerticalChildren(
                        ctx = ctx,
                        x = x + pad,
                        y = y + pad,
                        w = (w - pad * 2).coerceAtLeast(1),
                        h = (h - pad * 2).coerceAtLeast(1),
                        children = comp.children,
                        depth = depth + 1,
                        gap = gap
                    )
                }
                true
            }
            is AbstractContainer -> {
                renderContainerChromeBackgroundAndBorder(ctx, x, y, w, h, comp)
                val pad = resolveContainerInset(comp)
                val gap = (comp.style.gap?.resolvePx()?.roundToInt()
                    ?: comp.computedStyle?.gap?.resolvePx()?.roundToInt()
                    ?: 6).coerceIn(2, 24)
                val isRow = comp.flexDirection == FlexDirection.ROW ||
                    comp.style.flexDirection == FlexDirection.ROW ||
                    comp.bootstrap.flexDirection == FlexDirection.ROW ||
                    comp.hasClass("flex-row") ||
                    (comp.hasClass("d-flex") && !comp.hasClass("flex-column") &&
                        comp.children.size > 1 && comp.children.all { !isExpandingVerticalComponent(it) })
                val wrap = shouldWrapFlexChildren(comp)
                if (isRow) {
                    renderHorizontalChildren(
                        ctx = ctx,
                        x = x + pad,
                        y = y + pad,
                        w = (w - pad * 2).coerceAtLeast(1),
                        h = (h - pad * 2).coerceAtLeast(1),
                        children = comp.children,
                        depth = depth + 1,
                        gap = gap,
                        wrap = wrap
                    )
                } else {
                    renderVerticalChildren(
                        ctx = ctx,
                        x = x + pad,
                        y = y + pad,
                        w = (w - pad * 2).coerceAtLeast(1),
                        h = (h - pad * 2).coerceAtLeast(1),
                        children = comp.children,
                        depth = depth + 1,
                        gap = gap
                    )
                }
                true
            }
            else -> false
        }
    }

    /**
     * Renders container background and border according to GenericUI's system vs styled container rules:
     * - Standard containers on non-web system targets ignore `ComponentStyle` aesthetic overrides and
     *   only tint via `BootstrapType` (`backgroundType`, `borderType`).
     * - [StyledContainer], `:web`, and custom style engine subtrees honor full `ComponentStyle`
     *   `backgroundColor`, `borderColor`, and `borderWidthPx`.
     */
    fun renderContainerChromeBackgroundAndBorder(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: AbstractComponent
    ) {
        val allowFullStyle = ctx.allowsFullComponentStyleOnContainer(comp)
        val bgType = comp.backgroundType?.takeIf { it != BootstrapType.NONE }
            ?: comp.bootstrap.backgroundType?.takeIf { it != BootstrapType.NONE }
        val borderType = comp.borderType?.takeIf { it != BootstrapType.NONE }
            ?: comp.bootstrap.borderType?.takeIf { it != BootstrapType.NONE }

        val bgColor: RgbaColor? = when {
            allowFullStyle -> comp.style.backgroundColor
                ?: comp.computedStyle?.backgroundColor
                ?: bgType?.let { ctx.resolveBootstrapColor(it) }
            bgType != null -> ctx.resolveBootstrapColor(bgType)
            else -> null
        }

        if (bgColor != null && bgColor.a > 0) {
            if (bgColor.a >= 250) {
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, bgColor)
            } else {
                JestElectricControlTintRenderUtil.fillRectAlpha(ctx.buffer, x, y, w, h, bgColor)
            }
        }

        val borderColor: RgbaColor? = when {
            allowFullStyle -> comp.style.borderColor
                ?: comp.computedStyle?.borderColor
                ?: borderType?.let { ctx.resolveBootstrapColor(it) }
            borderType != null -> ctx.resolveBootstrapColor(borderType)
            else -> null
        }

        if (borderColor != null && borderColor.a > 0) {
            val thickness = if (allowFullStyle) {
                (comp.style.borderWidthPx ?: comp.computedStyle?.borderWidthPx ?: 1f).roundToInt().coerceIn(1, 4)
            } else {
                1
            }
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, borderColor, thickness)
        }
    }

    private fun renderDivider(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Divider
    ) {
        val color = comp.borderType?.takeIf { it != BootstrapType.NONE }?.let { ctx.resolveBootstrapColor(it) }
            ?: ctx.borderSubtle
        val lineY = y + h / 2
        JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x + 2, x + w - 2, lineY, color)
    }

    private fun renderCard(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        card: Card,
        depth: Int
    ) {
        val bgType = card.backgroundType?.takeIf { it != BootstrapType.NONE }
            ?: card.type.takeIf { it != BootstrapType.NONE }
        val bg = when {
            ctx.allowsFullComponentStyleOnContainer(card) && card.style.backgroundColor != null ->
                card.style.backgroundColor!!
            bgType != null -> ctx.resolveBootstrapColor(bgType)
            else -> ctx.bgCard
        }
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, bg)

        val borderColor = when {
            ctx.allowsFullComponentStyleOnContainer(card) && card.style.borderColor != null ->
                card.style.borderColor!!
            card.borderType != null && card.borderType != BootstrapType.NONE ->
                ctx.resolveBootstrapColor(card.borderType)
            else -> ctx.borderSubtle
        }
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, borderColor, 1)
        renderVerticalChildren(ctx, x + 8, y + 6, w - 16, h - 12, card.children, depth + 1, gap = 5)
    }

    private fun renderCollapsiblePanel(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        panel: CollapsiblePanel,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        val headerH = 26.coerceAtMost(h)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, headerH, ctx.bgElevated)
        val arrow = if (panel.isExpanded) "v" else ">"
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = x + 8,
            y = y + 9,
            maxW = (w - 16).coerceAtLeast(1),
            text = "$arrow ${panel.title}",
            color = ctx.textPrimary,
            scale = 1
        )
        if (panel.isExpanded && h > headerH + 6) {
            renderVerticalChildren(
                ctx = ctx,
                x = x + 6,
                y = y + headerH + 4,
                w = w - 12,
                h = h - headerH - 8,
                children = panel.bodyContainer.children,
                depth = depth + 1,
                gap = 4
            )
        }
    }

    private fun renderAlert(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        alert: Alert
    ) {
        val accent = ctx.resolveBootstrapColor(alert.type)
        val bg = accent.blendOverSurface(ctx.bgCard, 0.22f)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, bg)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, accent, 1)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, 4, h, accent)
        JestElectricControlTintRenderUtil.drawWrappedText(
            buf = ctx.buffer,
            x = x + 10,
            y = y + (h - 8).coerceAtLeast(4) / 2,
            maxW = (w - 16).coerceAtLeast(1),
            maxH = (h - 4).coerceAtLeast(8),
            text = alert.messageText.text,
            color = ctx.textPrimary,
            scale = 1
        )
    }

    private fun renderColumnContainer(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        row: ColumnContainer,
        depth: Int
    ) {
        val cols = row.columns.filter { it.isVisible }
        if (cols.isEmpty()) {
            renderHorizontalChildren(
                ctx = ctx,
                x = x,
                y = y,
                w = w,
                h = h,
                children = row.children,
                depth = depth + 1,
                gap = 8,
                wrap = shouldWrapFlexChildren(row)
            )
            return
        }
        val gap = 8
        var curX = x
        val totalSpan = cols.sumOf {
            (it.lg ?: it.md ?: it.span ?: (12 / cols.size.coerceAtLeast(1))).coerceIn(1, 12)
        }.coerceAtLeast(12)
        for ((idx, col) in cols.withIndex()) {
            val span = (col.lg ?: itSpanFallback(col.md, col.span, cols.size)).coerceIn(1, 12)
            val colW = if (idx == cols.lastIndex) {
                (x + w - curX).coerceAtLeast(30)
            } else {
                ((w - gap * (cols.size - 1)) * span / totalSpan).coerceAtLeast(30)
            }
            ctx.drawChild(curX, y, colW, h, col, depth + 1)
            curX += colW + gap
        }
    }

    private fun itSpanFallback(md: Int?, span: Int?, count: Int): Int =
        md ?: span ?: (12 / count.coerceAtLeast(1))

    fun shouldWrapFlexChildren(comp: AbstractComponent): Boolean =
        comp.bootstrap.flexWrap == FlexWrap.WRAP ||
            comp.bootstrap.flexWrap == FlexWrap.WRAP_REVERSE ||
            comp.style.flexWrap == FlexWrap.WRAP ||
            comp.style.flexWrap == FlexWrap.WRAP_REVERSE ||
            comp.computedStyle?.flexWrap == FlexWrap.WRAP ||
            comp.computedStyle?.flexWrap == FlexWrap.WRAP_REVERSE ||
            comp.flexWrap == FlexWrap.WRAP ||
            comp.flexWrap == FlexWrap.WRAP_REVERSE ||
            comp.hasClass("flex-wrap")

    private fun resolveContainerInset(comp: AbstractComponent): Int {
        if (comp is StyledContainer || comp.padding.top > 0 || comp.padding.left > 0) {
            return max(comp.padding.left, comp.padding.top).coerceIn(0, 16)
        }
        return 0
    }

    /**
     * Renders horizontal flex/stack children, supporting multi-line row wrapping when [wrap] is `true`
     * (`FlexWrap.WRAP`).
     */
    fun renderHorizontalChildren(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        children: List<AbstractComponent>,
        depth: Int,
        gap: Int,
        wrap: Boolean = false
    ) {
        val visible = children.filter { it.isVisible && !it.mountedInTitleBar }
        if (visible.isEmpty()) return

        if (wrap) {
            val lines = partitionWrappedLines(visible, w, gap)
            if (lines.size > 1) {
                var curY = y
                val defaultLineH = ((h - gap * (lines.size - 1)) / lines.size).coerceIn(22, 42)
                for (line in lines) {
                    if (curY >= y + h - 4) break
                    val lineH = line.maxOfOrNull { estimateInlineHeight(it, defaultLineH) }
                        ?.coerceIn(20, (y + h - curY).coerceAtLeast(20))
                        ?: defaultLineH
                    var curX = x
                    for (child in line) {
                        val cw = estimateInlineWidth(child).coerceAtMost((x + w - curX).coerceAtLeast(16))
                        if (curX + 8 > x + w) break
                        val ch = estimateInlineHeight(child, lineH).coerceAtMost(lineH)
                        val cy = curY + (lineH - ch) / 2
                        ctx.drawChild(curX, cy, cw, ch, child, depth + 1)
                        curX += cw + gap
                    }
                    curY += lineH + gap
                }
                return
            }
        }

        // Single-line horizontal layout: measure natural widths and distribute remaining width to expanding children
        val widths = IntArray(visible.size)
        val expanding = BooleanArray(visible.size)
        var fixedSum = 0
        var expandCount = 0

        for ((i, child) in visible.withIndex()) {
            val isExp = child is SplitPane || child is TabContainer || child is MdiContainer ||
                child is DockWorkspace || child is ScrollView || child is LazyList ||
                child is LazyGrid || child is TreeView<*> || child is Table ||
                child is Card || child is VStack ||
                JestElectricMediaCanvasRenderUtil.isExtensionCanvasOrSurface(child) ||
                (child is TextField && child !is NumberField && visible.size <= 5)
            if (isExp) {
                expanding[i] = true
                expandCount++
            } else {
                val est = estimateInlineWidth(child)
                widths[i] = est
                fixedSum += est
            }
        }

        val totalGaps = gap * (visible.size - 1)
        val availForExpand = (w - fixedSum - totalGaps).coerceAtLeast(0)
        if (expandCount > 0) {
            val each = (availForExpand / expandCount).coerceAtLeast(36)
            for (i in visible.indices) {
                if (expanding[i]) widths[i] = each
            }
        } else if (fixedSum + totalGaps > w && fixedSum > 0) {
            val scale = (w - totalGaps).coerceAtLeast(visible.size * 20).toFloat() / fixedSum.toFloat()
            for (i in visible.indices) {
                widths[i] = (widths[i] * scale).roundToInt().coerceAtLeast(20)
            }
        }

        var curX = x
        for ((i, child) in visible.withIndex()) {
            if (curX >= x + w - 4) break
            val cw = widths[i].coerceAtMost(x + w - curX)
            val ch = estimateInlineHeight(child, h).coerceAtMost(h)
            val isExpVert = isExpandingVerticalComponent(child)
            val cy = if (ch < h && !isExpVert) y + (h - ch) / 2 else y
            ctx.drawChild(curX, cy, cw, if (isExpVert) h else ch, child, depth + 1)
            curX += cw + gap
        }
    }

    private fun partitionWrappedLines(
        visible: List<AbstractComponent>,
        availW: Int,
        gap: Int
    ): List<List<AbstractComponent>> {
        val lines = mutableListOf<MutableList<AbstractComponent>>()
        var currentLine = mutableListOf<AbstractComponent>()
        var currentW = 0
        val maxW = availW.coerceAtLeast(60)
        for (child in visible) {
            val cw = estimateInlineWidth(child).coerceAtMost(maxW)
            val needed = if (currentLine.isEmpty()) cw else cw + gap
            if (currentLine.isNotEmpty() && currentW + needed > maxW) {
                lines.add(currentLine)
                currentLine = mutableListOf(child)
                currentW = cw
            } else {
                currentLine.add(child)
                currentW += needed
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    fun renderVerticalChildren(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        children: List<AbstractComponent>,
        depth: Int,
        gap: Int
    ) {
        val visible = children.filter { it.isVisible && !it.mountedInTitleBar }
        if (visible.isEmpty()) return

        val heights = IntArray(visible.size)
        val expanding = BooleanArray(visible.size)
        var fixedSum = 0
        var expandCount = 0

        for ((i, child) in visible.withIndex()) {
            if (isExpandingVerticalComponent(child)) {
                expanding[i] = true
                expandCount++
            } else {
                val est = estimateNaturalHeight(child, w)
                heights[i] = est
                fixedSum += est
            }
        }

        val totalGaps = gap * (visible.size - 1)
        val remaining = h - fixedSum - totalGaps
        if (expandCount > 0 && remaining > expandCount * 48) {
            val each = remaining / expandCount
            for (i in visible.indices) {
                if (expanding[i]) heights[i] = each
            }
        } else {
            for (i in visible.indices) {
                if (expanding[i]) {
                    heights[i] = (remaining / expandCount.coerceAtLeast(1)).coerceIn(64, (h / 2).coerceAtLeast(64))
                }
            }
        }

        var curY = y
        for ((i, child) in visible.withIndex()) {
            if (curY >= y + h - 4) break
            val ch = if (i == visible.lastIndex && expanding[i]) {
                (y + h - curY).coerceAtLeast(20)
            } else {
                heights[i].coerceAtMost(y + h - curY)
            }
            ctx.drawChild(x, curY, w, ch, child, depth + 1)
            curY += ch + gap
        }
    }

    fun isExpandingVerticalComponent(comp: AbstractComponent): Boolean {
        if (comp is DockWorkspace || comp is DockWorkspaceFragment ||
            comp is MdiContainer || comp is TabContainer || comp is TabbedFragment ||
            comp is SplitPane || comp is ColumnContainer || comp is ScrollView ||
            comp is LazyGridTree<*> || comp is LazyList || comp is LazyGrid ||
            comp is TreeView<*> || comp is Table || comp is MarkdownView ||
            comp is TextArea || JestElectricMediaCanvasRenderUtil.isExtensionCanvasOrSurface(comp)
        ) {
            return true
        }
        if (comp is AbstractContainer && comp !is HStack && comp !is Toolbar && comp !is StatusBar && comp !is MenuBar) {
            return comp.children.any { isExpandingVerticalComponent(it) }
        }
        return false
    }

    fun estimateInlineWidth(comp: AbstractComponent): Int {
        comp.widthPx?.takeIf { it > 0 }?.let { return it }
        comp.style.width?.resolvePx()?.roundToInt()?.takeIf { it > 0 }?.let { return it }
        return when (comp) {
            is Button -> (JestElectricControlTintRenderUtil.measureTextWidth(comp.text.ifEmpty { "Btn" }, 1) + 22).coerceIn(32, 220)
            is Badge -> (JestElectricControlTintRenderUtil.measureTextWidth(comp.text, 1) + 18).coerceIn(36, 220)
            is Heading -> (JestElectricControlTintRenderUtil.measureTextWidth(comp.text, if (comp.level <= 3) 2 else 1) + 12).coerceIn(40, 340)
            is Label -> (JestElectricControlTintRenderUtil.measureTextWidth(comp.text, 1) + 10).coerceIn(28, 280)
            is TextField -> 160
            is Select -> 130
            is CheckBox -> (JestElectricControlTintRenderUtil.measureTextWidth(comp.label, 1) + 34).coerceIn(48, 220)
            is Slider -> 130
            is ProgressBar -> 140
            else -> 110
        }
    }

    fun estimateInlineHeight(comp: AbstractComponent, availH: Int): Int {
        comp.heightPx?.takeIf { it > 0 }?.let { return it.coerceAtMost(availH) }
        comp.style.height?.resolvePx()?.roundToInt()?.takeIf { it > 0 }?.let { return it.coerceAtMost(availH) }
        return when (comp) {
            is Button, is TextField, is Select -> 26
            is Badge, is CheckBox, is Slider, is ProgressBar -> 22
            is Heading -> 24
            is Label -> 18
            else -> min(availH, 28)
        }
    }

    fun estimateNaturalHeight(comp: AbstractComponent, availW: Int): Int {
        comp.heightPx?.takeIf { it > 0 }?.let { return it }
        comp.style.height?.resolvePx()?.roundToInt()?.takeIf { it > 0 }?.let { return it }
        return when (comp) {
            is Divider -> 8
            is Heading -> if (comp.level <= 2) 26 else 20
            is Badge -> 22
            is Label -> {
                val charsPerLine = ((availW - 8) / 6).coerceAtLeast(20)
                val lines = ((comp.text.length + charsPerLine - 1) / charsPerLine).coerceIn(1, 3)
                lines * 12 + 4
            }
            is Button, is TextField, is Select, is CheckBox, is Slider -> 28
            is ProgressBar -> 20
            is Alert -> 34
            is Toolbar, is StatusBar, is MenuBar -> 34
            is HStack -> {
                if (shouldWrapFlexChildren(comp)) {
                    val vis = comp.children.filter { it.isVisible && !it.mountedInTitleBar }
                    val gap = comp.gapPx.coerceIn(2, 20)
                    val lines = partitionWrappedLines(vis, availW, gap)
                    (lines.size * 28 + (lines.size - 1).coerceAtLeast(0) * gap + 6).coerceIn(34, 240)
                } else {
                    34
                }
            }
            is FlexBox -> {
                if (comp.flexDirection == FlexDirection.ROW || comp.flexDirection == FlexDirection.ROW_REVERSE) {
                    if (shouldWrapFlexChildren(comp)) {
                        val vis = comp.children.filter { it.isVisible && !it.mountedInTitleBar }
                        val lines = partitionWrappedLines(vis, availW, 6)
                        (lines.size * 28 + (lines.size - 1).coerceAtLeast(0) * 6 + 6).coerceIn(34, 240)
                    } else {
                        34
                    }
                } else {
                    68
                }
            }
            is CollapsiblePanel -> if (comp.isExpanded) 90 else 28
            is Card -> {
                val vis = comp.children.filter { it.isVisible }
                val sum = vis.sumOf { estimateNaturalHeight(it, availW - 16) + 5 }
                (sum + 14).coerceIn(42, 220)
            }
            is AbstractContainer -> {
                val vis = comp.children.filter { it.isVisible }
                if (vis.isEmpty()) 28
                else (vis.sumOf { estimateNaturalHeight(it, availW - 8) + 4 } + 8).coerceIn(30, 220)
            }
            else -> 28
        }
    }
}
