# `JestElectricRunner` — Multiplatform UI Testing API Reference

> **Module Guide**: [**`:tests` (`../README.md`)**](../README.md) | **Master Docs**: [**`docs/testing.md`**](../../docs/testing.md) · [`platform-test-runners.md`](platform-test-runners.md)

## Overview
`JestElectricRunner` (`com.nexustools.genericui.tests.JestElectricRunner` inside `:tests` `commonMain`) is GenericUI's multiplatform UI test runner. It pairs **Robolectric-style** synchronous lifecycle controllers (`RoboAppController`, `RoboFragmentController`) with **Jest / Testing Library-style** declarative suite DSLs (`describe`, `beforeEach`, `afterEach`, `test`, `it`, `expect`), queries (`findById`, `findByText`), and user interaction simulation (`performClick`, `performHover`, `performFocus`, `performTypeText`).

---

## Core Entry Points (`JestSuiteScope` & Controllers)

Run suites via `JestElectricRunner.runSuite("Suite Name") { ... }`, `JestElectricRunner.runSuiteForTarget(target, "Suite Name") { ... }`, or `JestElectricRunner.runSuiteAcrossTargets(targets, "Suite Name") { ... }`. Inside each `test(...)` / `it(...)` block (`JestTestScope`):

| Helper / Method | Return Type | Behavior |
|-----------------|-------------|----------|
| `launchFragment(fragment, visualWindow = false)` | `RoboFragmentController<F>` | Mounts a `Fragment` into `FragmentTestWindow`, drives `INITIALIZED -> CREATED -> STARTED -> RESUMED` (`create()`, `start()`, `resume()`, `setup()`, `pause()`, `stop()`, `destroy()`, `idleMainLooper(ms)`), and supports queries |
| `launchApp(app, visualWindow = false)` | `RoboAppController<A>` | Boots an `Application` (`create()`, `start()`, `resume()`, `visible()`, `setup()`, `pause()`, `stop()`, `destroy()`, `idleMainLooper(ms)`), supporting `findById`, `findByText`, and `clickMenu` |
| `expect(value)` | `JestExpectation<T>` | Fluent assertions (`toBe`, `toEqual`, `toBeNull`, `toBeNotNull`, `toBeTruthy`, `toBeFalsy`, `toContain`, `toHaveLength`, `toBeGreaterThan`, `toBeLessThan`, plus `.not`) |
| `runSuiteAcrossTargets(...)` / `JestSuspendRunner` | `JestSuiteReport` | Executes across multiple backends cleanly using blocking dispatchers on JVM/Android/Native and promise/synchronous completion on Web |

---

## Querying & Interaction (`RoboNodeInteraction` & `RoboComponentExpectation`)

| Query / Action / Assertion | Behavior |
|----------------------------|----------|
| `findById(id: String)` / `findByText(text, substring = false, ignoreCase = true)` | Returns a `RoboNodeInteraction` over the active fragment/app tree |
| `performClick()` / `performHover()` / `performFocus()` | Dispatches click, hover, or focus events via `JestElectricRunnerNativeImpl` |
| `performTypeText(text: String)` | Enters text into focused input components |
| `assertExists()` / `assertDoesNotExist()` / `assertIsVisible()` / `assertIsHidden()` / `assertIsEnabled()` / `assertIsDisabled()` | Asserts widget existence, layout visibility, and enabled state |
| `assertTextEquals(expected)` / `assertTextContains(substring)` / `assertHasClass(cssClass)` / `assertHasTooltip(expected)` | Asserts displayed text, strict user CSS class isolation, and tooltips |

---

## Headless `backdropFilter`, Thread Dispatching, & Styling Verification

