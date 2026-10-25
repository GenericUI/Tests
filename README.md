# GenericUI — `JestElectricRunner` UI Testing Suite Submodule (`:tests`)

> **Main Repository**: [**GenericUI/GenericUI**](https://github.com/GenericUI/GenericUI)

This repository is the **UI Testing Suite Submodule (`:tests`)** of the [**GenericUI**](https://github.com/GenericUI/GenericUI) Kotlin Multiplatform Native-First UI framework. It provides **`JestElectricRunner`**, a cross-platform UI testing framework combining a **Jest-like BDD API** (`describe`, `beforeAll`, `afterAll`, `beforeEach`, `afterEach`, `it`, `test`, `expect`) with **Robolectric-inspired UI controllers** (`RoboAppController`, `RoboFragmentController`, `RoboNodeInteraction`, `RoboComponentExpectation`) and native platform runners across all 8 `GenericUI` backends.

---

## Submodule Structure

| Gradle Module | Directory | Target Platforms | Description |
| :--- | :--- | :--- | :--- |
| **`:tests`** | `./` | All KMP Targets | Umbrella dependency module re-exporting `:tests:common`. |
| **`:tests:common`** | `common/` | All KMP Targets | Core `JestElectricRunner` DSL, `JestExpectation`, `RoboComponentExpectation`, `RoboNodeInteraction`, `RoboComponentTreeFinder`, `RoboAppController`, and `RoboFragmentController`. |
| **`:tests:desktop:shared`** | `desktop/shared/` | `mingwX64`, `macosX64`, `macosArm64`, `linuxX64`, `linuxArm64` | Shared desktop test runner bridge (`DesktopSharedRunnerNativeImpl`). |
| **`:tests:desktop:windows`** | `desktop/windows/` | `mingwX64` | **Visual Onscreen Windows Runner** (`WindowsVisualAppRunnerNativeImpl` & `WindowsJestElectricBootstrap`) — launches a real Win32 window onscreen via `Win32NativeWindowHost`, displays a live test status banner, and pumps native Win32 messages and GDI/Direct2D repaints during test execution. |
| **`:tests:desktop:macos`** | `desktop/macos/` | `macosX64`, `macosArm64` | macOS AppKit `JestElectricRunner` native implementation (`MacosJestElectricRunnerNativeImpl`). |
| **`:tests:desktop:gtk`** | `desktop/gtk/` | `linuxX64`, `linuxArm64` | Linux GTK4 `JestElectricRunner` native implementation (`GtkJestElectricRunnerNativeImpl`). |
| **`:tests:desktop:qt`** | `desktop/qt/` | `linuxX64`, `linuxArm64` | Qt6 `JestElectricRunner` native implementation (`QtJestElectricRunnerNativeImpl`). |
| **`:tests:desktop:swing`** | `desktop/swing/` | `jvm` | Java Swing + JUnit 4 runner (`SwingJUnitRunnerNativeImpl` & `JestElectricSwingTestBase`) with Swing EDT synchronization. |
| **`:tests:mobile:shared`** | `mobile/shared/` | `android`, `iosArm64`, `iosSimulatorArm64` | Shared mobile test runner implementation (`MobileSharedRunnerNativeImpl`). |
| **`:tests:mobile:android`** | `mobile/android/` | `android` | Android JUnit + `RobolectricTestRunner` integration (`AndroidRobolectricRunnerNativeImpl` & `JestElectricRobolectricTestBase`). |
| **`:tests:mobile:ios`** | `mobile/ios/` | `iosArm64`, `iosSimulatorArm64` | iOS UIKit test runner implementation (`IosJestElectricRunnerNativeImpl`). |
| **`:tests:web`** | `web/` | `js(IR)` | Web DOM test runner implementation (`WebJestElectricRunnerNativeImpl`). |

---

## Example Usage

```kotlin
import com.nexustools.genericui.tests.JestElectricRunner
import kotlin.test.Test
import kotlin.test.assertTrue

class MyAppUiTest {
    @Test
    fun testMainApplicationFlow() {
        val report = JestElectricRunner.describe("Main Application UI Suite") {
            it("launches the app, interacts with controls, and verifies state") {
                val app = launchApp { MyGenericUiApp() }

                app.findByText("✨ New").assertExists().performClick()
                app.findById("canvas-title-input").performTypeText("Hero Banner")
                expect(app.app.hasOpenCanvas).toBe(true)
            }
        }
        assertTrue(report.allPassed)
    }
}
```

---

## Related GenericUI Repositories

- **Main Repository**: [https://github.com/GenericUI/GenericUI](https://github.com/GenericUI/GenericUI)
- **Desktop Backends (`:desktop`)**: [https://github.com/GenericUI/Desktop](https://github.com/GenericUI/Desktop)
- **Mobile Backends (`:mobile`)**: [https://github.com/GenericUI/Mobile](https://github.com/GenericUI/Mobile)
- **Web Backend (`:web`)**: [https://github.com/GenericUI/Web](https://github.com/GenericUI/Web)
- **Extensions (`:extensions`)**: [https://github.com/GenericUI/Extensions](https://github.com/GenericUI/Extensions)
