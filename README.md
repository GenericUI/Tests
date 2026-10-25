# GenericUI — `JestElectricRunner` UI Testing Suite Submodule (`:tests`)

> **Root Repository**: [**GenericUI (`../README.md`)**](../README.md) | **Root Documentation**: [SourceSet Hierarchy](../docs/sourcesets.md) · [Backends Guide](../docs/backends.md) · [Extensions Guide](../docs/extensions.md) · [Gradle Plugin](../docs/gradle-plugin.md) · [UI Testing](../docs/testing.md)

The **UI Testing Suite Submodule (`:tests`)** of **GenericUI** (`com.nexustools.genericui.tests.*`) provides **`JestElectricRunner`**—a unified, cross-platform UI testing framework that combines a **Jest-style BDD test & matcher DSL** (`describe`, `beforeAll`, `afterAll`, `beforeEach`, `afterEach`, `test`, `it`, `expect`, `expectComponent`, `expectNode`) with **Robolectric-inspired UI lifecycle & node controllers** (`launchApp`, `launchFragment`, `RoboAppController`, `RoboFragmentController`, `RoboNodeInteraction`, `RoboComponentExpectation`) and native platform runners across all 8 GenericUI backends.

---

## Submodule Architecture & Categorized Modules

| Gradle Module | Directory | Target Platforms | Key Classes | Responsibility | Detailed Guide |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`:tests`** | [`./`](./) | All KMP Targets | Umbrella Module | Re-exports `:tests:common` for multiplatform test sourceSets (`commonTest`). | [**`docs/jest-electric-runner.md`**](./docs/jest-electric-runner.md) |
| **`:tests:common`** | [`common/`](./common) | All KMP Targets | `JestElectricRunner`, `JestSuiteScope`, `JestTestScope`, `JestExpectation`, `RoboComponentExpectation`, `RoboAppController`, `RoboFragmentController`, `RoboNodeInteraction`, `RoboComponentTreeFinder` | Platform-agnostic BDD test runner, expectation matchers, component tree selectors, and lifecycle controllers. | [**`docs/jest-electric-runner.md`**](./docs/jest-electric-runner.md) |
| **`:tests:desktop:shared`** | [`desktop/shared/`](./desktop/shared) | `mingwX64`, `macosX64`, `macosArm64`, `linuxX64`, `linuxArm64` | `DesktopSharedRunnerNativeImpl` | Shared native desktop test runner bridge for `:windows`, `:macos`, `:gtk`, and `:qt`. | [**`docs/platform-test-runners.md`**](./docs/platform-test-runners.md) |
| **`:tests:desktop:windows`** | [`desktop/windows/`](./desktop/windows) | `mingwX64` | `WindowsVisualAppRunnerNativeImpl`, `WindowsJestElectricBootstrap` | **Visual Onscreen Win32 Runner**: Launches a real Win32 native window onscreen (`Win32NativeWindowHost`), displays a live test status banner (`[RUNNING]`, `[PASS]`, `[FAIL]`), and visually executes clicks, text input, hover/focus, and menu items in real time (`./gradlew windowsVisualTest`). | [**`docs/platform-test-runners.md`**](./docs/platform-test-runners.md) |
| **`:tests:desktop:macos`** | [`desktop/macos/`](./desktop/macos) | `macosX64`, `macosArm64` | `MacosJestElectricRunnerNativeImpl` | Native macOS AppKit `JestElectricRunnerNativeImpl` runner. | [**`docs/platform-test-runners.md`**](./docs/platform-test-runners.md) |
| **`:tests:desktop:gtk`** | [`desktop/gtk/`](./desktop/gtk) | `linuxX64`, `linuxArm64` | `GtkJestElectricRunnerNativeImpl` | Native Linux GTK4 `JestElectricRunnerNativeImpl` runner. | [**`docs/platform-test-runners.md`**](./docs/platform-test-runners.md) |
| **`:tests:desktop:qt`** | [`desktop/qt/`](./desktop/qt) | `linuxX64`, `linuxArm64` | `QtJestElectricRunnerNativeImpl` | Native Linux Qt6 `JestElectricRunnerNativeImpl` runner. | [**`docs/platform-test-runners.md`**](./docs/platform-test-runners.md) |
| **`:tests:desktop:swing`** | [`desktop/swing/`](./desktop/swing) | `jvm` | `SwingJUnitRunnerNativeImpl`, `JestElectricSwingTestBase` | Java Swing + JUnit 4 runner (`describeSwingSuite`) with headless and visual AWT/Swing EDT support. | [**`docs/platform-test-runners.md`**](./docs/platform-test-runners.md) |
| **`:tests:mobile:shared`** | [`mobile/shared/`](./mobile/shared) | `android`, `iosArm64`, `iosSimulatorArm64` | `MobileSharedRunnerNativeImpl` | Shared mobile test runner foundation for `:android` and `:ios`. | [**`docs/platform-test-runners.md`**](./docs/platform-test-runners.md) |
| **`:tests:mobile:android`** | [`mobile/android/`](./mobile/android) | `android` | `AndroidRobolectricRunnerNativeImpl`, `JestElectricRobolectricTestBase` | Android JUnit 4 + Robolectric runner (`describeAndroidSuite`) for testing `Fragment`s, `GenericUiFragmentActivity`, and Android Views without an emulator. | [**`docs/platform-test-runners.md`**](./docs/platform-test-runners.md) |
| **`:tests:mobile:ios`** | [`mobile/ios/`](./mobile/ios) | `iosArm64`, `iosSimulatorArm64` | `IosJestElectricRunnerNativeImpl` | Native iOS UIKit test runner (`iosSimulatorArm64Test`). | [**`docs/platform-test-runners.md`**](./docs/platform-test-runners.md) |
| **`:tests:web`** | [`web/`](./web) | `wasmJs`, `js(IR)` | `WebJestElectricRunnerNativeImpl` | Browser / DOM `JestElectricRunnerNativeImpl` runner for `:web`. | [**`docs/platform-test-runners.md`**](./docs/platform-test-runners.md) |

