package com.nexustools.genericui.tests

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.core.AbstractComponent
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.test.jestelectric.render.JestElectricButtonRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricContainerChromeRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricControlTintRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricDataDisplayRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricInputFieldRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricMediaCanvasRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricNavigationRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricRenderContext
import com.nexustools.genericui.test.jestelectric.render.JestElectricSliderProgressRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricTextRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricToggleSelectionRenderUtil
import com.nexustools.genericui.test.jestelectric.render.JestElectricWorkspaceWindowRenderUtil

/**
 * Software scene-graph rasterizer that renders a live GenericUI [Application] into an [ImageBuffer]
 * by delegating to the 12 modular `JestElectric*RenderUtil` classes in
 * `com.nexustools.genericui.test.jestelectric.render`.
 */
object JestElectricComponentSnapshotRenderer {

    /**
     * Renders [app] into a newly allocated [ImageBuffer] of dimensions `[width] x [height]`.
     */
    fun renderApplication(
        app: Application,
        width: Int = 1280,
        height: Int = 800,
        backendTargetOverride: String? = null
    ): ImageBuffer {
        val w = width.coerceAtLeast(320)
        val h = height.coerceAtLeast(240)
        val buf = ImageBuffer(w, h)
        val ctx = JestElectricRenderContext.fromApplication(
            app = app,
            buffer = buf,
            backendTargetOverride = backendTargetOverride,
            renderChild = { childCtx, cx, cy, cw, ch, comp, depth ->
                renderComponent(childCtx, cx, cy, cw, ch, comp, depth)
            }
        )

        // Fill window background
        JestElectricControlTintRenderUtil.fillRect(buf, 0, 0, w, h, ctx.bgWindow)

        val win = app.mainWindow
        val activeFrag = (app as? FragmentApplication)?.currentFragment
            ?: win.fragmentContainer?.currentFragment
            ?: app.currentFragment

        if (activeFrag == null && app is FragmentApplication) {
            runCatching { app.onCreateMainFragment() }
        }
        val rootFrag = (app as? FragmentApplication)?.currentFragment
            ?: win.fragmentContainer?.currentFragment
            ?: app.currentFragment

        val titleBar = rootFrag?.getTitleBar() ?: win.getTitleBar()
        val menuBar = rootFrag?.getMenuBar() ?: win.getMenuBar()
        val navBar = rootFrag?.getNavBar() ?: win.getNavBar()
        val bottomNav = rootFrag?.getBottomNavigationBar() ?: win.getBottomNavigationBar()

        var curY = 0

        // 1. Render Window TitleBar (36px)
        val titleBarH = 36
        JestElectricNavigationRenderUtil.renderWindowTitleBar(ctx, 0, curY, w, titleBarH, win, rootFrag, titleBar)
        curY += titleBarH

        // 2. Render MenuBar if present and not embedded in TitleBar
        if (menuBar != null && titleBar?.embedMenuBar != true) {
            val menuBarH = 28
            JestElectricNavigationRenderUtil.renderMenuBar(ctx, 0, curY, w, menuBarH, menuBar)
            curY += menuBarH
        }

        // 3. Render Navbar if present
        if (navBar != null && navBar.isVisible) {
            val navBarH = 42
            JestElectricNavigationRenderUtil.renderNavbar(ctx, 0, curY, w, navBarH, navBar, depth = 1)
            curY += navBarH
        }

        // Reserve bottom navigation bar if present
        val bottomNavH = if (bottomNav != null && bottomNav.isVisible) 44 else 0
        val bodyH = (h - curY - bottomNavH).coerceAtLeast(60)

        // 4. Render Main Content / Active Fragment
        val rootComponent: AbstractComponent? = rootFrag
            ?: win.getContent()
            ?: app.getContent()

        if (rootComponent != null) {
            renderComponent(ctx, 8, curY + 6, w - 16, bodyH - 12, rootComponent, depth = 0)
        }

        // 5. Render open DialogWindows if any
        val openDialogs = win.attachedDialogWindows.filter { it.isVisible }
        for (dlg in openDialogs) {
            JestElectricWorkspaceWindowRenderUtil.renderModalDialogOverlay(ctx, w, h, titleBarH, dlg)
        }

        // 6. Render BottomNavigationBar if present
        if (bottomNav != null && bottomNavH > 0) {
            JestElectricNavigationRenderUtil.renderBottomNav(ctx, 0, h - bottomNavH, w, bottomNavH, bottomNav)
        }

        // Outer window frame border
        JestElectricControlTintRenderUtil.drawRectBorder(buf, 0, 0, w, h, ctx.borderSubtle, 1)
        return buf
    }

