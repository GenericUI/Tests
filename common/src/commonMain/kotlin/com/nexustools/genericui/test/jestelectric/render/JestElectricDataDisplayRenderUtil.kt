package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.content.Avatar
import com.nexustools.genericui.component.content.ImageView
import com.nexustools.genericui.component.content.Table
import com.nexustools.genericui.component.lazy.LazyColumn
import com.nexustools.genericui.component.lazy.LazyGrid
import com.nexustools.genericui.component.lazy.LazyGridTree
import com.nexustools.genericui.component.lazy.LazyList
import com.nexustools.genericui.component.lazy.LazyRow
import com.nexustools.genericui.component.lazy.TreeNode
import com.nexustools.genericui.component.lazy.TreeView
import com.nexustools.genericui.component.overlay.Popover
import com.nexustools.genericui.component.overlay.Tooltip
import com.nexustools.genericui.core.AbstractComponent
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders data display and collection components ([Table] with proportional column weights/widths,
 * [LazyGridTree], [TreeView], [LazyList], [LazyColumn], [LazyRow], [LazyGrid] with adaptive
 * `minColumnWidthPx`, [ImageView], [Avatar], [Popover], [Tooltip]).
 */
object JestElectricDataDisplayRenderUtil {

    fun canRender(comp: AbstractComponent): Boolean =
        comp is Table ||
            comp is LazyGridTree<*> ||
            comp is LazyList ||
            comp is LazyColumn ||
            comp is LazyRow ||
            comp is LazyGrid ||
            comp is TreeView<*> ||
            comp is Avatar ||
            comp is ImageView ||
            comp is Tooltip ||
            comp is Popover

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
            is Table -> {
                renderTable(ctx, x, y, w, h, comp)
                true
            }
            is LazyGridTree<*> -> {
                renderLazyGridTree(ctx, x, y, w, h, comp, depth)
                true
            }
            is LazyRow -> {
                renderLazyRow(ctx, x, y, w, h, comp, depth)
                true
            }
            is LazyList -> {
                renderLazyList(ctx, x, y, w, h, comp, depth)
                true
            }
            is LazyGrid -> {
                renderLazyGrid(ctx, x, y, w, h, comp, depth)
                true
            }
            is TreeView<*> -> {
                renderTreeView(ctx, x, y, w, h, comp)
                true
            }
            is Avatar -> {
                renderAvatar(ctx, x, y, w, h, comp)
                true
            }
            is ImageView -> {
                renderImageView(ctx, x, y, w, h, comp)
                true
            }
            is Tooltip -> {
                renderTooltip(ctx, x, y, w, h, comp)
                true
            }
            is Popover -> {
                renderPopover(ctx, x, y, w, h, comp, depth)
                true
            }
            else -> false
        }
    }

    private fun renderTable(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        table: Table
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        val cols = table.columns
        val colCount = cols.size.coerceAtLeast(table.rows.firstOrNull()?.cells?.size ?: 1).coerceAtLeast(1)
        val colWidths = computeTableColumnWidths(table, colCount, w)
        val rowH = 22
        var curY = y

        if (cols.isNotEmpty()) {
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 1, curY + 1, w - 2, rowH, ctx.bgElevated)
            JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + w, curY + rowH, ctx.borderStrong)
            var cx = x
            for ((cIdx, col) in cols.withIndex()) {
                val cw = colWidths.getOrElse(cIdx) { (w / colCount).coerceAtLeast(32) }
                JestElectricControlTintRenderUtil.drawTextClipped(
                    buf = ctx.buffer,
                    x = cx + 6,
                    y = curY + 7,
                    maxW = (cw - 10).coerceAtLeast(1),
                    text = col.headerText,
                    color = ctx.textPrimary,
                    scale = 1,
                    bold = true
                )
                if (cIdx < cols.lastIndex) {
                    JestElectricControlTintRenderUtil.drawVLine(ctx.buffer, cx + cw, curY + 1, curY + rowH, ctx.borderSubtle)
                }
                cx += cw
            }
            curY += rowH + 1
        }

        for ((rIdx, row) in table.rows.withIndex()) {
            if (curY + rowH > y + h) break
            if (table.isStriped && rIdx % 2 == 1) {
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 1, curY, w - 2, rowH, ctx.bgSurface)
            }
            JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x + 1, x + w - 1, curY + rowH, ctx.borderSubtle)
            var cx = x
            for ((cIdx, cell) in row.cells.withIndex()) {
                if (cIdx >= colCount) break
                val cw = colWidths.getOrElse(cIdx) { (w / colCount).coerceAtLeast(32) }
                JestElectricControlTintRenderUtil.drawTextClipped(
                    buf = ctx.buffer,
                    x = cx + 6,
                    y = curY + 7,
                    maxW = (cw - 10).coerceAtLeast(1),
                    text = cell.text,
                    color = ctx.textSecondary,
                    scale = 1
                )
                cx += cw
            }
            curY += rowH
        }
    }

    /**
     * Resolves proportional column widths for [table], honoring explicit `col.widthPx`,
     * `data-column-weights` attribute, or proportional header/cell content weights.
     */
    private fun computeTableColumnWidths(table: Table, colCount: Int, totalWidth: Int): IntArray {
        val weights = FloatArray(colCount) { 1f }
        val attrWeights = table.getAttribute("data-column-weights")
            ?.split(',')
            ?.mapNotNull { it.trim().toFloatOrNull() }
        if (attrWeights != null && attrWeights.isNotEmpty()) {
            for (i in 0 until colCount) {
                weights[i] = attrWeights.getOrElse(i) { 1f }.coerceAtLeast(0.1f)
            }
        } else {
            val cols = table.columns
            val hasExplicitWidth = cols.any { (it.widthPx ?: 0) > 0 }
            if (hasExplicitWidth) {
                for (i in 0 until colCount) {
                    val px = cols.getOrNull(i)?.widthPx
                    weights[i] = (px?.toFloat() ?: 120f).coerceAtLeast(24f)
                }
            }
        }

        val weightSum = weights.sum().coerceAtLeast(0.1f)
        val widths = IntArray(colCount)
        var used = 0
        for (i in 0 until colCount) {
            if (i == colCount - 1) {
                widths[i] = (totalWidth - used).coerceAtLeast(24)
            } else {
                val cw = ((totalWidth * (weights[i] / weightSum)).roundToInt()).coerceAtLeast(24)
                widths[i] = cw
                used += cw
            }
        }
        return widths
    }

    private fun renderLazyGridTree(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        tree: LazyGridTree<*>,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        val cols = tree.treeColumns
        val totalWeight = cols.sumOf { it.widthWeight.toDouble() }.toFloat().coerceAtLeast(1f)
        val headerH = 24
        var curY = y

        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 1, curY + 1, w - 2, headerH, ctx.bgElevated)
        JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + w, curY + headerH, ctx.borderStrong)
        var hx = x
        for (col in cols) {
            val cw = ((w * (col.widthWeight / totalWeight)).roundToInt()).coerceAtLeast(48)
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = hx + 6,
                y = curY + 8,
                maxW = cw - 10,
                text = col.headerText,
                color = ctx.textPrimary,
                scale = 1
            )
            JestElectricControlTintRenderUtil.drawVLine(ctx.buffer, hx + cw, curY, curY + headerH, ctx.borderSubtle)
            hx += cw
        }
        curY += headerH + 1

        val rowH = 26
        val rows = tree.flattenedRows
        for ((rIdx, flatRow) in rows.withIndex()) {
            if (curY + rowH > y + h) break
            if (rIdx % 2 == 1) {
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 1, curY, w - 2, rowH, ctx.bgSurface)
            }
            JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x + 1, x + w - 1, curY + rowH, ctx.borderSubtle)
            var cx = x
            for ((cIdx, col) in cols.withIndex()) {
                val cw = ((w * (col.widthWeight / totalWeight)).roundToInt()).coerceAtLeast(48)
                val cell = flatRow.node.cells.getOrNull(cIdx)
                val comp = cell?.component
                val txt = cell?.text?.ifEmpty { null } ?: if (col.isTreeColumn) flatRow.node.keyText else ""
                val indent = if (col.isTreeColumn) (flatRow.depth * 14).coerceAtMost(cw / 2) else 0
                if (comp != null && comp.isVisible) {
                    ctx.drawChild(
                        cx + indent + 4,
                        curY + 2,
                        (cw - indent - 8).coerceAtLeast(24),
                        rowH - 4,
                        comp,
                        depth + 1
                    )
                } else {
                    val prefix = if (col.isTreeColumn && flatRow.node.hasBranchDisclosure) {
                        if (flatRow.node.isExpanded) "v " else "> "
                    } else {
                        ""
                    }
                    JestElectricControlTintRenderUtil.drawTextClipped(
                        buf = ctx.buffer,
                        x = cx + indent + 6,
                        y = curY + 9,
                        maxW = cw - indent - 10,
                        text = prefix + txt,
                        color = ctx.textPrimary,
                        scale = 1
                    )
                }
                JestElectricControlTintRenderUtil.drawVLine(ctx.buffer, cx + cw, curY, curY + rowH, ctx.borderSubtle)
                cx += cw
            }
            curY += rowH
        }
    }

    private fun renderLazyList(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        list: LazyList,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        if (list.children.isEmpty() && list.totalItemCount > 0) {
            runCatching { list.refreshLazyViewport() }
        }
        val items = list.children.filter { it.isVisible }
        val rowH = list.estimatedItemExtentPx.roundToInt().coerceIn(28, 56)
        var curY = y + 4
        for (item in items) {
            if (curY + rowH > y + h - 4) break
            ctx.drawChild(x + 4, curY, w - 14, rowH - 2, item, depth + 1)
            curY += rowH + 2
        }
        // Scrollbar
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + w - 7, y + 2, 5, h - 4, ctx.bgElevated)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + w - 7, y + 4, 5, (h / 4).coerceAtLeast(16), ctx.accent)
    }

    private fun renderLazyRow(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        row: LazyRow,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        if (row.children.isEmpty() && row.totalItemCount > 0) {
            runCatching { row.refreshLazyViewport() }
        }
        val items = row.children.filter { it.isVisible }
        val itemW = row.estimatedItemExtentPx.roundToInt().coerceIn(56, 180)
        var curX = x + 4
        for (item in items) {
            if (curX + itemW > x + w - 4) break
            ctx.drawChild(curX, y + 4, itemW - 4, (h - 12).coerceAtLeast(16), item, depth + 1)
            curX += itemW + 2
        }
    }

    private fun renderLazyGrid(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        grid: LazyGrid,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        if (grid.children.isEmpty()) {
            runCatching { grid.refreshLazyViewport() }
        }
        // Honor adaptive minColumnWidthPx so columns never crush below readable width
        val minColW = (grid.getAttribute("data-min-column-width-px")?.toIntOrNull() ?: 140).coerceAtLeast(48)
        val maxFitCols = max(1, (w - 14) / minColW)
        val cols = min(grid.columns.coerceAtLeast(1), maxFitCols).coerceAtLeast(1)
        val cellW = ((w - 14) / cols).coerceAtLeast(48)
        val cellH = grid.estimatedRowHeightPx.roundToInt().coerceIn(44, 120)
        val items = grid.children.filter { it.isVisible }
        for ((idx, item) in items.withIndex()) {
            val r = idx / cols
            val c = idx % cols
            val cx = x + 4 + c * cellW
            val cy = y + 4 + r * (cellH + 4)
            if (cy + cellH > y + h - 4) break
            ctx.drawChild(cx, cy, cellW - 4, cellH, item, depth + 1)
        }
    }

    private fun renderTreeView(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        tree: TreeView<*>
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        var curY = y + 4
        val rowH = 22
        fun drawNodes(nodes: List<TreeNode<*>>, level: Int) {
            for (node in nodes) {
                if (curY + rowH > y + h - 2) return
                val isSel = node.id == tree.selectedNodeId
                if (isSel) {
                    JestElectricControlTintRenderUtil.fillRect(
                        ctx.buffer,
                        x + 2,
                        curY,
                        w - 4,
                        rowH - 2,
                        ctx.accent.blendOverSurface(ctx.bgElevated, 0.35f)
                    )
                }
                val indent = level * 16
                val hasChildren = node.children.isNotEmpty() || node.lazyChildrenLoader != null
                val prefix = if (hasChildren) (if (node.isExpanded) "v " else "> ") else "  "
                JestElectricControlTintRenderUtil.drawTextClipped(
                    buf = ctx.buffer,
                    x = x + 8 + indent,
                    y = curY + 6,
                    maxW = w - indent - 14,
                    text = prefix + node.label,
                    color = ctx.textPrimary,
                    scale = 1
                )
                curY += rowH
                if (node.isExpanded) {
                    drawNodes(node.children, level + 1)
                }
            }
        }
        drawNodes(tree.rootNodes, 0)
    }

    private fun renderAvatar(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        avatar: Avatar
    ) {
        val r = (min(w, h) / 2 - 1).coerceAtLeast(8)
        val cx = x + w / 2
        val cy = y + h / 2
        JestElectricControlTintRenderUtil.fillCircle(ctx.buffer, cx, cy, r, ctx.accent)
        JestElectricControlTintRenderUtil.drawCircle(ctx.buffer, cx, cy, r, ctx.borderStrong)
        val initials = (avatar.fallbackText ?: "").ifEmpty { "U" }.take(2).uppercase()
        val tw = JestElectricControlTintRenderUtil.measureTextWidth(initials, 1)
        JestElectricControlTintRenderUtil.drawText(
            ctx.buffer,
            cx - tw / 2,
            cy - 3,
            initials,
            ctx.resolveBootstrapForeground(avatar.type),
            1,
            bold = true
        )
    }

    private fun renderImageView(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        img: ImageView
    ) {
        val srcBuf = img.imageBuffer
        if (srcBuf != null && srcBuf.width > 0 && srcBuf.height > 0) {
            for (dy in 0 until h) {
                val sy = (dy * srcBuf.height / h).coerceIn(0, srcBuf.height - 1)
                for (dx in 0 until w) {
                    val sx = (dx * srcBuf.width / w).coerceIn(0, srcBuf.width - 1)
                    ctx.buffer.setPixel(x + dx, y + dy, srcBuf.getPixel(sx, sy))
                }
            }
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        } else {
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgElevated)
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
            val label = (img.fallbackText ?: "").ifEmpty { img.sourceUrl ?: "Image" }
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = x + 6,
                y = y + (h - 7) / 2,
                maxW = (w - 12).coerceAtLeast(1),
                text = label,
                color = ctx.textSecondary,
                scale = 1
            )
        }
    }

    private fun renderTooltip(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        tooltip: Tooltip
    ) {
        val bh = h.coerceAtMost(24)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, bh, ctx.bgTitleBar)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, bh, ctx.borderStrong, 1)
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = x + 6,
            y = y + (bh - 7) / 2,
            maxW = (w - 12).coerceAtLeast(1),
            text = tooltip.title ?: "Tooltip",
            color = ctx.textPrimary,
            scale = 1
        )
    }

    private fun renderPopover(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        popover: Popover,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderStrong, 1)
        var bodyY = y + 4
        val title = popover.title
        if (!title.isNullOrBlank()) {
            val headerH = 22.coerceAtMost(h)
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 1, y + 1, w - 2, headerH, ctx.bgElevated)
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = x + 8,
                y = y + 7,
                maxW = (w - 16).coerceAtLeast(1),
                text = title,
                color = ctx.textPrimary,
                scale = 1,
                bold = true
            )
            bodyY = y + headerH + 4
        }
        if (bodyY < y + h - 4) {
            JestElectricContainerChromeRenderUtil.renderVerticalChildren(
                ctx = ctx,
                x = x + 6,
                y = bodyY,
                w = (w - 12).coerceAtLeast(1),
                h = (y + h - bodyY - 4).coerceAtLeast(1),
                children = popover.children,
                depth = depth + 1,
                gap = 4
            )
        }
    }
}
