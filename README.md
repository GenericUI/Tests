# `:tests` — Consolidated Multiplatform Testing & `JestElectricRunner`

> **Root Repository**: [**GenericUI (`../README.md`)**](../README.md) | **Master Docs**: [**`docs/testing.md`**](../docs/testing.md) · [**`docs/backends.md`**](../docs/backends.md)

## Overview
The **`:tests`** module ([`build.gradle.kts`](build.gradle.kts)) is a **single consolidated KMP module** (covering all 10 Kotlin targets and all 9 platform backends without child Gradle subprojects). It hosts **`JestElectricRunner`** — combining **Jest-style BDD syntax** (`describe`, `beforeEach`, `afterEach`, `test`, `it`, `expect`, `runSuite`) with **Robolectric-style lifecycle/node controllers** (`RoboAppController`, `RoboFragmentController`, `RoboNodeInteraction`, `RoboComponentExpectation`).

---

## SourceSet Architecture inside `:tests`

| `:tests` Source Set | Linked Backend Scope | Key Test Runner & Infrastructure |
|---------------------|----------------------|----------------------------------|
| **`commonMain` / `commonTest`** | `:common` | [`JestElectricRunner`](src/commonMain/kotlin/com/nexustools/genericui/tests/JestElectricRunner.kt), `JestSuiteScope`, `JestSuspendRunner`, `RoboAppController`, `RoboFragmentController`, `RoboNodeInteraction`, and 12 single-class `JestElectric*RenderUtil` snapshot utilities |
| **`systemMain`** | `:system` | Blink style/layout verification across all 8 non-web OS targets |
| **`desktopMain` / `nativeDesktopMain`** | `:desktop` | `DesktopSharedRunnerNativeImpl` desktop event loop & window test harness |
| **`jvmAndAndroidMain`** | `:jvm` | `JestScreenshotHostIo` shared screenshot & host filesystem helpers |
| **`mingwMain` / `mingwX64Test`** | `Windows` | `WindowsVisualAppRunnerNativeImpl` (`visualOnWindows = true` onscreen `HWND` vs. fast headless verification) + `WindowsBackdropBlurTest` |
| **`macosMain` / `macosArm64Test`** | `MacOS` (+ `:apple`) | `MacosJestElectricRunnerNativeImpl` (AppKit + Metal `CIContext` + squircle verification) |
| **`linuxMain` / `linuxTest`** | `Gtk` & `Qt` | Co-hosts both `GtkJestElectricRunnerNativeImpl` and `QtJestElectricRunnerNativeImpl` cleanly in one test binary (`LinuxGtkAndQtRunnerTest`) |
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

## `JestElectricButtonRenderUtil` Headless `backdropFilter`, Thread Dispatching, & `ServiceWorker` Testing

- **Headless `backdropFilter` Rendering (`JestElectricButtonRenderUtil` / `JestElectricContainerChromeRenderUtil`)**: Evaluates `CssFilter.backdropBlurOrFilter(styledSpec.backdropBlurPx, styledSpec.backdropFilter)` via `applyBackdropToImageBufferRegion(...)` on `StyledButton` and `StyledContainer` (keeping transparent `fillBg = null` when no explicit background/tint is set, and stripping `backdropFilter` on `TextBubble`), verified in `JestElectricRunnerTest.testSnapshotRendererAppliesBackdropBlurToStyledContainerAndStyledButton`.
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
