package com.nexustools.genericui.tests

import com.nexustools.genericui.core.NativeFactory
import com.nexustools.genericui.nativeimpl.action.ButtonNativeImpl
import com.nexustools.genericui.nativeimpl.action.DropdownNativeImpl
import com.nexustools.genericui.nativeimpl.content.ImageNativeImpl
import com.nexustools.genericui.nativeimpl.content.TableNativeImpl
import com.nexustools.genericui.nativeimpl.dialog.DialogWindowNativeImpl
import com.nexustools.genericui.nativeimpl.dialog.FileBrowserDialogNativeImpl
import com.nexustools.genericui.nativeimpl.dialog.StandardDialogsNativeImpl
import com.nexustools.genericui.nativeimpl.dock.DockAreaNativeImpl
import com.nexustools.genericui.nativeimpl.dock.DockGroupNativeImpl
import com.nexustools.genericui.nativeimpl.dock.DockPanelNativeImpl
import com.nexustools.genericui.nativeimpl.dock.DockWindowNativeImpl
import com.nexustools.genericui.nativeimpl.dock.DockWorkspaceNativeImpl
import com.nexustools.genericui.nativeimpl.dock.ToolPaletteNativeImpl
import com.nexustools.genericui.nativeimpl.document.BookContainerNativeImpl
import com.nexustools.genericui.nativeimpl.feedback.ProgressBarNativeImpl
import com.nexustools.genericui.nativeimpl.feedback.SpinnerNativeImpl
import com.nexustools.genericui.nativeimpl.form.CheckboxNativeImpl
import com.nexustools.genericui.nativeimpl.form.RangeSliderNativeImpl
import com.nexustools.genericui.nativeimpl.form.SelectNativeImpl
import com.nexustools.genericui.nativeimpl.form.TextInputNativeImpl
import com.nexustools.genericui.nativeimpl.layout.ContainerNativeImpl
import com.nexustools.genericui.nativeimpl.layout.DividerNativeImpl
import com.nexustools.genericui.nativeimpl.layout.ScrollViewNativeImpl
import com.nexustools.genericui.nativeimpl.lazy.LazyGridNativeImpl
import com.nexustools.genericui.nativeimpl.lazy.LazyListNativeImpl
import com.nexustools.genericui.nativeimpl.lazy.TreeViewNativeImpl
import com.nexustools.genericui.nativeimpl.navigation.BottomNavigationBarNativeImpl
import com.nexustools.genericui.nativeimpl.navigation.NavbarNativeImpl
import com.nexustools.genericui.nativeimpl.overlay.PopoverNativeImpl
import com.nexustools.genericui.nativeimpl.shell.ApplicationNativeImpl
import com.nexustools.genericui.nativeimpl.shell.ContextMenuNativeImpl
import com.nexustools.genericui.nativeimpl.shell.MenuBarNativeImpl
import com.nexustools.genericui.nativeimpl.shell.MenuItemNativeImpl
import com.nexustools.genericui.nativeimpl.shell.MenuNativeImpl
import com.nexustools.genericui.nativeimpl.shell.SplitPaneNativeImpl
import com.nexustools.genericui.nativeimpl.shell.WindowNativeImpl
import com.nexustools.genericui.nativeimpl.surface.CollapseNativeImpl
import com.nexustools.genericui.nativeimpl.system.NetworkMonitorNativeImpl
import com.nexustools.genericui.nativeimpl.system.NotificationManagerNativeImpl
import com.nexustools.genericui.nativeimpl.system.PermissionManagerNativeImpl
import com.nexustools.genericui.nativeimpl.system.SystemTrayNativeImpl
import com.nexustools.genericui.nativeimpl.tabs.TabContainerNativeImpl
import com.nexustools.genericui.nativeimpl.tabs.TabNativeImpl
import com.nexustools.genericui.nativeimpl.typography.MarkdownViewNativeImpl
import com.nexustools.genericui.nativeimpl.typography.TextNativeImpl

/**
 * Internal marker [NativeFactory] used by [DefaultJestElectricRunnerNativeImpl] and [RoboFragmentController]
 * to satisfy `FragmentContainer.resolveActiveFactory()` checks during headless lifecycle transitions.
 */
