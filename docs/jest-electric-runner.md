# `JestElectricRunner` — Complete BDD & Robolectric UI Testing DSL Guide

> **Parent Submodule**: [`:tests` (`../README.md`)](../README.md) | **Root Repository**: [**GenericUI (`../../README.md`)**](../../README.md)  
> **Root Docs**: [SourceSet Hierarchy](../../docs/sourcesets.md) · [Backends Guide](../../docs/backends.md) · [Extensions Guide](../../docs/extensions.md) · [Gradle Plugin](../../docs/gradle-plugin.md) · [UI Testing](../../docs/testing.md)  
> **Companion Testing Guide**: [**Platform Test Runners & Visual Win32 Testing (`./platform-test-runners.md`)**](./platform-test-runners.md)

**`JestElectricRunner`** (`com.nexustools.genericui.tests.*` in `:tests:common`) is GenericUI's unified multiplatform testing engine. It blends the expressive BDD structure of **Jest** with the deterministic lifecycle and widget tree control of **Robolectric** and **Testing Library**.

---

## 1. Suite & Lifecycle Hooks (`JestElectricRunner` & `JestSuiteScope`)

Top-level suites are started via `JestElectricRunner.describe(...)` (or `JestElectricRunner.runSuite(...)`) and return a `JestSuiteReport`. Inside a `JestSuiteScope`, you can nest child `describe(...)` suites and register lifecycle hooks:

| DSL Function | Receiver / Signature | Execution Order & Behavior |
| :--- | :--- | :--- |
| `JestElectricRunner.describe(suiteName, visualOnWindows, throwOnFailure, runnerImpl) { ... }` | `JestSuiteScope.() -> Unit` | Runs a top-level test suite, prints `report.formatSummary()`, and throws an `AssertionError` if `throwOnFailure == true` and any test fails. |
| `JestElectricRunner.runVisualAppTestMain(suiteName, args) { ... }` | `JestSuiteScope.() -> Unit` | Standalone `main(args)` entry point for visual test executables (such as `windowsVisualTest`), supporting `--headless` or `--no-visual` CLI flags. |
| `describe(name) { ... }` | `JestSuiteScope.() -> Unit` | Defines a nested sub-suite (`"Parent Suite > Child Suite"`), inheriting all outer `beforeEach` and `afterEach` hooks. |
| `beforeAll { ... }` | `() -> Unit` | Runs once before any test in the current `describe` scope executes. |
| `afterAll { ... }` | `() -> Unit` | Runs once in a `finally` block after all tests and child suites in the current `describe` scope complete. |
| `beforeEach { ... }` | `JestTestScope.() -> Unit` | Runs before each individual `test` / `it` block (outer-to-inner for nested suites). |
| `afterEach { ... }` | `JestTestScope.() -> Unit` | Runs after each individual `test` / `it` block (inner-to-outer for nested suites), before automatic `cleanupTestResources()` destroys active app/fragment controllers. |
| `it(name) { ... }` / `test(name) { ... }` | `JestTestScope.() -> Unit` | Defines an individual test case with full access to `launchApp`, `launchFragment`, `expect`, `expectComponent`, `expectNode`, `pumpMainLooper`, and `advanceTimersByTime`. |

---

## 2. Value Matchers (`expect(actual)` $\rightarrow$ `JestExpectation<T>`)

Inside `JestTestScope`, `expect(actual)` returns a `JestExpectation<T>`. Every matcher supports negation via `.not` (e.g., `expect(value).not.toBe(0)`):

| Matcher Method | Description |
| :--- | :--- |
| `.toBe(expected: T)` | Asserts equality (`actual == expected`). |
| `.toEqual(expected: Any?)` | Asserts structural equality (`actual == expected`). |
| `.toBeNull()` / `.toBeNotNull()` | Asserts that `actual` is `null` or non-`null`. |
| `.toBeTruthy()` / `.toBeFalsy()` | Asserts JS/Jest-style truthiness (`Boolean`, non-zero/non-NaN `Number`, non-empty `CharSequence` or `Collection`, non-null object). |
| `.toContain(element: Any?)` | Asserts that a `String`/`CharSequence` contains a substring, an `Iterable`/`Array` contains an element, or a `Map` contains a key/value. |
| `.toHaveLength(expectedLength: Int)` | Asserts the length/size of a `CharSequence`, `Collection`, `Array`, or `Map`. |
| `.toBeGreaterThan(bound: Number)` | Asserts numeric `(actual as Number).toDouble() > bound.toDouble()`. |
| `.toBeLessThan(bound: Number)` | Asserts numeric `(actual as Number).toDouble() < bound.toDouble()`. |
| `.not` | Inverts any subsequent matcher on `JestExpectation<T>`. |

---

## 3. Component Matchers (`expectComponent(comp)` & `expectNode(node)` $\rightarrow$ `RoboComponentExpectation`)

