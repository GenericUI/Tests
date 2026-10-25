# Platform-Specific `JestElectricRunner` Runners & Live Visual Testing

> **Parent Submodule**: [`:tests` (`../README.md`)](../README.md) | **Root Repository**: [**GenericUI (`../../README.md`)**](../../README.md)  
> **Root Docs**: [SourceSet Hierarchy](../../docs/sourcesets.md) · [Backends Guide](../../docs/backends.md) · [Extensions Guide](../../docs/extensions.md) · [Gradle Plugin](../../docs/gradle-plugin.md) · [UI Testing](../../docs/testing.md)  
> **Companion Testing Guide**: [**`JestElectricRunner` API & DSL Reference (`./jest-electric-runner.md`)**](./jest-electric-runner.md)

Every `JestElectricRunner` test delegates platform-specific initialization, window realization, event-loop pumping, and native widget interaction to a **`JestElectricRunnerNativeImpl`** registered in **`JestElectricNativeRegistry`**. This allows the **exact same test code** to run across all 8 GenericUI backends—including live onscreen visual Win32 execution on Windows, JUnit 4 on Swing, Robolectric on Android, and DOM testing on Web.

---

## Overview of All Platform Test Runners

| Module | Runner Class | Base Test Helper | `backendName` | Execution Mode & Native Integration |
| :--- | :--- | :--- | :--- | :--- |
| **`:tests:desktop:windows`** | `WindowsVisualAppRunnerNativeImpl` | `WindowsJestElectricBootstrap` | `"windows-win32-visual"` / `"windows-win32-headless"` | Opens a real Win32 `HWND` window onscreen via `Win32NativeWindowHost`, updates a live titlebar banner (`[RUNNING]`, `[PASS]`, `[FAIL]`), invalidates GDI/Direct2D surfaces, and pumps Win32 messages (`stepDelayMs`) after every click, hover, focus, keystroke, and menu action. |
| **`:tests:desktop:swing`** | `SwingJUnitRunnerNativeImpl` | `JestElectricSwingTestBase` | `"swing-junit"` | Initializes `SwingNativeFactory`, runs inside JUnit 4 (`@Before` / `@After`), and supports both headless CI execution (`java.awt.headless = true`) and live Swing window inspection. |
| **`:tests:mobile:android`** | `AndroidRobolectricRunnerNativeImpl` | `JestElectricRobolectricTestBase` | `"android-robolectric"` | Initializes `AndroidNativeFactory` inside JUnit 4 + Robolectric (`testDebugUnitTest`), testing Android Views, `MaterialToolbar`, `RecyclerView` lazy pools, and `GenericUiFragmentActivity` on the local JVM without an emulator. |
| **`:tests:web`** | `WebJestElectricRunnerNativeImpl` | `JestElectricNativeRegistry` | `"web-dom"` | Initializes `WebNativeFactory` and executes tests against `WebDomElement` trees and `WebBrowserDomBridge` in both `wasmJsTest` and `jsTest`. |
| **`:tests:desktop:macos`** | `MacosJestElectricRunnerNativeImpl` | `DesktopSharedRunnerNativeImpl` | `"macos-appkit"` | Initializes `MacosNativeFactory` (`macosArm64Test` / `macosX64Test`) and tests AppKit `NSWindow`/`NSView` & global `NSMenu` trees. |
| **`:tests:desktop:gtk`** | `GtkJestElectricRunnerNativeImpl` | `DesktopSharedRunnerNativeImpl` | `"linux-gtk"` | Initializes `GtkNativeFactory` (`linuxX64Test` / `linuxArm64Test`) and tests GTK4 widget hierarchies. |
| **`:tests:desktop:qt`** | `QtJestElectricRunnerNativeImpl` | `DesktopSharedRunnerNativeImpl` | `"linux-qt"` | Initializes `QtNativeFactory` (`linuxX64Test` / `linuxArm64Test`) and tests Qt6 widget and `QDockWidget` hierarchies. |
| **`:tests:mobile:ios`** | `IosJestElectricRunnerNativeImpl` | `MobileSharedRunnerNativeImpl` | `"ios-uikit"` | Initializes `IosNativeFactory` (`iosSimulatorArm64Test`) and tests UIKit `UIView` and `UINavigationBar` hierarchies. |