    /**
     * Dispatches rendering of a single [comp] subtree at `(x, y, w, h)` to the appropriate
     * type-grouped `JestElectric*RenderUtil` class.
     */
    fun renderComponent(
        ctx: JestElectricRenderContext,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        comp: AbstractComponent,
        depth: Int = 0
    ) {
        if (!comp.isVisible || w <= 4 || h <= 4 || depth > 28) return

        // 1. Workspace, Docking, MDI, SplitPane, ScrollView, Dialog, Toast
        if (JestElectricWorkspaceWindowRenderUtil.canRender(comp) &&
            JestElectricWorkspaceWindowRenderUtil.render(ctx, x, y, w, h, comp, depth)
        ) {
            return
        }

        // 2. Navigation (TabContainer, TabbedFragment, MenuBar, Navbar, Toolbar, Breadcrumb, Pagination, Stepper, BottomNavigationBar, SideNav, Drawer, StatusBar)
        if (JestElectricNavigationRenderUtil.canRender(comp) &&
            JestElectricNavigationRenderUtil.render(ctx, x, y, w, h, comp, depth)
        ) {
            return
        }

        // 3. Data Display (Table, LazyGridTree, LazyList, LazyColumn, LazyRow, LazyGrid, TreeView, ImageView, Avatar, Popover, Tooltip)
        if (JestElectricDataDisplayRenderUtil.canRender(comp) &&
            JestElectricDataDisplayRenderUtil.render(ctx, x, y, w, h, comp, depth)
        ) {
            return
        }

        // 4. Slider & Progress (Slider, RangeSlider, ProgressBar, Spinner)
        if (JestElectricSliderProgressRenderUtil.canRender(comp) &&
            JestElectricSliderProgressRenderUtil.render(ctx, x, y, w, h, comp)
        ) {
            return
        }

        // 5. Toggle & Selection (Switch, CheckBox, RadioButton, RadioGroup, ComboBox, Select, DropdownButton)
        if (JestElectricToggleSelectionRenderUtil.canRender(comp) &&
            JestElectricToggleSelectionRenderUtil.render(ctx, x, y, w, h, comp, depth)
        ) {
            return
        }

        // 6. Input Fields (TextField, PasswordField, SearchField, NumberField, ColorPicker, DatePicker, TimePicker, TextArea)
        if (JestElectricInputFieldRenderUtil.canRender(comp) &&
            JestElectricInputFieldRenderUtil.render(ctx, x, y, w, h, comp)
        ) {
            return
        }

        // 7. Action Buttons (Button, IconButton, ToggleButton, SplitDropdownButton, FloatingActionButton, StyledButton, ButtonGroup)
        if (JestElectricButtonRenderUtil.canRender(comp) &&
            JestElectricButtonRenderUtil.render(ctx, x, y, w, h, comp, depth)
        ) {
            return
        }

        // 8. Typography & Text (Badge, Heading, Blockquote, CodeBlock, MarkdownView, Text, Label)
        if (JestElectricTextRenderUtil.canRender(comp) &&
            JestElectricTextRenderUtil.render(ctx, x, y, w, h, comp)
        ) {
            return
        }

        // 9. Media, Canvas, & Extension Surfaces
        if (JestElectricMediaCanvasRenderUtil.canRender(comp) &&
            JestElectricMediaCanvasRenderUtil.render(ctx, x, y, w, h, comp, depth)
        ) {
            return
        }

        // 10. Containers & Layout Chrome (Container, VStack, HStack, FlexBox, ColumnContainer, Card, Accordion, Alert, CollapsiblePanel, Divider, StyledContainer)
        if (JestElectricContainerChromeRenderUtil.canRender(comp)) {
            JestElectricContainerChromeRenderUtil.render(ctx, x, y, w, h, comp, depth)
        }
    }
}