`expectComponent(component)` and `expectNode(roboNodeInteraction)` return a `RoboComponentExpectation` (also invertible via `.not`):

| Matcher Method | Description |
| :--- | :--- |
| `.toExist()` | Asserts `component != null` (or `.not.toExist()` for `component == null`). |
| `.toBeVisible()` | Asserts `component != null && component.isVisible`. |
| `.toBeHidden()` | Asserts `component == null \|\| !component.isVisible`. |
| `.toBeEnabled()` | Asserts `component.isEnabled == true`. |
| `.toBeDisabled()` | Asserts `component.isEnabled == false`. |
| `.toHaveText(expected: String)` | Asserts exact extracted text match (`RoboComponentTreeFinder.extractText(node) == expected`). |
| `.toContainText(substring: String, ignoreCase: Boolean = true)` | Asserts extracted text contains `substring`. |
| `.toHaveClass(cssClass: String)` | Asserts `component.hasClass(cssClass)` (works with both Bootstrap 5 enum classes and custom CSS classes). |
| `.toHaveTooltip(expected: String, substring: Boolean = true)` | Asserts `component.tooltipText` matches `expected`. |
| `.toBeChecked()` | Asserts `(component as? CheckBox)?.isChecked == true`. |

---

## 4. Application & Fragment Lifecycle Controllers (`RoboAppController` & `RoboFragmentController`)

### `RoboAppController<A : Application>` (`launchApp { ... }`)
Calling `launchApp { MyApplication() }` inside a `test` / `it` block instantiates your `Application` (or `FragmentApplication`) and drives it through `setup()` (`create() -> start() -> resume() -> visible()`), automatically registering it for teardown at the end of the test.

- **Lifecycle Methods**: `create()`, `start()`, `resume()`, `visible()`, `setup()`, `pause()`, `stop()`, `destroy()`, `idleMainLooper(durationMs)`.
- **Component Tree Selectors**:
  - `findById(id: String): RoboNodeInteraction`
  - `findByText(text: String, substring: Boolean = false, ignoreCase: Boolean = true): RoboNodeInteraction`
  - `findByClass(cssClass: String): RoboNodeInteraction`
  - `findByTooltip(tooltip: String, substring: Boolean = true): RoboNodeInteraction`
  - `findAllButtons(): List<Button>`
  - `findAllComponents(): List<Component>`
- **Menu Bar Interaction**:
  - `clickMenu(menuTitle: String, itemLabel: String): RoboAppController<A>` — invokes a native menu item in the application's `TitleBar` / `MenuBar`.

### `RoboFragmentController<F : Fragment>` (`launchFragment { ... }`)
Calling `launchFragment { MyFragment() }` mounts a standalone `Fragment` inside an isolated test `Window` + `FragmentContainer` and drives it through the Android `Activity`-compatible lifecycle (`INITIALIZED` $\rightarrow$ `CREATED` $\rightarrow$ `STARTED` $\rightarrow$ `RESUMED`).

- **Lifecycle Methods**: `create()`, `start()`, `resume()`, `setup()`, `pause()`, `stop()`, `destroy()`, `idleMainLooper(durationMs)` (flushes fragment `setTimeout`/`setInterval` timers and steps CSS transitions).
- **Component Tree Selectors**: `findById`, `findByText`, `findByClass`, `findByTooltip`, `findAllButtons`, `findAllComponents`.

---

## 5. Node Queries, Assertions, & Simulated User Actions (`RoboNodeInteraction`)

Every `findById(...)`, `findByText(...)`, `findByClass(...)`, or `findByTooltip(...)` call returns a fluent `RoboNodeInteraction`:

| Category | Method | Description |
| :--- | :--- | :--- |
| **Assertions** | `.assertExists()` / `.assertDoesNotExist()` | Verifies the component was found (or not found) in the live tree. |
| **Assertions** | `.assertIsVisible()` / `.assertIsHidden()` | Verifies component visibility state. |
| **Assertions** | `.assertIsEnabled()` / `.assertIsDisabled()` | Verifies component enabled/disabled state. |
| **Assertions** | `.assertTextEquals(expected)` / `.assertTextContains(sub)` | Verifies extracted text of a `Button`, `Text`, `TextInput`, `Tab`, etc. |
| **Assertions** | `.assertHasClass(cssClass)` | Verifies Bootstrap/CSS class presence. |
| **Assertions** | `.assertHasTooltip(expected, substring)` | Verifies tooltip text. |
| **Actions** | `.performClick()` | Dispatches a native click through `runnerImpl.performNativeClick(node)`. Throws `AssertionError` if disabled or hidden. |
| **Actions** | `.performHover()` | Simulates pointer hover (`setHoveredState(true)` + native repaint). |
| **Actions** | `.performFocus()` | Simulates keyboard/input focus (`setFocusedState(true)` + native repaint). |
| **Actions** | `.performTypeText(text: String)` | Focuses a `TextInput` and types `text`, firing `onValueChanged` listeners and pumping UI events. |
| **Actions** | `.performSelectValue(value: String)` | Selects an option in a `Select` dropdown widget and fires `onSelectionChanged`. |
| **Actions** | `.performSetChecked(checked: Boolean)` | Toggles a `CheckBox` or Switch widget. |
| **Actions** | `.performSetSliderValue(value: Double)` | Updates a `Slider` / `RangeSlider` value and fires `onValueChanged`. |

