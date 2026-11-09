package com.nexustools.genericui.test.jestelectric.render

import com.nexustools.genericui.component.action.Toolbar
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.helper.TabbedFragment
import com.nexustools.genericui.component.navigation.BottomNavigationBar
import com.nexustools.genericui.component.navigation.Breadcrumb
import com.nexustools.genericui.component.navigation.Navbar
import com.nexustools.genericui.component.navigation.Pagination
import com.nexustools.genericui.component.navigation.SideNav
import com.nexustools.genericui.component.navigation.Stepper
import com.nexustools.genericui.component.overlay.Drawer
import com.nexustools.genericui.component.shell.Menu
import com.nexustools.genericui.component.shell.MenuBar
import com.nexustools.genericui.component.shell.Window
import com.nexustools.genericui.component.system.StatusBar
import com.nexustools.genericui.component.tabs.Tab
import com.nexustools.genericui.component.tabs.TabContainer
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.ui.Placement
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.window.TitleBar
import kotlin.math.min

/**
 * Renders navigation and application chrome components ([TabContainer], [Tab], [TabbedFragment],
 * [MenuBar], [Navbar], [Toolbar], [Breadcrumb], [Pagination], [Stepper], [BottomNavigationBar],
 * [SideNav], [Drawer], [StatusBar], and window [TitleBar]).
 */
object JestElectricNavigationRenderUtil {

