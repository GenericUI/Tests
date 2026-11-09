# Platform Test Runners (`:tests:*`)

## Overview
Each GenericUI backend has a corresponding test runner submodule under `:tests` that wires `JestElectricRunner` to that backend's widget factory, window implementation, resource resolver, stylesheet manager, and lifecycle dispatcher.

---

## Submodule Matrix

| Test Module | Backend Under Test | Target(s) | Execution Mode |
|-------------|--------------------|-----------|----------------|
| `:tests:common` | `:common` | All KMP targets | Core `JestElectricRunner` engine, `MockSystemServices`, strongly-typed `ComponentStyle` / `ComponentBootstrap` / `PrecompiledStyleSheet` / `ThemeConfig` assertions, 12 modular `JestElectric*RenderUtil` snapshot renderers, and common assertion DSL |
| `:tests:desktop:nativeCommon` | `:desktop:nativeCommon` | `mingwX64`, `macosX64`, `macosArm64`, `linuxX64` | Native desktop container, `DesktopBaseNativeImpl` user class isolation, `DesktopRenderableNativeImpl`, `DesktopControlTintRenderUtil` offscreen empty-text control sprite tinting + contrasting text overlay, `DesktopLazyViewportEngine`, and docking test helpers |
| `:tests:desktop:windows` | `:desktop:windows` | `mingwX64` | Headless Win32 widget/layout, 12 `Win32*RenderUtil` classes + 36 `Windows*NativeImpl` classes, `Win32DwmUxThemeBridge` (`UxTheme`), and `WindowsCurCodec` (`.cur` binary synthesis) verification by default; real Win32 `HWND` window rendering when `testing.visualOnWindows = true` (`GeneratedWindowsVisualTestLauncher`) |
| `:tests:desktop:macos` | `:desktop:macos` | `macosX64`, `macosArm64` | AppKit `NSWindow` / `NSView` widget tree, `MacosControlRenderUtil` offscreen `NSCell` / `NSButtonCell` sprite tinting, `MacosCursorTranslator` (`NSCursor`), and `MacosResourceResolverImpl` verification |
| `:tests:desktop:gtk` | `:desktop:gtk` | `linuxX64` | GTK4 widget hierarchy, `GtkControlRenderUtil` offscreen GTK CSS theme sprite tinting, `GtkHeaderBar` single-placement titlebar, `GdkCursor`, and Linux XDG storage verification |
| `:tests:desktop:qt` | `:desktop:qt` | `linuxX64` | Qt6 `QMainWindow` / `QWidget` hierarchy, `QtControlRenderUtil` offscreen `QStyle` / `QPainter` sprite tinting, `QCursor`, and `QtResourceResolverImpl` verification |
| `:tests:desktop:swing` | `:desktop:swing` | `jvm` | Swing EDT (`JFrame` / `JComponent`) hierarchy, layout, offscreen native Look-and-Feel (`UIManager.getSystemLookAndFeelClassName()`) empty-text sprite tinting (`DesktopControlTintRenderUtil`), `Toolkit.createCustomCursor`, and event simulation |
| `:tests:mobile:common` | `:mobile:common` | `androidTarget`, `iosX64`, `iosArm64`, `iosSimulatorArm64` | Mobile `MobileBaseNativeImpl`, `MobileLazyViewportEngine`, system backend typography vs. aesthetic style restraints, and optional navbar chrome test helpers |
| `:tests:mobile:android` | `:mobile:android` | `androidTarget` | Verifies `GenericUiFragmentActivity`, 1:1 `@AppFragment` Activity intent routing (`AndroidActivityRegistry`), `@style/Theme.GenericUI.App` Material theme XML & offscreen native `Drawable` sprite tinting, `AndroidNavbarImpl` (`MaterialToolbar`), and `AndroidResourceResolverImpl` |
| `:tests:mobile:ios` | `:mobile:ios` | `iosX64`, `iosArm64`, `iosSimulatorArm64` | Verifies `IosLauncher`, `UIViewController` / `UIView` hierarchy, offscreen UIKit control sprite tinting, and optional top navigation bar rendering |
| `:tests:web` | `:web` | `js(IR)`, `wasmJs` | Verifies `WebDomElement` (`userClassList` vs. `enumClassList` isolation and `!important` styles), `WebCssStyleTranslator`, `WebStyleSheetManager` (`<link rel="stylesheet">`, build-time `<link id="genericui-build-theme-css">`, and runtime `<style id="genericui-runtime-theme-css">`), `data-bs-theme` switching, 4-state Bootstrap 5 navbar/menubar chrome (`WebWindowImpl`), `WebFragmentHistoryBridge` (`window.location.hash` + `window.history.state`), and `WebResourceResolverImpl` |

---

## Running Platform Test Suites

```bash
# Run common + JVM Swing + Android unit tests
./gradlew :tests:common:allTests :tests:desktop:swing:test :tests:mobile:android:testDebugUnitTest

# Run Windows native tests (mingwX64)
./gradlew :tests:desktop:windows:mingwX64Test

# Run Web tests (JS & WasmJS)
./gradlew :tests:web:jsTest :tests:web:wasmJsTest
```