---

## Complete End-to-End Code Examples

### 1. Testing a Multi-Panel Application & Menu Bar with `launchApp`

```kotlin
package com.example.studio.tests

import com.nexustools.genericui.tests.JestElectricRunner
import kotlin.test.Test
import kotlin.test.assertTrue

class HaEditorWorkspaceSuiteTest {

    @Test
    fun runWorkspaceSuite() {
        val report = JestElectricRunner.describe("HAEditor Workspace & Tool Palette Suite") {

            describe("Tool Selection & Layer Opacity Controls") {
                it("selects the Bezier Pen tool, adjusts layer opacity slider, and toggles visibility") {
                    val app = launchApp { HaEditorTestApp() }

                    // 1. Click a tool button by tooltip and verify active CSS class
                    val penToolBtn = app.findByTooltip("Bezier Pen Tool")
                        .assertExists()
                        .assertIsVisible()
                        .performHover()
                        .performClick()

                    expectNode(penToolBtn).toHaveClass("btn-primary")
                    expect(app.app.activeToolId).toBe("vector.pen")

                    // 2. Adjust the layer opacity slider and verify state
                    app.findById("layer-opacity-slider")
                        .assertIsEnabled()
                        .performSetSliderValue(0.75)

                    expect(app.app.activeLayerOpacity).toBe(0.75)

                    // 3. Toggle a CheckBox and trigger a top-level menu item
                    val lockCheck = app.findById("layer-alpha-lock-checkbox")
                        .performSetChecked(true)
                    expectNode(lockCheck).toBeChecked()

                    app.clickMenu("View", "Toggle Single/Multi Window Mode")
                    expect(app.app.isMultiWindowMode).toBe(true)
                }
            }
        }

        assertTrue(report.allPassed)
    }
}
```

### 2. Testing a Standalone `Fragment` Lifecycle & Timers with `launchFragment`

```kotlin
import com.nexustools.genericui.enums.fragment.FragmentState
import com.nexustools.genericui.tests.JestElectricRunner
import kotlin.test.Test

class PlayerFragmentLifecycleTest {

    @Test
    fun testFragmentLifecycleAndTimers() {
        JestElectricRunner.describe("Episode Caster PlayerFragment Lifecycle") {
            it("drives fragment from CREATED -> RESUMED -> PAUSED -> STOPPED -> DESTROYED and advances timers") {
                val controller = launchFragment { PlayerViewportFragment() }

                // launchFragment calls setup() -> fragment is in RESUMED state
                expect(controller.fragment.state).toBe(FragmentState.RESUMED)
                expect(controller.fragment.getNavBar()?.title).toContain("Now Playing")

                // Advance fragment timers (e.g. auto-hiding playback controls overlay after 3000ms)
                controller.findById("playback-overlay").assertIsVisible()
                advanceTimersByTime(3000L)
                controller.findById("playback-overlay").assertIsHidden()

                // Drive lifecycle transitions explicitly
                controller.pause()
                expect(controller.fragment.state).toBe(FragmentState.PAUSED)

                controller.resume()
                expect(controller.fragment.state).toBe(FragmentState.RESUMED)

                controller.stop()
                expect(controller.fragment.state).toBe(FragmentState.STOPPED)

                controller.destroy()
                expect(controller.fragment.state).toBe(FragmentState.DESTROYED)
            }
        }
    }
}
```

---

## Related Documentation

- **Parent Testing Overview**: [`../README.md`](../README.md)
- **Companion Testing Guide**: [**Platform Test Runners & Visual Win32 Testing (`./platform-test-runners.md`)**](./platform-test-runners.md)
- **Root GenericUI Documentation**: [Root README](../../README.md) · [SourceSet Hierarchy](../../docs/sourcesets.md) · [Backends Guide](../../docs/backends.md) · [Extensions Guide](../../docs/extensions.md) · [Gradle Plugin](../../docs/gradle-plugin.md) · [UI Testing](../../docs/testing.md)
- **Sibling Submodules**: [`:desktop`](../../desktop/README.md) · [`:mobile`](../../mobile/README.md) · [`:web`](../../web/README.md) · [`:NJS`](../../lib/NJS/README.md)
