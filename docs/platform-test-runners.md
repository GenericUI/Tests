# Platform Test Runners & `ServiceWorker` Test Harnesses (`:tests`)

> **Module Guide**: [**`:tests` (`../README.md`)**](../README.md) | **Related**: [`jest-electric-runner.md`](jest-electric-runner.md) · [`../../docs/testing.md`](../../docs/testing.md) · [`../../docs/backends.md`](../../docs/backends.md)

## Overview
GenericUI's **`:tests`** module wires `JestElectricRunner` across every platform target inside a single consolidated KMP Gradle project (10 Kotlin targets / 9 platform backends), alongside dedicated unit, Robolectric, loopback IPC, and hardware blur verification suites.

---

## Consolidated `:tests` SourceSet Matrix

| `:tests` Source Set | Backend Under Test | Target(s) | Execution Mode & Key Verification Classes |
|---------------------|--------------------|-----------|-------------------------------------------|
| `commonMain` / `commonTest` | `:common` | All KMP targets | `JestElectricRunner`, `JestSuiteScope`, `RoboAppController`, `RoboFragmentController`, and `:tests` `commonMain` 12 `JestElectric*RenderUtil` headless software snapshot helpers (`JestElectricRunnerTest` verifying 10-op `backdropFilter` across `StyledComponent` / `StyledContainer` / `StyledButton`) |
| `systemMain` | `:system` | 8 non-web OS targets | Non-web Blink CSS matcher & layout verification (`StyledWidgetVisualController`, `SemanticSurfaceStyleResolver`, `SystemStorageKeyHelper`, style restraints) |
| `desktopMain` / `nativeDesktopMain` | `:desktop` | `mingwX64`, `macosArm64`, `linuxX64`, `linuxArm64` | `DesktopSharedRunnerNativeImpl`, `DesktopBaseNativeImpl` user class isolation, `DesktopContainerLayoutController`, `DesktopStyledWidgetDelegate`, `DesktopButtonNativeImpl`, docking & viewport virtualization |
| `mingwMain` / `mingwX64Test` | `Windows` | `mingwX64` | `WindowsVisualAppRunnerNativeImpl` (`PrintWindow(..., PW_RENDERFULLCONTENT)` real `HWND` surface capture when `testing.visualOnWindows = true`, headless `ComCtl32 v6` / `Win32ControlMessageRouter` / `Win32LayeredStageHostHwnd` checks), `WindowsCurCodec`, and **`WindowsBackdropBlurTest`** (Direct2D blur regression tests) |
| `macosMain` / `macosArm64Test` | `MacOS` | `macosArm64` | `MacosJestElectricRunnerNativeImpl` (retained AppKit `NSWindow`/`NSView` capture via `cacheDisplayInRect:toBitmapImageRep:`, `MacosWindowRootView`, `MacosBootstrapButtonControl`, `:apple` `AppleStyledSurfaceHelper`, `MacosSurfaceLayerSupport`, `MacosCursorTranslator`, `!MacosNativeWindowHost.isAlreadyWrappedAppleSquircleIcon`, `MacosResourceResolverImpl`) |
| `linuxMain` / `linuxTest` | `Gtk` & `Kde` | `linuxX64`, `linuxArm64` | Co-hosts **`GtkJestElectricRunnerNativeImpl`** (`GtkJestElectricBootstrap`, real GTK4 `GtkWidget*` capture via `GtkWidgetPaintable` + `GskRenderer`, `GtkCustomLayout`, `GskTransform`, `GtkHeaderBar`) and **`KdeJestElectricRunnerNativeImpl`** (`KdeJestElectricBootstrap`, real KDE `KF6` + `Breeze` alien `QWidget*` capture via `QWidget::render`, `KdeDynamicBindings`, `KdeVTableBridge`, optional `QT_QPA_PLATFORM=offscreen`) + shared `LinuxResourceResolverImpl` & `linux/style/` (`LinuxGtkAndKdeRunnerTest`) |
| `jvmMain` / `jvmTest` | `Swing` | `jvm` | `SwingJUnitRunnerNativeImpl` & `JestElectricSwingTestBase` (`EventQueue` EDT, `SwingHardwareBackdropBlurRenderer` headless/VolatileImage fallback, `JniLibraryLoader`) |
| `mobileMain` | `:mobile` | `android`, `iosArm64`, `iosSimulatorArm64` | `MobileSharedRunnerNativeImpl` adaptive posture, gesture, and mobile chrome verification |
| `androidMain` / `androidHostTest` | `Android` | `android` | `AndroidRobolectricRunnerNativeImpl` & `JestElectricRobolectricTestBase` (Robolectric SDK 35: `GenericUiFragmentActivity`, `AndroidActivityRegistry`, `AndroidHardwareBackdropBlurController`, `AndroidResourceResolverImpl`) |
| `iosMain` / `iosTest` | `IOS` & `IPadOS` | `iosArm64`, `iosSimulatorArm64` | Co-hosts `IosJestElectricRunnerNativeImpl` (`BackendTarget.IOS`) and `IpadosJestElectricRunnerNativeImpl` (`BackendTarget.IPadOS` multi-window `UIWindowScene`, `UIMenuSystem`, trackpad/keyboard) (`IosAndIpadosJestElectricRunnerTest`) |
| `webMain` / `jsMain` / `wasmJsMain` | `Web` | `js(IR)`, `wasmJs` | `WebJestElectricRunnerNativeImpl` & `WebTestHostBridge` (`WebDomElement` class isolation, `WebCssStyleTranslator`, `WebBackdropFilterUtil`, `WebStyleSheetManager`, `WebFragmentHistoryBridge`) |