---

## 1. Live Onscreen Visual Testing on Windows (`:tests:desktop:windows`)

On Windows ([`:desktop:windows`](../../desktop/docs/windows.md)), **`:tests:desktop:windows`** provides **`WindowsVisualAppRunnerNativeImpl`** and **`WindowsJestElectricBootstrap`**.

When `visualWindowEnabled = true` (the default for `./gradlew windowsVisualTest`), `WindowsVisualAppRunnerNativeImpl`:
1. Initializes `WindowsNativeFactory` with `WindowsApplicationNativeImpl.interactiveWin32GuiEnabled = true` and `Win32NativeWindowHost.visualTestMode = true`.
2. Creates and displays the real Win32 native `HWND` window on your monitor when `launchApp { ... }` is called.
3. Updates `Win32NativeWindowHost.visualTestBannerText` in the window's titlebar/header as each test progresses:
   - `"JestElectricRunner | <SuiteName>"` on launch
   - `"[RUNNING] <SuitePath> > <TestName>"` while executing
   - `"[PASS] <SuitePath> > <TestName>"` or `"[FAIL] <SuitePath> > <TestName>"` on completion
4. Calls `Win32NativeWindowHost.invalidateAllWindows()` and `Win32NativeWindowHost.pumpVisualWindowEvents(stepDelayMs)` after every `performClick()`, `performHover()`, `performFocus()`, `performTypeText()`, and `clickMenu()` so you can visually watch buttons depress, text fields populate, and panels dock/undock in real time.
5. Automatically closes all visual Win32 windows in `closeVisualWindow()` when the suite finishes.

### Example: Standalone & Unit Visual Win32 Test (`WindowsJestElectricBootstrap`)

```kotlin
package com.example.studio.windows.test

import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.tests.windows.WindowsJestElectricBootstrap
import kotlin.test.Test
import kotlin.test.assertTrue

class StudioWindowsVisualTest {

    @Test
    fun testWin32StudioWindowVisually() {
        // Install the visual Win32 runner (stepDelayMs = 32ms between visual UI actions):
        WindowsJestElectricBootstrap.install(
            visualWindowEnabled = true,
            stepDelayMs = 32L
        )

        val report = JestElectricRunner.describe("Win32 Visual Studio UI Suite") {
            it("opens a live Win32 HWND window and interacts with controls visibly") {
                val app = launchApp { MyWin32StudioApp() }

                app.findById("create-layer-btn")
                    .assertExists()
                    .performHover()
                    .performClick()

                app.findById("layer-name-input")
                    .performFocus()
                    .performTypeText("Background Matte")
                    .assertTextEquals("Background Matte")
            }
        }
        assertTrue(report.allPassed)
    }
}

// Optional standalone main entry point for ./gradlew windowsVisualTest
// (supports --headless or --no-visual CLI flags for CI servers):
fun main(args: Array<String>) {
    val visual = args.none { it == "--headless" || it == "--no-visual" }
    WindowsJestElectricBootstrap.install(visualWindowEnabled = visual, stepDelayMs = 24L)
    JestElectricRunner.runVisualAppTestMain("Win32 Visual Studio Suite", args) {
        it("runs the full visual regression suite") {
            val app = launchApp { MyWin32StudioApp() }
            app.findById("create-layer-btn").performClick()
        }
    }
}
```

---

## 2. Java Swing + JUnit 4 Runner (`:tests:desktop:swing`)

For [`:desktop:swing`](../../desktop/docs/swing.md), extend **`JestElectricSwingTestBase`** (which automatically registers `SwingJUnitRunnerNativeImpl` before each `@Test` and resets the registry afterward) and use `describeSwingSuite(...)`:

