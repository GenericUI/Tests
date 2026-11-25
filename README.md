# `:tests` — Consolidated Multiplatform Testing & `JestElectricRunner`

> **Root Repository**: [**GenericUI (`../README.md`)**](../README.md) | **Master Docs**: [**`docs/testing.md`**](../docs/testing.md) · [**`docs/backends.md`**](../docs/backends.md)

## Overview
The **`:tests`** module ([`build.gradle.kts`](build.gradle.kts)) is a **single consolidated KMP module** (covering all 10 Kotlin targets and all 9 platform backends without child Gradle subprojects). It hosts **`JestElectricRunner`** — combining **Jest-style BDD syntax** (`describe`, `beforeEach`, `afterEach`, `test`, `it`, `expect`, `runSuite`) with **Robolectric-style lifecycle/node controllers** (`RoboAppController`, `RoboFragmentController`, `RoboNodeInteraction`, `RoboComponentExpectation`).

---

## SourceSet Architecture inside `:tests`

| `:tests` Source Set | Linked Backend Scope | Key Test Runner & Infrastructure |
|---------------------|----------------------|----------------------------------|
| **`commonMain` / `commonTest`** | `:common` | [`JestElectricRunner`](src/commonMain/kotlin/com/nexustools/genericui/tests/JestElectricRunner.kt), `JestSuiteScope`, `JestSuspendRunner`, `RoboAppController`, `RoboFragmentController`, `RoboNodeInteraction`, and 12 `:tests` `commonMain` headless snapshot utilities (`JestElectric*RenderUtil`) |
| **`systemMain`** | `:system` | Blink style/layout verification across all 8 non-web OS targets |
| **`desktopMain` / `nativeDesktopMain`** | `:desktop` | `DesktopSharedRunnerNativeImpl` desktop event loop, retained layout/style sync, & window test harness |
| **`jvmAndAndroidMain`** | `:jvm` | `JestScreenshotHostIo` shared screenshot & host filesystem helpers |
| **`mingwMain` / `mingwX64Test`** | `Windows` | `WindowsVisualAppRunnerNativeImpl` (`visualOnWindows = true` onscreen `HWND` capture via `PrintWindow(..., PW_RENDERFULLCONTENT)` vs. headless verification) + `WindowsBackdropBlurTest` |
| **`macosMain` / `macosArm64Test`** | `MacOS` (+ `:apple`) | `MacosJestElectricRunnerNativeImpl` (real AppKit `NSWindow`/`NSView` capture via `cacheDisplayInRect:toBitmapImageRep:` + Metal `CIContext` + squircle verification) |
| **`linuxMain` / `linuxTest`** | `Gtk` & `Kde` | Co-hosts **`GtkJestElectricRunnerNativeImpl`** (`GtkJestElectricBootstrap`, real widget capture via `GtkWidgetPaintable` + `GskRenderer`) and **`KdeJestElectricRunnerNativeImpl`** (`KdeJestElectricBootstrap`, real widget capture via `QWidget::render`) cleanly in [`LinuxGtkAndKdeRunnerTest`](src/linuxTest/kotlin/com/nexustools/genericui/tests/linux/LinuxGtkAndKdeRunnerTest.kt) |
| **`jvmMain` / `jvmTest`** | `Swing` (+ `:jvm`) | `SwingJUnitRunnerNativeImpl` & `JestElectricSwingTestBase` |
| **`mobileMain`** | `:mobile` | `MobileSharedRunnerNativeImpl` |
| **`androidMain` / `androidHostTest`** | `Android` (+ `:jvm`) | `AndroidRobolectricRunnerNativeImpl` & `JestElectricRobolectricTestBase` (Robolectric SDK 35) |
| **`iosMain` / `iosTest`** | `IOS` & `IPadOS` (+ `:apple`) | Co-hosts both `IosJestElectricRunnerNativeImpl` and `IpadosJestElectricRunnerNativeImpl` (`IosAndIpadosJestElectricRunnerTest`) |
| **`webMain` / `jsMain` / `wasmJsMain`** | `Web` (`:web`) | `WebJestElectricRunnerNativeImpl` & `WebTestHostBridge` (`wasmJsTest` + `jsTest` under Node) |

---