1. **`JestElectricButtonRenderUtil` & Headless `backdropFilter` Snapshots**:
   - All 12 single-class `JestElectric*RenderUtil` snapshot utilities (`com.nexustools.genericui.test.jestelectric.render`) work without opening native OS windows:
     - [`JestElectricButtonRenderUtil`](../src/commonMain/kotlin/com/nexustools/genericui/test/jestelectric/render/JestElectricButtonRenderUtil.kt) (`Button`, `StyledButton`, `IconButton`, `ToggleButton`, `SplitDropdownButton`, `FloatingActionButton`, `ButtonGroup`) and [`JestElectricContainerChromeRenderUtil`](../src/commonMain/kotlin/com/nexustools/genericui/test/jestelectric/render/JestElectricContainerChromeRenderUtil.kt) (`StyledContainer`, `Card`, `Container`, etc.) evaluate `CssFilter.backdropBlurOrFilter(styledSpec.backdropBlurPx, styledSpec.backdropFilter)` and invoke `applyBackdropToImageBufferRegion(...)` onto the `RGBA_8888` snapshot buffer (excluding `TextBubble`, which strips `backdropFilter`).
     - When `effBackdropFilter != null` and no custom/tinted background is configured on `StyledButton`, `fillBg` remains `null` so backdrop blur stays visible instead of filling opaque `ctx.bgElevated`. Verified in `JestElectricRunnerTest.testSnapshotRendererAppliesBackdropBlurToStyledContainerAndStyledButton`.
2. **Thread & Coroutine Dispatching (`DefaultUiThreadDispatcherNativeImpl` & `JvmUiThreadDispatcherBase`)**:
   - `DefaultUiThreadDispatcherNativeImpl` separates UI and Logic execution queues when `isSplitThreadingEnabled = true` (`dispatchToUi`, `dispatchToLogic`, `runOnUiBlocking`).
   - `JvmUiThreadDispatcherBase` backs Swing and Android using `Executors.newSingleThreadExecutor()` + `CountDownLatch` `runOnUiBlocking` synchronization while degrading gracefully to inline synchronous execution during headless unit/Robolectric tests where no AWT `EventQueue` or Android Main `Looper` is pumping.
3. **Typography vs. Style Restraints & Class Isolation**:
   - Verify `BootstrapType.NONE` native defaults, semantic `BootstrapType` tinting, user-only `customClasses` / `userClassList` isolation, `ThemeMode.AUTO` / `ThemeConfig`, and single-placement `TitleBar` (`mountedInTitleBar == true`).

---

## 12 Modular Snapshot Rendering Utilities (`com.nexustools.genericui.test.jestelectric.render`)

| Class | Responsibility |
|-------|----------------|
| `JestElectricRenderContext` | Snapshot `RGBA_8888` canvas state, viewport clip stack, `ThemeConfig` / `ThemeMode` resolution, and tree walk |
| `JestElectricControlTintRenderUtil` | Pixel primitives (`fillRect`, `fillRectAlpha`, rounded rects) and empty-text offscreen `BootstrapType` sprite tinting |
| `JestElectricButtonRenderUtil` | `Button`, `StyledButton` (with `backdropFilter` + transparent glass default), `IconButton`, `ToggleButton`, `SplitDropdownButton`, `FloatingActionButton`, `ButtonGroup` |
| `JestElectricContainerChromeRenderUtil` | `Container`, `VStack`/`HStack`/`FlexBox`/`Grid`, `StyledContainer` (with `backdropFilter`, excluding `TextBubble`), `Card`, `Accordion`, `Divider` |
| `JestElectricTextRenderUtil` | `Label`, `Text`, `Heading`, `Badge`, `Blockquote`, `CodeBlock`, `MarkdownView` typography & color rendering |
| `JestElectricInputFieldRenderUtil` | `TextInput`, `TextField`, `TextArea`, `SearchBox`, `NumberField`, `PasswordField` |
| `JestElectricToggleSelectionRenderUtil` | `CheckBox`, `RadioButton`, `Switch`, `Select`, `DropdownButton` |
| `JestElectricSliderProgressRenderUtil` | `RangeSlider`, `ProgressBar`, `Spinner`, `ExpandingVolumeSlider` |
| `JestElectricNavigationRenderUtil` | `TitleBar`, `MenuBar`, `TabContainer`, `Tab`, `Navbar`, `Drawer`, `BottomNavigationBar`, `Breadcrumb` |
| `JestElectricDataDisplayRenderUtil` | `Table`, `DataGrid`, `TreeView`, `LazyList`, `LazyGrid`, `Calendar`, `Timeline` |
| `JestElectricWorkspaceWindowRenderUtil` | `SplitPane`, `ScrollView`, `DockWorkspace`, `MdiContainer`, `DialogHost`, `ToastHost`, stage HUD overlays |
| `JestElectricMediaCanvasRenderUtil` | `Canvas2D`, `Canvas3D`, `VideoPlayer`, `AudioPlayer`, `WebView`, `ImageView` |