```kotlin
package com.example.studio.swing.test

import com.nexustools.genericui.tests.swing.JestElectricSwingTestBase
import org.junit.Test
import kotlin.test.assertTrue

class StudioSwingUiTest : JestElectricSwingTestBase(headless = true) {

    @Test
    fun testSwingWorkspaceComponents() {
        val report = describeSwingSuite("Swing Studio JUnit 4 Suite") {
            it("mounts Swing JComponent peers and verifies interactions") {
                val app = launchApp { MySwingStudioApp() }

                app.findByText("Export PNG")
                    .assertExists()
                    .assertIsEnabled()
                    .performClick()

                expect(app.app.exportTriggered).toBe(true)
            }
        }
        assertTrue(report.allPassed)
    }
}
```

---

## 3. Android Robolectric + JUnit 4 Runner (`:tests:mobile:android`)

For [`:mobile:android`](../../mobile/docs/android.md), extend **`JestElectricRobolectricTestBase`** (backed by `AndroidRobolectricRunnerNativeImpl` and `MobileSharedRunnerNativeImpl`) and use `describeAndroidSuite(...)`:

```kotlin
package com.example.episodecaster.android.test

import com.nexustools.genericui.tests.android.JestElectricRobolectricTestBase
import org.junit.Test
import kotlin.test.assertTrue

class EpisodeCasterAndroidRobolectricTest : JestElectricRobolectricTestBase() {

    @Test
    fun testAdaptiveFragmentOnAndroid() {
        val report = describeAndroidSuite("Episode Caster Android Robolectric Suite") {
            it("mounts EpisodeCasterMainFragment and interacts with queue controls") {
                val frag = launchFragment { EpisodeCasterMainFragment() }

                frag.findById("play-pause-btn")
                    .assertExists()
                    .assertIsVisible()
                    .performClick()

                expect(frag.fragment.isPlaying).toBe(true)
            }
        }
        assertTrue(report.allPassed)
    }
}
```

---

## 4. Web HTML5 DOM Runner (`:tests:web`) & Native macOS / GTK / Qt / iOS Runners

On `:web` (`WebJestElectricRunnerNativeImpl`), `:desktop:macos` (`MacosJestElectricRunnerNativeImpl`), `:desktop:gtk` (`GtkJestElectricRunnerNativeImpl`), `:desktop:qt` (`QtJestElectricRunnerNativeImpl`), and `:mobile:ios` (`IosJestElectricRunnerNativeImpl`), register the platform runner in `JestElectricNativeRegistry` (or let the GenericUI Gradle plugin test bootstrapper register it automatically):

```kotlin
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.tests.web.WebJestElectricRunnerNativeImpl
import kotlin.test.Test

class WebDomComponentsTest {

    @Test
    fun testWebDomLazyListAndButtons() {
        JestElectricNativeRegistry.register(WebJestElectricRunnerNativeImpl())

        JestElectricRunner.describe("Web DOM UI Suite") {
            it("renders semantic HTML5 DOM elements and verifies lazy list windowing") {
                val app = launchApp { StudioWebApp() }

                app.findById("import-folder-btn")
                    .assertExists()
                    .assertHasClass("btn-primary")
                    .performClick()
            }
        }
    }
}
```

---

## Related Documentation

- **Parent Testing Overview**: [`../README.md`](../README.md)
- **Companion Testing Guide**: [**`JestElectricRunner` API & DSL Reference (`./jest-electric-runner.md`)**](./jest-electric-runner.md)
- **Root GenericUI Documentation**: [Root README](../../README.md) · [SourceSet Hierarchy](../../docs/sourcesets.md) · [Backends Guide](../../docs/backends.md) · [Extensions Guide](../../docs/extensions.md) · [Gradle Plugin](../../docs/gradle-plugin.md) · [UI Testing](../../docs/testing.md)
- **Sibling Submodules**: [`:desktop`](../../desktop/README.md) · [`:mobile`](../../mobile/README.md) · [`:web`](../../web/README.md) · [`:NJS`](../../lib/NJS/README.md)