---

## `ServiceWorker` Test Harnesses Across Platforms

- **Reset & Isolation (`ServiceWorkerExtension.resetForTests()`)**: Resets registered worker instance, `ServiceWorkerOptions`, channels, and notification state cleanly between tests.
- **Desktop Loopback & OS Locks (`extensions/system/serviceworker` `ServiceWorkerRuntimeAndIpcTest`)**: Drives `DesktopServiceWorkerHost` over ephemeral `127.0.0.1:0` loopback sockets with 32-byte hex auth tokens and `DesktopServiceWorkerChannelClient` IPC, plus `DesktopOsHelpers.acquireSingleInstanceLock` (`CreateMutexW` / `fcntl` / `flock` / `FileChannel.tryLock`) and `WindowsShortcutWriter` (`.lnk` generation) without needing persistent desktop tray daemons.
- **Android Robolectric Notification & Action Tests (`AndroidServiceWorkerRobolectricTest`)**: Exercises `GeneratedAndroidServiceWorkerService` and `AndroidServiceWorkerController` under Robolectric SDK 35, verifying `NotificationChannel` creation, `ServiceCompat.startForeground` ongoing notifications, `setNotificationTitle`/`setNotificationText`/`setNotificationSubText`, default `"Stop"` action (`DEFAULT_STOP_ACTION_ID`), `extendNotificationActions` / `replaceNotificationActions` `Intent` dispatch, and `AndroidServiceWorkerBinder` IPC.
- **iOS & iPadOS Unsupported Verification (`AppleMobileServiceWorkerTest`)**: Asserts `ServiceWorkerExtension.isSupported == false`, `isAutostartSupported == false`, and zero `@MainServiceWorker` instantiation.
- **Web Headless Node Bridges (`WebServiceWorkerBridgeTest` & `ServiceWorkerCrossPlatformTest`)**: Verifies `WebServiceWorkerClientBridge` (`isServiceWorkerSupported == false` in Node) and `WebWorkerGlobalBridge` (`isWorkerGlobalScope == false`) degrade safely without browser globals.

---

## Running Platform Test Suites

```bash
./gradlew :tests:jvmTest :tests:wasmJsTest :tests:jsTest
./gradlew :tests:mingwX64Test :tests:linuxX64Test :tests:macosArm64Test :tests:iosSimulatorArm64Test
./gradlew :tests:testAndroidHostTest :extensions:system:serviceworker:jvmTest :extensions:system:serviceworker:testAndroidHostTest
```