## Single-`commonTest` UI Testing with `JestElectricRunner`

```kotlin
import com.nexustools.genericui.tests.JestElectricRunner
import kotlin.test.Test

class CounterFragmentTest {
    @Test
    fun clickingIncrementUpdatesLabel() {
        val report = JestElectricRunner.runSuite("CounterFragment Suite") {
            test("increments counter on button click") {
                val controller = launchFragment(CounterFragment())

                controller.findById("counter-label").assertTextEquals("Count: 0")
                controller.findById("increment-btn").performClick()
                controller.findById("counter-label").assertTextEquals("Count: 1")
                controller.destroy()
            }

            test("verifies BootstrapType.NONE default & user CSS class isolation") {
                val controller = launchFragment(CounterFragment())
                val button = controller.findById("increment-btn").component!!

                button.addClass("custom-action")
                expect(button.hasClass("custom-action")).toBe(true)
                expect(button.getClasses()).toEqual(setOf("custom-action"))
                controller.destroy()
            }
        }
        report.assertAllPassed()
    }
}
```

---

## Headless Snapshots, Real OS Widget Surface Capture, Thread Dispatching, & `ServiceWorker` Testing

- **Headless `:common` Snapshots & Real Retained OS Surface Capture**: In `:tests` `commonMain`, [`JestElectricComponentSnapshotRenderer`](src/commonMain/kotlin/com/nexustools/genericui/test/jestelectric/render/JestElectricComponentSnapshotRenderer.kt) and the 12 headless `JestElectric*RenderUtil` helpers evaluate all 10 `CssFilter` / `backdrop-filter` ops (`CssFilter.backdropBlurOrFilter(styledSpec.backdropBlurPx, styledSpec.backdropFilter)` via `FastBackdropBlurKernel` & `CssFilterColorMatrix`) across `StyledComponent`, `StyledButton`, `StyledContainer`, and `Composite` components (`StyledSlider`, `SeekBar`, `ExpandingVolumeSlider` — keeping transparent `fillBg = null` when no explicit background/tint is set), alongside `ThemeStyleSheetRegistry` runtime theme updates, `SystemCssAnimationController` (`Animator`) `@keyframes`, and proximal `position: relative` containing-block layout. Separately, the desktop visual/platform runners (`MacosJestElectricRunnerNativeImpl`, `WindowsVisualAppRunnerNativeImpl`, `GtkJestElectricRunnerNativeImpl`, `KdeJestElectricRunnerNativeImpl`) capture **real mounted native OS widget hierarchies** via `cacheDisplayInRect:toBitmapImageRep:`, `PrintWindow(..., PW_RENDERFULLCONTENT)`, `GtkWidgetPaintable` + `GskRenderer`, and `QWidget::render`.
- **UI vs. Logic Dispatcher (`DefaultUiThreadDispatcherNativeImpl` / `JvmUiThreadDispatcherBase`)**: Handles `dispatchToUi`, `dispatchToLogic`, `runOnUiBlocking`, and headless inline execution automatically in tests (`JestSuspendRunner`).
- **`ServiceWorker` Test Harnesses**: Includes `ServiceWorkerExtension.resetForTests()`, `ServiceWorkerRuntimeAndIpcTest` (desktop loopback `127.0.0.1:0` IPC + `DesktopOsHelpers` + `WindowsShortcutWriter`), `AndroidServiceWorkerRobolectricTest` (Robolectric SDK 35 notification/action intents + `Binder` IPC), `AppleMobileServiceWorkerTest` (`isSupported == false`), `WebServiceWorkerBridgeTest`, and `ServiceWorkerCrossPlatformTest`.

---

## Visual Windows Mode & Convenience Commands

```kotlin
genericui {
    testing {
        enabled = true
        visualOnWindows = true
    }
}
```

```bash
./gradlew :tests:jvmTest :tests:wasmJsTest :tests:jsTest
./gradlew testSwing testWindows testMac testLinux testAndroid testIos testIpados testWeb windowsVisualTest
```

---

## Detailed Documentation
- [`JestElectricRunner` API Reference (`docs/jest-electric-runner.md`)](docs/jest-electric-runner.md)
- [Platform Test Runners (`docs/platform-test-runners.md`)](docs/platform-test-runners.md)