internal object HeadlessLifecycleMarkerFactory : NativeFactory {
    override val backendName: String = "headless-marker"
    private fun unsupported(): Nothing = throw UnsupportedOperationException("Headless marker factory")
    override fun createApplication(): ApplicationNativeImpl<*> = unsupported()
    override fun createWindow(): WindowNativeImpl<*> = unsupported()
    override fun createMenuBar(): MenuBarNativeImpl<*> = unsupported()
    override fun createMenu(): MenuNativeImpl<*> = unsupported()
    override fun createMenuItem(checkable: Boolean, radio: Boolean): MenuItemNativeImpl<*> = unsupported()
    override fun createContextMenu(): ContextMenuNativeImpl<*> = unsupported()
    override fun createSplitPane(): SplitPaneNativeImpl<*> = unsupported()
    override fun createDockWorkspace(): DockWorkspaceNativeImpl<*> = unsupported()
    override fun createDockArea(): DockAreaNativeImpl<*> = unsupported()
    override fun createDockGroup(): DockGroupNativeImpl<*> = unsupported()
    override fun createDockPanel(): DockPanelNativeImpl<*> = unsupported()
    override fun createDockWindow(): DockWindowNativeImpl<*> = unsupported()
    override fun createToolPalette(): ToolPaletteNativeImpl<*> = unsupported()
    override fun createContainer(classPrefix: String): ContainerNativeImpl<*> = unsupported()
    override fun createBookContainer(): BookContainerNativeImpl<*> = unsupported()
    override fun createScrollView(): ScrollViewNativeImpl<*> = unsupported()
    override fun createDivider(): DividerNativeImpl<*> = unsupported()
    override fun createText(tagName: String, classPrefix: String): TextNativeImpl<*> = unsupported()
    override fun createMarkdownView(): MarkdownViewNativeImpl<*> = unsupported()
    override fun createImage(): ImageNativeImpl<*> = unsupported()
    override fun createTable(): TableNativeImpl<*> = unsupported()
    override fun createButton(classPrefix: String): ButtonNativeImpl<*> = unsupported()
    override fun createDropdown(): DropdownNativeImpl<*> = unsupported()
    override fun createTextInput(multiline: Boolean): TextInputNativeImpl<*> = unsupported()
    override fun createSelect(): SelectNativeImpl<*> = unsupported()
    override fun createCheckbox(switchStyle: Boolean, radioStyle: Boolean): CheckboxNativeImpl<*> = unsupported()
    override fun createRangeSlider(): RangeSliderNativeImpl<*> = unsupported()
    override fun createCollapse(): CollapseNativeImpl<*> = unsupported()
    override fun createTab(closable: Boolean): TabNativeImpl<*> = unsupported()
    override fun createTabContainer(): TabContainerNativeImpl<*> = unsupported()
    override fun createNavbar(): NavbarNativeImpl<*> = unsupported()
    override fun createBottomNavigationBar(): BottomNavigationBarNativeImpl<*> = unsupported()
    override fun createProgressBar(): ProgressBarNativeImpl<*> = unsupported()
    override fun createSpinner(): SpinnerNativeImpl<*> = unsupported()
    override fun createPopover(): PopoverNativeImpl<*> = unsupported()
    override fun createDialogWindow(): DialogWindowNativeImpl<*> = unsupported()
    override fun createFileBrowserDialog(): FileBrowserDialogNativeImpl = unsupported()
    override fun createStandardDialogs(): StandardDialogsNativeImpl = unsupported()
    override fun createLazyList(): LazyListNativeImpl<*> = unsupported()
    override fun createLazyGrid(): LazyGridNativeImpl<*> = unsupported()
    override fun createTreeView(): TreeViewNativeImpl<*> = unsupported()
    override fun createPermissionManager(): PermissionManagerNativeImpl = unsupported()
    override fun createSystemTray(): SystemTrayNativeImpl = unsupported()
    override fun createNotificationManager(): NotificationManagerNativeImpl = unsupported()
    override fun createNetworkMonitor(): NetworkMonitorNativeImpl = unsupported()
}