---

## Hierarchical Test SourceSet Integration

When you apply `id("com.nexustools.genericui")`, `:tests:common` and the appropriate platform test runner (`:tests:desktop:*`, `:tests:mobile:*`, `:tests:web`) are automatically wired into the corresponding `*Test` sourceSets:

```
commonTest (:tests:common)
 ├── webTest (:tests:web)
 └── systemCommonTest
      ├── desktopCommonTest
      │    ├── swingTest (:tests:desktop:swing)
      │    └── desktopNativeCommonTest (:tests:desktop:shared)
      │         ├── windowsTest (:tests:desktop:windows — WindowsVisualAppRunnerNativeImpl)
      │         ├── macTest (:tests:desktop:macos — MacosJestElectricRunnerNativeImpl)
      │         └── linuxTest
      │              ├── gtkTest (:tests:desktop:gtk — GtkJestElectricRunnerNativeImpl)
      │              └── qtTest (:tests:desktop:qt — QtJestElectricRunnerNativeImpl)
      └── mobileCommonTest (:tests:mobile:shared)
           ├── androidUnitTest (:tests:mobile:android — AndroidRobolectricRunnerNativeImpl)
           └── iosTest (:tests:mobile:ios — IosJestElectricRunnerNativeImpl)
```

For full sourceSet details, see [**Root SourceSet Hierarchy (`../docs/sourcesets.md`)**](../docs/sourcesets.md).

---

## Quick Example: Writing a Multi-Backend UI Test

```kotlin
import com.nexustools.genericui.tests.JestElectricRunner
import kotlin.test.Test
import kotlin.test.assertTrue

class StudioWorkspaceUiTest {

    @Test
    fun testCanvasCreationAndMenuWorkflow() {
        val report = JestElectricRunner.describe("Studio Workspace UI Suite") {
            beforeEach {
                // Runs before each test in this describe block
            }

            it("launches the app, clicks New Canvas, types a title, and verifies state") {
                val app = launchApp { MyStudioApp() }

                app.findById("new-canvas-btn")
                    .assertExists()
                    .assertIsVisible()
                    .assertIsEnabled()
                    .performClick()

                app.findById("canvas-title-input")
                    .assertExists()
                    .performFocus()
                    .performTypeText("Hero Banner 4K")
                    .assertTextEquals("Hero Banner 4K")

                app.clickMenu("File", "Save Project")
                expect(app.app.isProjectSaved).toBe(true)
            }
        }
        assertTrue(report.allPassed)
    }
}
```

---

## Testing Documentation Guides (`tests/docs/*.md`)

1. [**`JestElectricRunner` API & DSL Reference (`docs/jest-electric-runner.md`)**](./docs/jest-electric-runner.md) — Complete guide to `describe`, `beforeAll`, `afterAll`, `beforeEach`, `afterEach`, `test`, `it`, `expect` (`JestExpectation`), `expectComponent` / `expectNode` (`RoboComponentExpectation`), `launchApp` (`RoboAppController`), `launchFragment` (`RoboFragmentController`), and `RoboNodeInteraction`.
2. [**Platform-Specific Test Runners & Visual Testing (`docs/platform-test-runners.md`)**](./docs/platform-test-runners.md) — Complete guide to `WindowsVisualAppRunnerNativeImpl` (`windowsVisualTest` live onscreen Win32 testing), `SwingJUnitRunnerNativeImpl` (`JestElectricSwingTestBase`), `AndroidRobolectricRunnerNativeImpl` (`JestElectricRobolectricTestBase`), `WebJestElectricRunnerNativeImpl`, and macOS/GTK/Qt/iOS runners.

---

## Cross-References to Sibling Submodules & Root Docs

- **Root GenericUI Repository**: [`../README.md`](../README.md)
- **Root Architecture & Guides**:
  - [SourceSet Hierarchy (`../docs/sourcesets.md`)](../docs/sourcesets.md)
  - [Platform Backends Overview (`../docs/backends.md`)](../docs/backends.md)
  - [Extensions Architecture (`../docs/extensions.md`)](../docs/extensions.md)
  - [Gradle Plugin Reference (`../docs/gradle-plugin.md`)](../docs/gradle-plugin.md)
  - [UI Testing Guide (`../docs/testing.md`)](../docs/testing.md)
- **Sibling Submodules**:
  - **Desktop Backends (`:desktop`)**: [`../desktop/README.md`](../desktop/README.md) ([`:desktop:windows`](../desktop/docs/windows.md), [`:desktop:macos`](../desktop/docs/macos.md), [`:desktop:gtk`](../desktop/docs/gtk.md), [`:desktop:qt`](../desktop/docs/qt.md), [`:desktop:swing`](../desktop/docs/swing.md))
  - **Mobile Backends (`:mobile`)**: [`../mobile/README.md`](../mobile/README.md) ([`:mobile:android`](../mobile/docs/android.md), [`:mobile:ios`](../mobile/docs/ios.md))
  - **Web Backend (`:web`)**: [`../web/README.md`](../web/README.md)
  - **Nexus JavaScript Engine (`:NJS`)**: [`../NJS/README.md`](../lib/NJS/README.md)
