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

## Verifying Styling, Cursors, Theming, Window Chrome, & Navigation History

`JestElectricRunner` allows tests in `commonTest` to verify all styling, cursor, theme, chrome, and navigation invariants:
- **`ComponentStyle` (`getStyle()`) & `ComponentBootstrap` (`getBootstrap()`)**: Verify `BootstrapType.NONE` default, semantic `BootstrapType` tinting (`DANGER`, `PRIMARY`, etc.), `!important` property flags, `accentColor` (`accent-color`), and `CssCustomCursor` (`.cur` encoding via `WindowsCurCodec` on Windows, `NSImage` PNG encoding via `MacosCursorTranslator` on macOS).
- **Strict User CSS Class Isolation**: Verify that `getClasses()`, `getClass()`, and `hasClass()` return exclusively user-added classes (`customClasses` / `userClassList`) and never expose enum-managed classes.
- **`IApplication.addStyleSheet` / `removeStyleSheet` & `ThemeMode`**: Verify stylesheet registration (`R.style.<name>`) and `app.setTheme(ThemeMode.AUTO | ThemeMode.DARK | ThemeMode.LIGHT)` / `app.getResolvedTheme()`.
- **Single-Placement `TitleBar` & Optional Mobile/Web Navbar**: Verify `mountedInTitleBar == true` on desktop and `isTitleBarRequired == false` on mobile/web.
