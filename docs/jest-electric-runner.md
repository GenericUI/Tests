# `JestElectricRunner` — Multiplatform UI Testing API Reference

## Overview
`JestElectricRunner` (`com.nexustools.genericui.testing.JestElectricRunner` in `:tests:common`) is GenericUI's multiplatform UI test runner. It combines **Robolectric-style** synchronous lifecycle driving with **Jest / Testing Library-style** declarative widget queries, event simulation, and assertions.

---

## Core Entry Points (`UiTestContext`)

Inside `JestElectricRunner.runTest { ... }`, the receiver is a `UiTestContext` providing:

| Method | Description |
|--------|-------------|
| `launchFragment(args = FragmentBundle(), factory: () -> F): F` | Mounts a `Fragment` inside a `FragmentHostFragment` attached to a test `Window`, drives its lifecycle through `onAttach` $\rightarrow$ `onCreate` $\rightarrow$ `onCreateView` $\rightarrow$ `onStart` $\rightarrow$ `onResume`, and returns the fragment instance |
| `launchApp(mainFragment: () -> Fragment, fragments: Map<String, () -> Fragment>): FragmentHostFragment` | Boots a complete application context with registered `@AppFragment` factories, mounts the `@MainAppFragment`, and returns the root `FragmentHostFragment` |
| `launchWindow(title: String = "Test", width: Int = 1024, height: Int = 768, block: Window.() -> Unit): Window` | Creates and mounts a top-level `Window` directly for testing window chrome (`TitleBar`, `MenuBar`, `StatusBar`, `TitleCapabilities`) |
| `advanceTimeBy(delayMs: Long)` | Advances virtual `UiTimer` and animation clocks deterministically without sleeping |
| `IdleSynchronizer.waitForIdle()` | Flushes pending UI dispatcher tasks, layout passes, and fragment transactions |

---

## Querying Widgets (`WidgetMatcher`)

| Query Function | Matches |
|----------------|---------|
| `onWidgetWithId(id: String)` | Widget whose `id` matches `id` |
| `onWidgetWithText(text: String, substring: Boolean = false)` | `Label`, `Button`, `TextInput`, `Checkbox`, or other widget displaying `text` |
| `onWidgetWithTag(tag: String)` | Widget carrying the specified test tag or CSS class |
| `onAllWidgets(matcher: (Widget) -> Boolean)` | All widgets in the active window/fragment hierarchy satisfying `matcher` |

---

## Simulating User Interactions

| Action | Description |
|--------|-------------|
| `performClick()` | Dispatches a click/tap event if the widget is visible and enabled |
| `performDoubleClick()` | Dispatches a double-click event |
| `performTextInput(text: String)` | Focuses a `TextInput` / `TextArea` / `RichTextEditor` and replaces its text value |
| `performKeyPress(keyCode: Int, modifiers: Set<KeyModifier> = emptySet())` | Dispatches key down/up events |
| `performScrollTo(offsetX: Int, offsetY: Int)` | Scrolls a `ScrollView`, `LazyList`, or `LazyGrid` and triggers viewport virtualization updates |

---

## Verifying Styling, Cursors, `ThemeConfig`, Native Control Tinting, Window Chrome, & Navigation History

`JestElectricRunner` allows tests in `commonTest` to verify all styling, cursor, theme, chrome, and navigation invariants:
- **Strongly-Typed `ComponentStyle` (`getStyle()`) & `ComponentBootstrap` (`getBootstrap()`)**: Verify `BootstrapType.NONE` default, semantic `BootstrapType` tinting (`DANGER`, `PRIMARY`, etc.), 128-bit `CssProperty` `!important` bitmasks (`importantMaskLo` / `importantMaskHi`), `accentColor` (`accent-color`), and `CssCustomCursor` (`.cur` encoding via `WindowsCurCodec` on Windows, `NSImage` PNG encoding via `MacosCursorTranslator` on macOS). GenericUI does not expose string-keyed style APIs (`setStyleProperty(String, String)` is prohibited).
- **System Backend Typography vs. Aesthetic Style Restraints**: Verify that text elements (`Label`, `Text`, `Heading`, `Badge`, `Blockquote`, `CodeBlock`, `MarkdownView`) honor typography and text-color styles, standard controls/containers ignore aesthetic/typography `ComponentStyle` overrides on system targets and tint strictly via `BootstrapType`, and `StyledContainer` / `StyledButton` (and `:extensions:alien`) honor full `ComponentStyle` aesthetics across all backends.
- **Strict User CSS Class Isolation**: Verify that `getClasses()`, `getClass()`, and `hasClass()` return exclusively user-added classes (`customClasses` / `userClassList`) and never expose enum-managed classes.
- **`IApplication.addStyleSheet` / `removeStyleSheet`, `ThemeMode`, & `ThemeConfig`**: Verify stylesheet registration (`R.style.<name>`), `app.setTheme(ThemeMode.AUTO | ThemeMode.DARK | ThemeMode.LIGHT)` / `app.getResolvedTheme()`, and `app.setThemeConfig(config)` / `app.setThemeColor(argb)` (`BootstrapColorPalette`).
- **Single-Placement `TitleBar` & Optional Mobile/Web Navbar**: Verify `mountedInTitleBar == true` on desktop and `isTitleBarRequired == false` on mobile/web.

