package com.nexustools.genericui.tests

/**
 * **`JestElectricRunner`** — Unified Cross-Platform UI Testing Engine for GenericUI.
 *
 * Combines **Jest**-style BDD test suites (`describe`, `it`, `test`, `beforeEach`, `afterEach`,
 * `expect(x).toBe(...)`) with **Robolectric**-style application/fragment lifecycle controllers
 * (`launchApp`, `launchFragment`, `findById`, `findByText`, `performClick`, `performTypeText`, `clickMenu`).
 *
 * - **Visual Mode on Windows (`:tests:desktop:windows`)**: When executed on Windows with
 *   `visualOnWindows = true`, `JestElectricRunner` launches the actual Win32 native window
 *   on screen via `Win32NativeWindowHost`, updates a live titlebar banner with each running test,
 *   and performs UI interactions visibly against the live application window.
 * - **JUnit / Robolectric Integration (`:tests:desktop:swing`, `:tests:mobile:android`)**:
 *   Executes inside standard JUnit 4 / Robolectric runners while sharing the exact same test DSL.
 */
object JestElectricRunner {

    /**
     * Runs a top-level test suite and returns a [JestSuiteReport].
     *
     * @param suiteName Human-readable name of the test suite.
     * @param visualOnWindows Whether to open a live visual Win32 window when running on `:desktop:windows`.
     * @param throwOnFailure When `true` (default), automatically throws an [AssertionError] if any test fails.
     * @param runnerImpl The platform [JestElectricRunnerNativeImpl] to use (defaults to [JestElectricNativeRegistry.current]).
     * @param block The suite definition block.
     */
    fun runSuite(
        suiteName: String,
        visualOnWindows: Boolean = true,
        throwOnFailure: Boolean = true,
        runnerImpl: JestElectricRunnerNativeImpl = JestElectricNativeRegistry.current(),
        block: JestSuiteScope.() -> Unit
    ): JestSuiteReport {
        val rootScope = JestSuiteScope(
            suitePath = suiteName,
            runnerImpl = runnerImpl,
            visualOnWindows = visualOnWindows
        )
        rootScope.block()
        val results = mutableListOf<JestTestResult>()
        try {
            rootScope.executeInto(results)
        } finally {
            runnerImpl.closeVisualWindow()
        }
        val report = JestSuiteReport(suiteName = suiteName, results = results)
        println(report.formatSummary())
        if (throwOnFailure) {
            report.assertAllPassed()
        }
        return report
    }

    /**
     * Top-level convenience entry point matching Jest's `describe(suiteName) { ... }`.
     */
    fun describe(
        suiteName: String,
        visualOnWindows: Boolean = true,
        throwOnFailure: Boolean = true,
        runnerImpl: JestElectricRunnerNativeImpl = JestElectricNativeRegistry.current(),
        block: JestSuiteScope.() -> Unit
    ): JestSuiteReport = runSuite(
        suiteName = suiteName,
        visualOnWindows = visualOnWindows,
        throwOnFailure = throwOnFailure,
        runnerImpl = runnerImpl,
        block = block
    )

    /**
     * Entry-point helper for standalone visual test executables (such as `windowsVisualTest` on `:desktop:windows`).
     *
     * Supports `--headless` or `--no-visual` CLI flags to override [visualOnWindows] when needed.
     */
    fun runVisualAppTestMain(
        suiteName: String,
        args: Array<String> = emptyArray(),
        block: JestSuiteScope.() -> Unit
    ): JestSuiteReport {
        val visual = args.none { it == "--headless" || it == "--no-visual" }
        return runSuite(
            suiteName = suiteName,
            visualOnWindows = visual,
            throwOnFailure = true,
            block = block
        )
    }
}
