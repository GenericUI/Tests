package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.dialog.DialogWindow
import com.nexustools.genericui.component.dock.ColorSwatchBox
import com.nexustools.genericui.component.dock.DockArea
import com.nexustools.genericui.component.dock.DockPanel
import com.nexustools.genericui.component.dock.DockWorkspace
import com.nexustools.genericui.component.dock.ToolPalette
import com.nexustools.genericui.component.feedback.Toast
import com.nexustools.genericui.component.feedback.ToastContainer
import com.nexustools.genericui.component.fragment.helper.DockWorkspaceFragment
import com.nexustools.genericui.component.layout.ScrollView
import com.nexustools.genericui.component.shell.MdiContainer
import com.nexustools.genericui.component.shell.MdiWindow
import com.nexustools.genericui.component.shell.SplitPane
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.core.AbstractContainer
import com.nexustools.genericui.enums.ui.Orientation
import com.nexustools.genericui.style.color.RgbaColor
import kotlin.math.roundToInt

/**
 * Renders workspace, multi-document, docking, split-pane, scrollable, dialog, toast,
 * book-container, and carousel-selector components ([DockWorkspaceFragment], [DockWorkspace],
 * [DockPanel], [ToolPalette], [ColorSwatchBox], [MdiContainer], [MdiWindow], [SplitPane],
 * [ScrollView], [DialogWindow], [Toast], [ToastContainer]).
 */
object JestElectricWorkspaceWindowRenderUtil {

