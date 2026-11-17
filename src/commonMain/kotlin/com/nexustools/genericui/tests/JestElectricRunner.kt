package com.nexustools.genericui.tests

import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget

/**
 * **`JestElectricRunner`** — Unified Cross-Platform UI Testing Engine for GenericUI.
 *
 * Combines **Jest**-style BDD test suites (`describe`, `it`, `test`, `beforeEach`, `afterEach`,
 * `expect(x).toBe(...)`) with **Robolectric**-style application/fragment lifecycle controllers
 * (`launchApp`, `launchFragment`, `findById`, `findByText`, `performClick`, `performTypeText`, `clickMenu`).
 *
 * - **Visual Mode on Windows**: When executed on Windows with `visualOnWindows = true`,
 *   `JestElectricRunner` launches the actual Win32 native window on screen via
 *   `Win32NativeWindowHost`, updates a live titlebar banner with each running test,
 *   and performs UI interactions visibly against the live application window.
 * - **Multi-Target Apple / Linux Support**: Supports running suites explicitly against
 *   specific [BackendTarget]s (`runSuiteForTarget` / `runSuiteAcrossTargets`), including
 *   both `BackendTarget.IOS` and `BackendTarget.IPadOS` on Apple UIKit and both
 *   `BackendTarget.Gtk` and `BackendTarget.Qt` on Linux.
 * - **JUnit / Robolectric Integration**: Executes inside standard JVM Swing and Android JUnit 4
 *   runners while sharing the exact same test DSL.
 */
object JestElectricRunner {

    /**
     * Runs a top-level test suite and returns a [JestSuiteReport].
     *
     * @param suiteName Human-readable name of the test suite.
     * @param visualOnWindows Whether to open a live visual Win32 window when running on Windows.
     * @param throwOnFailure When `true` (default), automatically throws an [AssertionError] if any test fails.
     * @param runnerImpl The platform [JestElectricRunnerNativeImpl] to use (defaults to [JestElectricNativeRegistry.current]).
     * @param block The suite definition block.
     */
    fun runSuite(
        suiteName: String,
        visualOnWindows: Boolean = true,
        throwOnFailure: Boolean = true,
        perTestTimeoutMs: Long = 15_000L,
        runnerImpl: JestElectricRunnerNativeImpl = JestElectricNativeRegistry.current(),
        block: JestSuiteScope.() -> Unit
    ): JestSuiteReport {
        val rootScope = JestSuiteScope(
            suitePath = suiteName,
            runnerImpl = runnerImpl,
            visualOnWindows = visualOnWindows,
            perTestTimeoutMs = perTestTimeoutMs
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
     * Runs a top-level test suite against the registered runner for [target] (for example,
     * [BackendTarget.IOS] or [BackendTarget.IPadOS]), temporarily activating that target's
     * runner and `NativeFactory` and restoring the previous state when finished.
     *
     * @throws IllegalStateException if no runner is registered for [target] in [JestElectricNativeRegistry].
     */
    fun runSuiteForTarget(
        suiteName: String,
        target: BackendTarget,
        visualOnWindows: Boolean = true,
        throwOnFailure: Boolean = true,
        perTestTimeoutMs: Long = 15_000L,
        block: JestSuiteScope.() -> Unit
    ): JestSuiteReport {
        val targetRunner = JestElectricNativeRegistry.forTarget(target)
            ?: throw IllegalStateException(
                "No JestElectricRunnerNativeImpl registered for BackendTarget.${target.name} (${target.id})."
            )
        val previousRunner = JestElectricNativeRegistry.current()
        val previousFactory = GenericUI.activeFactory
        JestElectricNativeRegistry.register(targetRunner)
        targetRunner.ensureBackendInitialized()
        return try {
            runSuite(
                suiteName = "$suiteName [${target.displayName}]",
                visualOnWindows = visualOnWindows,
                throwOnFailure = throwOnFailure,
                perTestTimeoutMs = perTestTimeoutMs,
                runnerImpl = targetRunner,
                block = block
            )
        } finally {
            JestElectricNativeRegistry.register(previousRunner)
            if (previousFactory != null) {
                GenericUI.initialize(previousFactory)
            } else {
                GenericUI.activeFactory = null
            }
        }
    }

    /**
     * Executes the same suite sequentially across each target in [targets] (for example,
     * `listOf(BackendTarget.IOS, BackendTarget.IPadOS)` or `listOf(BackendTarget.Gtk, BackendTarget.Qt)`)
     * and returns the per-target [JestSuiteReport] map.
     */
    fun runSuiteAcrossTargets(
        suiteName: String,
        targets: Iterable<BackendTarget>,
        visualOnWindows: Boolean = true,
        throwOnFailure: Boolean = true,
        perTestTimeoutMs: Long = 15_000L,
        block: JestSuiteScope.() -> Unit
    ): Map<BackendTarget, JestSuiteReport> {
        val targetList = targets.toList()
        require(targetList.isNotEmpty()) { "Expected at least one BackendTarget to run suite '$suiteName'." }
        val reports = LinkedHashMap<BackendTarget, JestSuiteReport>(targetList.size)
        for (target in targetList) {
            reports[target] = runSuiteForTarget(
                suiteName = suiteName,
                target = target,
                visualOnWindows = visualOnWindows,
                throwOnFailure = throwOnFailure,
                perTestTimeoutMs = perTestTimeoutMs,
                block = block
            )
        }
        return reports
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
     * Entry-point helper for standalone visual test executables (such as `windowsVisualTest` on Windows).
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
