# Platform Test Runners & `ServiceWorker` Test Harnesses (`:tests`)

> **Module Guide**: [**`:tests` (`../README.md`)**](../README.md) | **Related**: [`jest-electric-runner.md`](jest-electric-runner.md) · [`../../docs/testing.md`](../../docs/testing.md) · [`../../docs/backends.md`](../../docs/backends.md)

## Overview
GenericUI's **`:tests`** module wires `JestElectricRunner` across every platform target inside a single consolidated KMP Gradle project (10 Kotlin targets / 9 platform backends), alongside dedicated unit, Robolectric, loopback IPC, and hardware blur verification suites.

---

## Consolidated `:tests` SourceSet Matrix

| `:tests` Source Set | Backend Under Test | Target(s) | Execution Mode & Key Verification Classes |
|---------------------|--------------------|-----------|-------------------------------------------|
| `commonMain` / `commonTest` | `:common` | All KMP targets | `JestElectricRunner`, `JestSuiteScope`, `RoboAppController`, `RoboFragmentController`, 12 `JestElectric*RenderUtil` snapshot renderers (`JestElectricRunnerTest` including `backdropFilter` checks on `StyledContainer` / `StyledButton` vs. `TextBubble`) |
| `systemMain` | `:system` | 8 non-web OS targets | Non-web Blink CSS matcher & layout verification (`SystemStorageKeyHelper`, style restraints) |
| `desktopMain` / `nativeDesktopMain` | `:desktop` | `mingwX64`, `macosArm64`, `linuxX64`, `linuxArm64` | `DesktopSharedRunnerNativeImpl`, `DesktopBaseNativeImpl` user class isolation, `DesktopControlTintRenderUtil`, docking & viewport virtualization |
| `mingwMain` / `mingwX64Test` | `Windows` | `mingwX64` | `WindowsVisualAppRunnerNativeImpl` (headless Win32/UxTheme by default, real `HWND` when `testing.visualOnWindows = true`), `WindowsCurCodec`, and **`WindowsBackdropBlurTest`** (Direct2D blur regression tests) |
| `macosMain` / `macosArm64Test` | `MacOS` | `macosArm64` | `MacosJestElectricRunnerNativeImpl` (AppKit `NSWindow`/`NSView`, `MacosControlRenderUtil`, `MacosMetalBackdropBlurEngine`, `MacosCursorTranslator`, `!MacosNativeWindowHost.isAlreadyWrappedAppleSquircleIcon`, `MacosResourceResolverImpl`) |
| `linuxMain` / `linuxTest` | `Gtk` & `Qt` | `linuxX64`, `linuxArm64` | Single test binary co-hosting `GtkJestElectricRunnerNativeImpl` (`GtkControlRenderUtil`, `Gtk4DynamicBindings`, `GtkHeaderBar`) and `QtJestElectricRunnerNativeImpl` (`QtControlRenderUtil`, `Qt6DynamicBindings`, `QMainWindow`) + shared `LinuxResourceResolverImpl` (`LinuxGtkAndQtRunnerTest`) |
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