    fun canRender(comp: AbstractComponent): Boolean =
        comp is DockWorkspaceFragment ||
            comp is DockWorkspace ||
            comp is DockPanel ||
            comp is ToolPalette ||
            comp is ColorSwatchBox ||
            comp is MdiContainer ||
            comp is MdiWindow ||
            comp is SplitPane ||
            comp is ScrollView ||
            comp is DialogWindow ||
            comp is Toast ||
            comp is ToastContainer ||
            comp.htmlTag.contains("bookcontainer", ignoreCase = true) ||
            comp.htmlTag.contains("carousel", ignoreCase = true)

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
            is DockWorkspaceFragment -> {
                renderDockWorkspace(ctx, x, y, w, h, comp.dockWorkspace, depth + 1)
                true
            }
            is DockWorkspace -> {
                renderDockWorkspace(ctx, x, y, w, h, comp, depth + 1)
                true
            }
            is DockPanel -> {
                renderDockPanel(ctx, x, y, w, h, comp, depth + 1)
                true
            }
            is ToolPalette -> {
                renderToolPalette(ctx, x, y, w, h, comp)
                true
            }
            is ColorSwatchBox -> {
                renderColorSwatchBox(ctx, x, y, w, h, comp)
                true
            }
            is MdiContainer -> {
                renderMdiContainer(ctx, x, y, w, h, comp, depth + 1)
                true
            }
            is MdiWindow -> {
                renderMdiWindow(ctx, x, y, w, h, comp, depth + 1)
                true
            }
            is SplitPane -> {
                renderSplitPane(ctx, x, y, w, h, comp, depth + 1)
                true
            }
            is ScrollView -> {
                renderScrollView(ctx, x, y, w, h, comp, depth + 1)
                true
            }
            is DialogWindow -> {
                renderDialogWindowInline(ctx, x, y, w, h, comp, depth + 1)
                true
            }
            is Toast -> {
                renderToast(ctx, x, y, w, h, comp)
                true
            }
            is ToastContainer -> {
                JestElectricContainerChromeRenderUtil.renderVerticalChildren(
                    ctx = ctx,
                    x = x,
                    y = y,
                    w = w,
                    h = h,
                    children = comp.children,
                    depth = depth + 1,
                    gap = 6
                )
                true
            }
            else -> {
                if (comp is AbstractContainer &&
                    (comp.htmlTag.contains("bookcontainer", ignoreCase = true) ||
                        comp.htmlTag.contains("carousel", ignoreCase = true))
                ) {
                    JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgSurface)
                    JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
                    JestElectricContainerChromeRenderUtil.renderHorizontalChildren(
                        ctx = ctx,
                        x = x + 6,
                        y = y + 4,
                        w = (w - 12).coerceAtLeast(1),
                        h = (h - 8).coerceAtLeast(1),
                        children = comp.children,
                        depth = depth + 1,
                        gap = 8
                    )
                    true
                } else {
                    false
                }
            }
        }
    }

    fun renderModalDialogOverlay(
        ctx: JestElectricRenderContext,
        viewportW: Int,
        viewportH: Int,
        titleBarH: Int,
        dlg: DialogWindow
    ) {
        val dw = (viewportW * 0.56f).roundToInt().coerceIn(320, viewportW - 40)
        val dh = (viewportH * 0.54f).roundToInt().coerceIn(220, viewportH - 40)
        val dx = (viewportW - dw) / 2
        val dy = (viewportH - dh) / 2
        JestElectricControlTintRenderUtil.fillRectAlpha(
            ctx.buffer,
            0,
            titleBarH,
            viewportW,
            viewportH - titleBarH,
            RgbaColor(0, 0, 0, 110)
        )
        renderDialogWindowInline(ctx, dx, dy, dw, dh, dlg, 1)
    }

    private fun renderDialogWindowInline(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        dlg: DialogWindow,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.accent, 2)
        val headerH = 32.coerceAtMost(h)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, headerH, ctx.bgTitleBar)
        JestElectricControlTintRenderUtil.drawText(
            ctx.buffer,
            x + 12,
            y + 9,
            dlg.getTitle().ifEmpty { "Dialog" },
            ctx.textPrimary,
            2
        )
        dlg.getContent()?.let { content ->
            if (h > headerH + 12) {
                ctx.drawChild(x + 10, y + headerH + 6, w - 20, h - headerH - 16, content, depth + 1)
            }
        }
    }

    private fun renderToast(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        toast: Toast
    ) {
        val accent = ctx.resolveBootstrapColor(toast.type)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgElevated)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, accent, 1)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, 4, h, accent)
        val label = if (toast.title.isNotEmpty() && toast.message.isNotEmpty()) {
            "${toast.title}: ${toast.message}"
        } else {
            toast.title.ifEmpty { toast.message }
        }
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = x + 10,
            y = y + (h - 7) / 2,
            maxW = (w - 16).coerceAtLeast(1),
            text = label,
            color = ctx.textPrimary,
            scale = 1
        )
    }

    private fun renderSplitPane(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        sp: SplitPane,
        depth: Int
    ) {
        val ratio = sp.dividerPosition.coerceIn(0.15f, 0.85f)
        val first = sp.firstPane ?: sp.firstPaneHost
        val second = sp.secondPane ?: sp.secondPaneHost
        if (sp.orientation == Orientation.HORIZONTAL) {
            val firstW = ((w - 6) * ratio).roundToInt().coerceAtLeast(40)
            val secondW = (w - 6 - firstW).coerceAtLeast(40)
            ctx.drawChild(x, y, firstW, h, first, depth + 1)
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + firstW + 1, y, 4, h, ctx.borderSubtle)
            ctx.drawChild(x + firstW + 6, y, secondW, h, second, depth + 1)
        } else {
            val firstH = ((h - 6) * ratio).roundToInt().coerceAtLeast(40)
            val secondH = (h - 6 - firstH).coerceAtLeast(40)
            ctx.drawChild(x, y, w, firstH, first, depth + 1)
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y + firstH + 1, w, 4, ctx.borderSubtle)
            ctx.drawChild(x, y + firstH + 6, w, secondH, second, depth + 1)
        }
    }

    private fun renderScrollView(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        scroll: ScrollView,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgSurface)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        JestElectricContainerChromeRenderUtil.renderVerticalChildren(
            ctx = ctx,
            x = x + 4,
            y = y + 4,
            w = (w - 14).coerceAtLeast(1),
            h = (h - 8).coerceAtLeast(1),
            children = scroll.children,
            depth = depth + 1,
            gap = 6
        )
        // Scrollbar track & thumb
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + w - 7, y + 2, 5, (h - 4).coerceAtLeast(1), ctx.bgElevated)
        JestElectricControlTintRenderUtil.fillRect(
            ctx.buffer,
            x + w - 7,
            y + 4,
            5,
            (h / 3).coerceAtLeast(16).coerceAtMost((h - 8).coerceAtLeast(1)),
            ctx.borderStrong
        )
    }

    private fun renderDockWorkspace(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        ws: DockWorkspace,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgWindow)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)

        val topPanels = collectAreaPanels(ws.topArea)
        val bottomPanels = collectAreaPanels(ws.bottomArea)
        val leftPanels = collectAreaPanels(ws.leftArea)
        val rightPanels = collectAreaPanels(ws.rightArea)

        val topH = if (topPanels.isNotEmpty()) 54 else 0
        val bottomH = if (bottomPanels.isNotEmpty()) 136 else 0
        val leftW = if (leftPanels.isNotEmpty() || ws.toolPalette != null) 200 else 0
        val rightW = if (rightPanels.isNotEmpty()) 260 else 0

        if (topH > 0) {
            renderDockAreaStrip(ctx, x, y, w, topH, topPanels, depth + 1, horizontal = true)
        }

        val midY = y + topH
        val midH = (h - topH - bottomH).coerceAtLeast(80)

        if (leftW > 0) {
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, midY, leftW, midH, ctx.bgSurface)
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, midY, leftW, midH, ctx.borderSubtle, 1)
            var ly = midY + 4
            ws.toolPalette?.let { tp ->
                val tpH = 150.coerceAtMost(midH - 8)
                renderToolPalette(ctx, x + 4, ly, leftW - 8, tpH, tp)
                ly += tpH + 6
            }
            if (leftPanels.isNotEmpty() && ly < midY + midH - 20) {
                renderDockAreaStrip(ctx, x + 2, ly, leftW - 4, midY + midH - ly - 2, leftPanels, depth + 1, horizontal = false)
            }
        }

        if (rightW > 0) {
            val rx = x + w - rightW
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, rx, midY, rightW, midH, ctx.bgSurface)
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, rx, midY, rightW, midH, ctx.borderSubtle, 1)
            renderDockAreaStrip(ctx, rx + 2, midY + 2, rightW - 4, midH - 4, rightPanels, depth + 1, horizontal = false)
        }

        // Center workspace area
        val cx = x + leftW
        val cw = (w - leftW - rightW).coerceAtLeast(80)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, cx, midY, cw, midH, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, cx, midY, cw, midH, ctx.borderSubtle, 1)

        if (ws.centralDocumentTabs.entries.isNotEmpty()) {
            ctx.drawChild(cx + 4, midY + 4, cw - 8, midH - 8, ws.centralDocumentTabs, depth + 1)
        } else if (ws.backgroundComponent != null) {
            ctx.drawChild(cx + 4, midY + 4, cw - 8, midH - 8, ws.backgroundComponent!!, depth + 1)
        } else {
            val centerPanels = collectAreaPanels(ws.centerArea)
            if (centerPanels.isNotEmpty()) {
                renderDockAreaStrip(ctx, cx + 4, midY + 4, cw - 8, midH - 8, centerPanels, depth + 1, horizontal = false)
            }
        }

        if (bottomH > 0) {
            val by = y + h - bottomH
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, by, w, bottomH, ctx.bgSurface)
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, by, w, bottomH, ctx.borderSubtle, 1)
            renderDockAreaStrip(ctx, x + 2, by + 2, w - 4, bottomH - 4, bottomPanels, depth + 1, horizontal = true)
        }
    }

    private fun collectAreaPanels(area: DockArea): List<DockPanel> {
        val out = mutableListOf<DockPanel>()
        for (grp in area.groups) {
            for (panel in grp.panels) {
                if (panel.isVisible && !panel.isFloating) {
                    out.add(panel)
                }
            }
        }
        return out
    }

    private fun renderDockAreaStrip(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        panels: List<DockPanel>,
        depth: Int,
        horizontal: Boolean
    ) {
        if (panels.isEmpty()) return
        if (horizontal) {
            val pw = (w / panels.size).coerceAtLeast(60)
            for ((i, panel) in panels.withIndex()) {
                val px = x + i * pw
                val actualW = if (i == panels.lastIndex) (x + w - px) else (pw - 2)
                renderDockPanel(ctx, px, y, actualW, h, panel, depth + 1)
            }
        } else {
            val ph = (h / panels.size).coerceAtLeast(48)
            for ((i, panel) in panels.withIndex()) {
                val py = y + i * ph
                val actualH = if (i == panels.lastIndex) (y + h - py) else (ph - 2)
                renderDockPanel(ctx, x, py, w, actualH, panel, depth + 1)
            }
        }
    }

    private fun renderDockPanel(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        panel: DockPanel,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        val headerH = 24.coerceAtMost(h)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, headerH, ctx.bgElevated)
        JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + w, y + headerH - 1, ctx.borderSubtle)
        val arrow = if (panel.isCollapsed) ">" else "v"
        JestElectricControlTintRenderUtil.drawText(ctx.buffer, x + 6, y + 8, "$arrow ${panel.title}", ctx.textPrimary, 1)

        if (!panel.isCollapsed && h > headerH + 6) {
            JestElectricContainerChromeRenderUtil.renderVerticalChildren(
                ctx = ctx,
                x = x + 6,
                y = y + headerH + 4,
                w = w - 12,
                h = h - headerH - 8,
                children = panel.children.filter { !it.mountedInTitleBar },
                depth = depth + 1,
                gap = 4
            )
        }
    }

    private fun renderToolPalette(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        tp: ToolPalette
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        val cols = tp.columns.coerceIn(1, 4)
        val cellW = ((w - 8) / cols).coerceAtLeast(24)
        val cellH = 26
        for ((idx, tool) in tp.tools.withIndex()) {
            val r = idx / cols
            val c = idx % cols
            val tx = x + 4 + c * cellW
            val ty = y + 4 + r * (cellH + 3)
            if (ty + cellH > y + h - 24) break
            val isSel = tool.id == tp.activeToolId
            val bg = if (isSel) ctx.accent else ctx.bgElevated
            val fg = if (isSel) RgbaColor.WHITE else ctx.textPrimary
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, tx + 1, ty, cellW - 2, cellH, bg)
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, tx + 1, ty, cellW - 2, cellH, ctx.borderSubtle, 1)
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = tx + 5,
                y = ty + 9,
                maxW = cellW - 8,
                text = tool.name.ifEmpty { tool.id },
                color = fg,
                scale = 1
            )
        }
        if (h >= 50) {
            val sy = y + h - 22
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 16, sy + 4, 14, 14, tp.backgroundColor)
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x + 16, sy + 4, 14, 14, ctx.borderStrong, 1)
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 8, sy, 14, 14, tp.foregroundColor)
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x + 8, sy, 14, 14, RgbaColor.WHITE, 1)
        }
    }

    private fun renderColorSwatchBox(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        box: ColorSwatchBox
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 18, y + 10, 20, 20, box.backgroundColor)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x + 18, y + 10, 20, 20, ctx.borderStrong, 1)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 8, y + 4, 20, 20, box.foregroundColor)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x + 8, y + 4, 20, 20, RgbaColor.WHITE, 1)
        var sx = x + 46
        for (sw in box.paletteSwatches) {
            if (sx + 14 > x + w - 4) break
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, sx, y + 8, 12, 12, sw)
            JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, sx, y + 8, 12, 12, ctx.borderSubtle, 1)
            sx += 15
        }
    }

    private fun renderMdiContainer(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        mdi: MdiContainer,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgWindow)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)

        var curY = y
        val toolbarH = if (mdi.toolbarBar.isVisible) 34 else 0
        if (toolbarH > 0) {
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, curY, w, toolbarH, ctx.bgSurface)
            JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + w, curY + toolbarH - 1, ctx.borderSubtle)
            ctx.drawChild(x + 6, curY + 4, w - 12, toolbarH - 8, mdi.toolbarBar, depth + 1)
            curY += toolbarH
        }

        val taskbarH = if (mdi.taskbarStrip.isVisible) 32 else 0
        val deskH = (h - toolbarH - taskbarH).coerceAtLeast(80)

        JestElectricControlTintRenderUtil.fillRect(
            ctx.buffer,
            x,
            curY,
            w,
            deskH,
            if (ctx.isDark) RgbaColor(12, 19, 34) else RgbaColor(226, 232, 240)
        )
        var gy = curY + 16
        val dotColor = if (ctx.isDark) RgbaColor(35, 48, 72) else RgbaColor(203, 213, 225)
        while (gy < curY + deskH) {
            var gx = x + 16
            while (gx < x + w) {
                ctx.buffer.setPixel(gx, gy, dotColor)
                gx += 24
            }
            gy += 24
        }

        val visibleWins = mdi.windows.filter { it.isVisible }
        for ((idx, win) in visibleWins.withIndex()) {
            val wb = win.windowBounds
            val wx = (x + wb.x).coerceIn(x + 4, (x + w - 140).coerceAtLeast(x + 4))
            val wy = (curY + wb.y).coerceIn(curY + 4, (curY + deskH - 90).coerceAtLeast(curY + 4))
            val ww = wb.width.coerceIn(180, (x + w - wx - 4).coerceAtLeast(140))
            val wh = wb.height.coerceIn(110, (curY + deskH - wy - 4).coerceAtLeast(85))
            renderMdiWindow(
                ctx = ctx,
                x = wx,
                y = wy,
                w = ww,
                h = wh,
                win = win,
                depth = depth + 1,
                isActive = (win === mdi.activeWindow || idx == visibleWins.lastIndex)
            )
        }

        if (taskbarH > 0) {
            val ty = y + h - taskbarH
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, ty, w, taskbarH, ctx.bgSurface)
            JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + w, ty, ctx.borderSubtle)
            ctx.drawChild(x + 6, ty + 4, w - 12, taskbarH - 8, mdi.taskbarStrip, depth + 1)
        }
    }

    private fun renderMdiWindow(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        win: MdiWindow,
        depth: Int,
        isActive: Boolean = true
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgCard)
        val borderColor = if (isActive) ctx.accent else ctx.borderStrong
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, borderColor, if (isActive) 2 else 1)

        val headerH = 26.coerceAtMost(h)
        val headerBg = if (isActive) ctx.accent.blendOverSurface(ctx.bgElevated, 0.35f) else ctx.bgElevated
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x + 1, y + 1, w - 2, headerH, headerBg)
        JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x + 1, x + w - 1, y + headerH, borderColor)

        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = x + 8,
            y = y + 9,
            maxW = (w - 80).coerceAtLeast(40),
            text = win.getTitle(),
            color = ctx.textPrimary,
            scale = 1
        )
        if (w >= 90) {
            JestElectricControlTintRenderUtil.drawText(ctx.buffer, x + w - 46, y + 9, "- [] X", ctx.textSecondary, 1)
        }

        if (h > headerH + 8) {
            JestElectricContainerChromeRenderUtil.renderVerticalChildren(
                ctx = ctx,
                x = x + 6,
                y = y + headerH + 4,
                w = w - 12,
                h = h - headerH - 8,
                children = win.bodyContainer.children,
                depth = depth + 1,
                gap = 4
            )
        }
    }
}