---

## Modular Snapshot Rendering Architecture (`JestElectric*RenderUtil` in `:tests:common`)

Headless component snapshot rendering in `:tests:common` (`com.nexustools.genericui.tests.render`) is organized into 12 focused, single-class-per-file `*RenderUtil` modules (replacing the legacy `JestElectricComponentSnapshotRenderer.kt` monolith):

| Class | Responsibility |
|-------|----------------|
| `JestElectricRenderContext` | Snapshot canvas state, viewport clipping stack, active `ThemeConfig` / `ThemeMode` resolution, and tree traversal |
| `JestElectricPrimitiveRenderUtil` | Pixel-accurate rounded rectangles, AA lines, borders, shadows, and bitmap blitting |
| `JestElectricControlTintRenderUtil` | Empty-text offscreen native control sprite synthesis and `DesktopControlTintRenderUtil`-compatible `BootstrapType` + `ThemeConfig` pixel tinting |
| `JestElectricActionControlRenderUtil` | `Button`, `StyledButton`, `IconButton`, `ToggleButton`, `ButtonGroup`, `DropdownButton`, `SplitButton`, `FloatingActionButton` |
| `JestElectricFormControlRenderUtil` | `TextField`, `PasswordField`, `SearchField`, `TextArea`, `NumberField`, `CheckBox`, `RadioButton`, `Switch`, `Select`, `ComboBox`, `RangeSlider` |
| `JestElectricFeedbackRenderUtil` | `ProgressBar`, `Spinner`, `Alert`, `Toast`, `Snackbar`, `Tooltip`, `Popover`, `Skeleton` |
| `JestElectricTypographyRenderUtil` | `Label`, `Text`, `Heading`, `Badge`, `Tag`, `Blockquote`, `CodeBlock`, `MarkdownView` (honoring full `ComponentStyle` typography and text color) |
| `JestElectricSurfaceRenderUtil` | `Card`, `Accordion`, `Paper`, `Callout`, `EmptyState`, `Divider`, `StyledContainer` |
| `JestElectricContainerLayoutRenderUtil` | `Container`, `HStack`, `VStack`, `ZStack`, `FlexBox` (including `FlexWrap.WRAP`), `Grid`, `ScrollView`, `SplitPane`, `CollapsiblePanel`, `ResizableBox`, `AspectRatioBox` |
| `JestElectricDockAndMdiRenderUtil` | `DockContainer`, `DockSplitNode`, `DockTabGroup`, `DockPanel`, `MdiContainer` (including `tileWindows` & `autoTileInitialWindows`), `MdiWindow`, `Drawer`, `BottomSheet`, `Modal` |
| `JestElectricNavigationAndShellRenderUtil` | `TitleBar`, `MenuBar`, `ToolBar`, `StatusBar`, `TabContainer`, `Breadcrumb`, `Pagination`, `Stepper`, `NavigationRail`, `BottomNavigationBar` |
| `JestElectricDataAndLazyRenderUtil` | `Table` (including `columnWeights`), `DataGrid`, `TreeView`, `Timeline`, `ListGroup`, `DescriptionList`, `StatCard`, `LazyList`, `LazyGrid` (including `minColumnWidthPx`), `Image`, `Avatar`, `Icon` |
