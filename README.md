# `:tests` — Multiplatform Testing & `JestElectricRunner`

## Overview
The `:tests` module hierarchy provides `JestElectricRunner` — a Robolectric + Jest inspired multiplatform UI testing framework — along with platform-specific test runner backends covering every GenericUI target (`:desktop:windows`, `:desktop:macos`, `:desktop:gtk`, `:desktop:qt`, `:desktop:swing`, `:mobile:android`, `:mobile:ios`, and `:web`).

---

## Module Hierarchy

```text
:tests                        # Aggregator & cross-platform verification suites
├── :tests:common             # JestElectricRunner, UiTestContext, WidgetMatcher, AssertionScope, MockSystemServices
├── :tests:desktop:nativeCommon # Native desktop test harness helpers (Win32, macOS, GTK, Qt)
├── :tests:desktop:windows    # Windows native (mingwX64) test runner + UxTheme / WindowsCurCodec + optional HWND visual mode
├── :tests:desktop:macos      # macOS native (macosX64, macosArm64) test runner + MacosCursorTranslator verification
├── :tests:desktop:gtk        # Linux GTK (linuxX64) test runner
├── :tests:desktop:qt         # Linux Qt (linuxX64) test runner
├── :tests:desktop:swing      # JVM Swing test runner
├── :tests:mobile:common      # Mobile test harness helpers (Android & iOS)
├── :tests:mobile:android     # Android test runner (Activity / View hierarchy inspection)
├── :tests:mobile:ios         # iOS (iosX64, iosArm64, iosSimulatorArm64) test runner
└── :tests:web                # Web (js, wasmJs) virtual DOM, WebDomElement class isolation, WebStyleSheetManager & History test runner
```

---

## Single-`commonTest` UI Testing with `JestElectricRunner`

Just as application code lives in `src/commonMain/kotlin`, UI tests live in `src/commonTest/kotlin` and run across every enabled backend without platform-specific test code:

### 1. Testing an Individual Fragment (`launchFragment`)
```kotlin
import com.nexustools.genericui.testing.JestElectricRunner
import kotlin.test.Test

class CounterFragmentTest {
    @Test
    fun clickingIncrementUpdatesLabel() = JestElectricRunner.runTest {
        val fragment = launchFragment { CounterFragment() }

        onWidgetWithId("counter-label").assertTextEquals("Count: 0")
        onWidgetWithId("increment-btn").performClick()
        onWidgetWithId("counter-label").assertTextEquals("Count: 1")
    }
}
```

### 2. Testing `ComponentStyle`, `ComponentBootstrap`, User Class Isolation, & `ThemeMode`
```kotlin
class StylingAndThemeTest {
    @Test
    fun verifiesBootstrapTypeNoneDefaultAndUserClassIsolation() = JestElectricRunner.runTest {
        val fragment = launchFragment { CounterFragment() }
        val button = onWidgetWithId("increment-btn").widget

        // Default BootstrapType is NONE; enum classes never pollute getClasses()
        button.addClass("custom-action")
        assert(button.hasClass("custom-action"))
        assert(button.getClasses() == setOf("custom-action"))
    }
}
```

---

## Generated Visual Test Runners (`testing { visualOnWindows = true }`)

By default, `:tests:desktop:windows` and all other test runners execute headlessly for fast CI verification. When debugging UI layout or native Win32 `UxTheme` rendering interactively, enable `visualOnWindows` in `build.gradle.kts`:

```kotlin
genericui {
    app {
        id = "com.example.myapp"
        name = "My App"
    }
    testing {
        visualOnWindows = true
    }
}
```

When `testing.visualOnWindows` is enabled (or `-Pgenericui.visualTests=true` is passed on the Gradle command line), `GenericUiGenerateLaunchersTask` generates `GeneratedWindowsVisualTestLauncher` in `mingwX64Test`, opening a real Win32 window during test execution.

---

## Detailed Documentation
- [`JestElectricRunner` API Reference (`docs/jest-electric-runner.md`)](docs/jest-electric-runner.md)
- [Platform Test Runners (`docs/platform-test-runners.md`)](docs/platform-test-runners.md)
