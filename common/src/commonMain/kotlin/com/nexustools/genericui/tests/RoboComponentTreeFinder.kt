package com.nexustools.genericui.tests

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.dock.DockArea
import com.nexustools.genericui.component.dock.DockGroup
import com.nexustools.genericui.component.dock.DockPanel
import com.nexustools.genericui.component.dock.DockWorkspace
import com.nexustools.genericui.component.dock.PanelWindow
import com.nexustools.genericui.component.form.CheckBox
import com.nexustools.genericui.component.form.Select
import com.nexustools.genericui.component.form.TextField
import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentContainer
import com.nexustools.genericui.component.fragment.helper.DockWorkspaceFragment
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.component.shell.FragmentApplication
import com.nexustools.genericui.component.shell.Menu
import com.nexustools.genericui.component.shell.MenuBar
import com.nexustools.genericui.component.shell.MenuItem
import com.nexustools.genericui.component.shell.Window
import com.nexustools.genericui.component.tabs.Tab
import com.nexustools.genericui.component.tabs.TabContainer
import com.nexustools.genericui.component.typography.Label
import com.nexustools.genericui.core.AbstractContainer
import com.nexustools.genericui.window.TitleBar

/**
 * Recursive scene-graph inspector and text extractor for GenericUI applications, windows,
 * fragments, dock workspaces, tab containers, and menus.
 */
object RoboComponentTreeFinder {

    /**
     * Collects all components reachable from [app], including its `mainWindow`, window chrome
     * (`MenuBar`, `Navbar`, `BottomNavigationBar`, `TitleBar`), active `Fragment`, and dock/tab trees.
     */
    fun collectFromApplication(app: Application): List<Component> {
        val visited = LinkedHashSet<Component>()
        collectInto(app.mainWindow, visited)
        app.getContent()?.let { collectInto(it, visited) }
        if (app is FragmentApplication) {
            collectInto(app.rootFragmentContainer, visited)
            app.currentFragment?.let { collectInto(it, visited) }
        }
        return visited.toList()
    }

    /**
     * Collects all components reachable from [root].
     */
    fun collectAllComponents(root: Component?): List<Component> {
        if (root == null) return emptyList()
        val visited = LinkedHashSet<Component>()
        collectInto(root, visited)
        return visited.toList()
    }

    private fun collectTitleBarComponents(titleBar: TitleBar?, visited: MutableSet<Component>) {
        if (titleBar == null) return
        titleBar.leadingComponents.forEach { collectInto(it, visited) }
        titleBar.centerComponents.forEach { collectInto(it, visited) }
        titleBar.trailingComponents.forEach { collectInto(it, visited) }
    }

    private fun collectInto(node: Component?, visited: MutableSet<Component>) {
        if (node == null || !visited.add(node)) return

        if (node is Window) {
            node.getContent()?.let { collectInto(it, visited) }
            node.getMenuBar()?.let { collectInto(it, visited) }
            node.getNavBar()?.let { collectInto(it, visited) }
            node.getBottomNavigationBar()?.let { collectInto(it, visited) }
            collectTitleBarComponents(node.getTitleBar(), visited)
        }

        if (node is Fragment) {
            node.getMenuBar()?.let { collectInto(it, visited) }
            node.getNavBar()?.let { collectInto(it, visited) }
            node.getBottomNavigationBar()?.let { collectInto(it, visited) }
            collectTitleBarComponents(node.getTitleBar(), visited)
        }

        if (node is FragmentContainer) {
            node.currentFragment?.let { collectInto(it, visited) }
        }

        if (node is DockWorkspaceFragment) {
            collectInto(node.dockWorkspace, visited)
            node.documents.forEach { doc ->
                collectInto(doc.content, visited)
                doc.detachedDocumentWindow?.let { collectInto(it, visited) }
            }
            node.detachedDockWindows.forEach { collectInto(it, visited) }
        }

        if (node is DockWorkspace) {
            collectInto(node.leftArea, visited)
            collectInto(node.rightArea, visited)
            collectInto(node.topArea, visited)
            collectInto(node.bottomArea, visited)
            collectInto(node.centerArea, visited)
            collectInto(node.centralDocumentTabs, visited)
            node.toolPalette?.let { collectInto(it, visited) }
            node.backgroundComponent?.let { collectInto(it, visited) }
            node.panels.values.forEach { collectInto(it, visited) }
            node.floatingWindows.values.forEach { collectInto(it, visited) }
            node.documents.forEach { doc ->
                collectInto(doc.content, visited)
                doc.detachedDocumentWindow?.let { collectInto(it, visited) }
            }
        }

        if (node is DockArea) {
            node.groups.forEach { collectInto(it, visited) }
        }

        if (node is DockGroup) {
            node.panels.forEach { collectInto(it, visited) }
        }

        if (node is DockPanel) {
            collectTitleBarComponents(node.getTitleBar(), visited)
        }

        if (node is PanelWindow) {
            node.panel?.let { collectInto(it, visited) }
        }

        if (node is TabContainer) {
            node.entries.forEach { entry ->
                collectInto(entry.tab, visited)
                entry.tab.customComponent?.let { collectInto(it, visited) }
                collectInto(entry.content, visited)
            }
        }

        if (node is Tab) {
            node.customComponent?.let { collectInto(it, visited) }
        }

        if (node is AbstractContainer) {
            node.children.forEach { collectInto(it, visited) }
        }
    }

    /**
     * Extracts the primary human-readable text or label from [component].
     */
    fun extractText(component: Component): String = when (component) {
        is Button -> component.text
        is Label -> component.text
        is TextField -> component.value.ifEmpty { component.placeholder }
        is CheckBox -> component.label
        is Select -> component.selectedValue ?: ""
        is DockPanel -> component.title
        is Tab -> component.displayTitle
        is Menu -> component.getLabel()
        is MenuItem -> component.getLabel()
        is Window -> component.getTitle()
        else -> ""
    }

    /**
     * Finds a [MenuItem] inside [app]'s `Window` or active `Fragment` [MenuBar] matching
     * [menuTitle] and [itemLabel].
     */
    fun findMenuItem(app: Application, menuTitle: String, itemLabel: String): MenuItem? {
        val menuBars = mutableListOf<MenuBar>()
        app.mainWindow.getMenuBar()?.let { menuBars.add(it) }
        if (app is FragmentApplication) {
            app.currentFragment?.getMenuBar()?.let {
                if (!menuBars.contains(it)) menuBars.add(it)
            }
        }
        for (bar in menuBars) {
            for (child in bar.children) {
                val menu = child as? Menu ?: continue
                if (menu.getLabel().equals(menuTitle, ignoreCase = true) ||
                    menu.getLabel().replace("&", "").equals(menuTitle.replace("&", ""), ignoreCase = true)
                ) {
                    val found = findMenuItemInMenu(menu, itemLabel)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun findMenuItemInMenu(menu: Menu, itemLabel: String): MenuItem? {
        val normalizedTarget = itemLabel.replace("&", "").trim()
        for (child in menu.children) {
            when (child) {
                is MenuItem -> {
                    val raw = child.getLabel()
                    val normalized = raw.replace("&", "").trim()
                    if (raw.equals(itemLabel, ignoreCase = true) ||
                        normalized.equals(normalizedTarget, ignoreCase = true) ||
                        normalized.startsWith(normalizedTarget, ignoreCase = true)
                    ) {
                        return child
                    }
                }
                is Menu -> {
                    val nested = findMenuItemInMenu(child, itemLabel)
                    if (nested != null) return nested
                }
            }
        }
        return null
    }
}