    fun canRender(comp: AbstractComponent): Boolean =
        comp is TabbedFragment ||
            comp is TabContainer ||
            comp is Tab ||
            comp is MenuBar ||
            comp is Navbar ||
            comp is Toolbar ||
            comp is Breadcrumb ||
            comp is Pagination ||
            comp is Stepper ||
            comp is BottomNavigationBar ||
            comp is SideNav ||
            comp is Drawer ||
            comp is StatusBar

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
            is TabbedFragment -> {
                JestElectricContainerChromeRenderUtil.renderVerticalChildren(
                    ctx = ctx,
                    x = x,
                    y = y,
                    w = w,
                    h = h,
                    children = comp.children,
                    depth = depth + 1,
                    gap = 4
                )
                true
            }
            is TabContainer -> {
                renderTabContainer(ctx, x, y, w, h, comp, depth)
                true
            }
            is Tab -> {
                renderSingleTab(ctx, x, y, w, h, comp)
                true
            }
            is MenuBar -> {
                renderMenuBar(ctx, x, y, w, h, comp)
                true
            }
            is Navbar -> {
                renderNavbar(ctx, x, y, w, h, comp, depth)
                true
            }
            is Toolbar -> {
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
                    gap = 6,
                    wrap = JestElectricContainerChromeRenderUtil.shouldWrapFlexChildren(comp)
                )
                true
            }
            is Breadcrumb -> {
                renderBreadcrumb(ctx, x, y, w, h, comp)
                true
            }
            is Pagination -> {
                renderPagination(ctx, x, y, w, h, comp)
                true
            }
            is Stepper -> {
                renderStepper(ctx, x, y, w, h, comp)
                true
            }
            is BottomNavigationBar -> {
                renderBottomNav(ctx, x, y, w, h, comp)
                true
            }
            is SideNav -> {
                renderSideNav(ctx, x, y, w, h, comp, depth)
                true
            }
            is Drawer -> {
                renderDrawer(ctx, x, y, w, h, comp, depth)
                true
            }
            is StatusBar -> {
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgStatusBar)
                JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
                JestElectricContainerChromeRenderUtil.renderHorizontalChildren(
                    ctx = ctx,
                    x = x + 8,
                    y = y + 2,
                    w = (w - 16).coerceAtLeast(1),
                    h = (h - 4).coerceAtLeast(1),
                    children = comp.children,
                    depth = depth + 1,
                    gap = 12
                )
                true
            }
            else -> false
        }
    }

    fun renderWindowTitleBar(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        win: Window,
        rootFrag: Fragment?,
        titleBar: TitleBar?
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgTitleBar)
        JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + w, y + h - 1, ctx.borderSubtle)

        var leftX = x + 10
        // App icon badge
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, leftX, y + 9, 18, 18, ctx.accent)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, leftX, y + 9, 18, 18, RgbaColor(255, 255, 255, 90), 1)
        JestElectricControlTintRenderUtil.drawText(ctx.buffer, leftX + 4, y + 11, "G", RgbaColor.WHITE, 2)
        leftX += 26

        // Window title
        val showTitle = titleBar?.showWindowTitle ?: true
        val winTitle = win.getTitle().ifEmpty { "GenericUI Application" }
        if (showTitle) {
            JestElectricControlTintRenderUtil.drawTextClipped(ctx.buffer, leftX, y + 11, 520, winTitle, ctx.textPrimary, 2)
            leftX += min(520, JestElectricControlTintRenderUtil.measureTextWidth(winTitle, 2) + 16)
        }

        // Embedded menu bar if configured
        val menuBar = rootFrag?.getMenuBar() ?: win.getMenuBar()
        if (titleBar?.embedMenuBar == true && menuBar != null) {
            for (child in menuBar.children) {
                val menu = child as? Menu ?: continue
                val lbl = menu.getLabel().replace("&", "")
                val mw = JestElectricControlTintRenderUtil.measureTextWidth(lbl, 1) + 14
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, leftX, y + 7, mw, h - 14, ctx.bgElevated)
                JestElectricControlTintRenderUtil.drawText(ctx.buffer, leftX + 7, y + 13, lbl, ctx.textSecondary, 1)
                leftX += mw + 4
            }
        }

        // Leading & center titlebar components
        if (titleBar != null) {
            for (comp in titleBar.leadingComponents) {
                if (!comp.isVisible) continue
                val cw = JestElectricContainerChromeRenderUtil.estimateInlineWidth(comp).coerceAtMost(260)
                ctx.drawChild(leftX, y + 4, cw, h - 8, comp, 1)
                leftX += cw + 6
            }
            for (comp in titleBar.centerComponents) {
                if (!comp.isVisible) continue
                val cw = JestElectricContainerChromeRenderUtil.estimateInlineWidth(comp).coerceAtMost(360)
                ctx.drawChild(leftX, y + 4, cw, h - 8, comp, 1)
                leftX += cw + 6
            }
        }

        // Window caption buttons on the right
        val btnW = 36
        var rightX = x + w - btnW * 3 - 4
        if (titleBar != null && titleBar.trailingComponents.isNotEmpty()) {
            var trailX = rightX - 8
            for (comp in titleBar.trailingComponents.asReversed()) {
                if (!comp.isVisible) continue
                val cw = JestElectricContainerChromeRenderUtil.estimateInlineWidth(comp).coerceAtMost(180)
                trailX -= cw
                if (trailX > leftX) {
                    ctx.drawChild(trailX, y + 5, cw, h - 10, comp, 1)
                    trailX -= 6
                }
            }
        }

        // Minimize, Maximize, Close
        JestElectricControlTintRenderUtil.drawText(ctx.buffer, rightX + 13, y + 11, "-", ctx.textSecondary, 2)
        rightX += btnW
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, rightX + 12, y + 12, 11, 11, ctx.textSecondary, 1)
        rightX += btnW
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, rightX + 2, y + 4, btnW - 4, h - 8, RgbaColor(220, 38, 38, 190))
        JestElectricControlTintRenderUtil.drawText(ctx.buffer, rightX + 13, y + 11, "X", RgbaColor.WHITE, 2)
    }

    fun renderMenuBar(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        menuBar: MenuBar
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgMenuBar)
        JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + w, y + h - 1, ctx.borderSubtle)
        var curX = x + 10
        for (child in menuBar.children) {
            val menu = child as? Menu ?: continue
            val label = menu.getLabel().replace("&", "")
            val itemW = JestElectricControlTintRenderUtil.measureTextWidth(label, 1) + 18
            JestElectricControlTintRenderUtil.drawText(ctx.buffer, curX + 6, y + 9, label, ctx.textPrimary, 1)
            curX += itemW
        }
    }

    fun renderNavbar(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        navBar: Navbar,
        depth: Int = 1
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgSurface)
        JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + w, y + h - 1, ctx.borderSubtle)
        var curX = x + 14
        if (navBar.brandTitle.isNotEmpty()) {
            JestElectricControlTintRenderUtil.drawText(ctx.buffer, curX, y + 13, navBar.brandTitle, ctx.accent, 2)
            curX += JestElectricControlTintRenderUtil.measureTextWidth(navBar.brandTitle, 2) + 20
        }
        for (child in navBar.children) {
            if (!child.isVisible) continue
            val cw = JestElectricContainerChromeRenderUtil.estimateInlineWidth(child).coerceAtMost(220)
            ctx.drawChild(curX, y + 6, cw, h - 12, child, depth)
            curX += cw + 8
        }
    }

    fun renderBottomNav(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        bottomNav: BottomNavigationBar
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgSurface)
        JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + w, y, ctx.borderSubtle)
        val items = bottomNav.items
        if (items.isEmpty()) return
        val cellW = (w / items.size).coerceAtLeast(1)
        for ((idx, item) in items.withIndex()) {
            val ix = x + idx * cellW
            val isSel = item.first == bottomNav.selectedItemId
            val color = if (isSel) ctx.accent else ctx.textSecondary
            if (isSel) {
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, ix + cellW / 4, y + 2, cellW / 2, 3, ctx.accent)
            }
            val tw = JestElectricControlTintRenderUtil.measureTextWidth(item.second, 1)
            JestElectricControlTintRenderUtil.drawText(ctx.buffer, ix + (cellW - tw) / 2, y + 16, item.second, color, 1)
        }
    }

    private fun renderTabContainer(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        tc: TabContainer,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgSurface)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)

        val tabBarH = 30.coerceAtMost(h)
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, tabBarH, ctx.bgElevated)
        JestElectricControlTintRenderUtil.drawHLine(ctx.buffer, x, x + w, y + tabBarH - 1, ctx.borderSubtle)

        var tx = x + 6
        val selIdx = tc.selectedIndex.coerceIn(0, (tc.entries.size - 1).coerceAtLeast(0))
        for ((idx, entry) in tc.entries.withIndex()) {
            val title = entry.tab.displayTitle.ifEmpty { entry.tab.title.ifEmpty { "Tab ${idx + 1}" } }
            val tw = (JestElectricControlTintRenderUtil.measureTextWidth(title, 1) + 22).coerceIn(56, 200)
            if (tx + tw > x + w - 4) break
            val isSel = idx == selIdx
            val tabBg = if (isSel) ctx.bgCard else ctx.bgElevated
            JestElectricControlTintRenderUtil.fillRect(ctx.buffer, tx, y + 3, tw, tabBarH - 3, tabBg)
            JestElectricControlTintRenderUtil.drawRectBorder(
                ctx.buffer,
                tx,
                y + 3,
                tw,
                tabBarH - 3,
                if (isSel) ctx.accent else ctx.borderSubtle,
                1
            )
            if (isSel) {
                JestElectricControlTintRenderUtil.fillRect(ctx.buffer, tx, y + 3, tw, 2, ctx.accent)
            }
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = tx + 8,
                y = y + 11,
                maxW = tw - 14,
                text = title,
                color = if (isSel) ctx.textPrimary else ctx.textSecondary,
                scale = 1
            )
            tx += tw + 3
        }

        val activeContent = tc.entries.getOrNull(selIdx)?.content
        if (activeContent != null && h > tabBarH + 8) {
            ctx.drawChild(x + 6, y + tabBarH + 6, w - 12, h - tabBarH - 12, activeContent, depth + 1)
        }
    }

    private fun renderSingleTab(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        tab: Tab
    ) {
        val bg = if (tab.isSelected) ctx.bgCard else ctx.bgElevated
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, bg)
        JestElectricControlTintRenderUtil.drawRectBorder(
            ctx.buffer,
            x,
            y,
            w,
            h,
            if (tab.isSelected) ctx.accent else ctx.borderSubtle,
            1
        )
        val title = tab.displayTitle.ifEmpty { tab.title.ifEmpty { "Tab" } }
        JestElectricControlTintRenderUtil.drawTextClipped(
            buf = ctx.buffer,
            x = x + 8,
            y = y + (h - 7) / 2,
            maxW = (w - 16).coerceAtLeast(1),
            text = title,
            color = if (tab.isSelected) ctx.textPrimary else ctx.textSecondary,
            scale = 1
        )
    }

    private fun renderBreadcrumb(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Breadcrumb
    ) {
        var cx = x + 4
        val ty = y + (h - 7) / 2
        for ((idx, item) in comp.items.withIndex()) {
            if (cx >= x + w - 8) break
            val fg = if (item.isActive || idx == comp.items.lastIndex) ctx.textSecondary else ctx.accent
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = cx,
                y = ty,
                maxW = (x + w - cx).coerceAtLeast(1),
                text = item.label,
                color = fg,
                scale = 1
            )
            cx += JestElectricControlTintRenderUtil.measureTextWidth(item.label, 1) + 6
            if (idx < comp.items.lastIndex && cx < x + w - 8) {
                JestElectricControlTintRenderUtil.drawText(ctx.buffer, cx, ty, "/", ctx.textMuted, 1)
                cx += 10
            }
        }
    }

    private fun renderPagination(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Pagination
    ) {
        val total = comp.totalPages.coerceIn(1, 9)
        val cur = comp.currentPage.coerceIn(1, total)
        val btnH = h.coerceAtMost(26)
        val by = y + (h - btnH) / 2
        var cx = x + 2
        val btnW = 24

        // Prev button
        JestElectricControlTintRenderUtil.renderTintedNativeControl(
            ctx = ctx,
            x = cx,
            y = by,
            w = btnW,
            h = btnH,
            bootstrapType = BootstrapType.NONE,
            isEnabled = cur > 1,
            labelText = "<"
        )
        cx += btnW + 2

        for (p in 1..total) {
            if (cx + btnW > x + w - btnW) break
            val isActive = p == cur
            JestElectricControlTintRenderUtil.renderTintedNativeControl(
                ctx = ctx,
                x = cx,
                y = by,
                w = btnW,
                h = btnH,
                bootstrapType = if (isActive) BootstrapType.PRIMARY else BootstrapType.NONE,
                labelText = p.toString()
            )
            cx += btnW + 2
        }

        // Next button
        if (cx + btnW <= x + w) {
            JestElectricControlTintRenderUtil.renderTintedNativeControl(
                ctx = ctx,
                x = cx,
                y = by,
                w = btnW,
                h = btnH,
                bootstrapType = BootstrapType.NONE,
                isEnabled = cur < total,
                labelText = ">"
            )
        }
    }

    private fun renderStepper(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Stepper
    ) {
        val steps = comp.steps
        if (steps.isEmpty()) return
        val stepW = (w / steps.size).coerceAtLeast(36)
        val cy = y + h / 2
        for ((idx, label) in steps.withIndex()) {
            val sx = x + idx * stepW
            val cx = sx + 12
            val isCompleted = idx < comp.activeStepIndex
            val isActive = idx == comp.activeStepIndex
            val circleColor = when {
                isActive || isCompleted -> ctx.accent
                else -> ctx.bgElevated
            }
            JestElectricControlTintRenderUtil.fillCircle(ctx.buffer, cx, cy, 8, circleColor)
            JestElectricControlTintRenderUtil.drawCircle(ctx.buffer, cx, cy, 8, ctx.borderStrong)
            val numFg = if (isActive || isCompleted) RgbaColor.WHITE else ctx.textSecondary
            JestElectricControlTintRenderUtil.drawText(ctx.buffer, cx - 2, cy - 3, "${idx + 1}", numFg, 1)
            JestElectricControlTintRenderUtil.drawTextClipped(
                buf = ctx.buffer,
                x = cx + 12,
                y = cy - 3,
                maxW = (stepW - 28).coerceAtLeast(8),
                text = label,
                color = if (isActive) ctx.textPrimary else ctx.textSecondary,
                scale = 1,
                bold = isActive
            )
        }
    }

    private fun renderSideNav(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: SideNav,
        depth: Int
    ) {
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, x, y, w, h, ctx.bgSurface)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, x, y, w, h, ctx.borderSubtle, 1)
        JestElectricContainerChromeRenderUtil.renderVerticalChildren(
            ctx = ctx,
            x = x + 6,
            y = y + 6,
            w = (w - 12).coerceAtLeast(1),
            h = (h - 12).coerceAtLeast(1),
            children = comp.children,
            depth = depth + 1,
            gap = 4
        )
    }

    private fun renderDrawer(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: Drawer,
        depth: Int
    ) {
        if (!comp.isOpen && !comp.isVisible) return
        val drawerW = min(w, 260)
        val dx = if (comp.placement == Placement.END) x + w - drawerW else x
        JestElectricControlTintRenderUtil.fillRect(ctx.buffer, dx, y, drawerW, h, ctx.bgCard)
        JestElectricControlTintRenderUtil.drawRectBorder(ctx.buffer, dx, y, drawerW, h, ctx.borderStrong, 1)
        JestElectricContainerChromeRenderUtil.renderVerticalChildren(
            ctx = ctx,
            x = dx + 8,
            y = y + 8,
            w = (drawerW - 16).coerceAtLeast(1),
            h = (h - 16).coerceAtLeast(1),
            children = comp.children,
            depth = depth + 1,
            gap = 6
        )
    }
}
