package com.nexustools.genericui.tests

import kotlin.time.TimeSource

/**
 * Receiver scope for `describe(...)` suite blocks in `JestElectricRunner`.
 *
 * Supports nested `describe(...)` blocks, lifecycle hooks (`beforeAll`, `afterAll`,
 * `beforeEach`, `afterEach`), and test cases (`test(...)` / `it(...)`).
 */
class JestSuiteScope(
    val suitePath: String,
    private val runnerImpl: JestElectricRunnerNativeImpl,
    private val visualOnWindows: Boolean = true,
    private val inheritedBeforeEach: List<JestTestScope.() -> Unit> = emptyList(),
    private val inheritedAfterEach: List<JestTestScope.() -> Unit> = emptyList()
) {
    private val beforeAllHooks = mutableListOf<() -> Unit>()
    private val afterAllHooks = mutableListOf<() -> Unit>()
    private val beforeEachHooks = mutableListOf<JestTestScope.() -> Unit>()
    private val afterEachHooks = mutableListOf<JestTestScope.() -> Unit>()
    private val testEntries = mutableListOf<Pair<String, JestTestScope.() -> Unit>>()
    private val childSuites = mutableListOf<JestSuiteScope>()

    fun beforeAll(block: () -> Unit) {
        beforeAllHooks.add(block)
    }

    fun afterAll(block: () -> Unit) {
        afterAllHooks.add(block)
    }

    fun beforeEach(block: JestTestScope.() -> Unit) {
        beforeEachHooks.add(block)
    }

    fun afterEach(block: JestTestScope.() -> Unit) {
        afterEachHooks.add(block)
    }

    fun test(name: String, block: JestTestScope.() -> Unit) {
        testEntries.add(name to block)
    }

    fun it(name: String, block: JestTestScope.() -> Unit) {
        testEntries.add(name to block)
    }

    fun describe(name: String, block: JestSuiteScope.() -> Unit) {
        val nestedPath = if (suitePath.isBlank()) name else "$suitePath > $name"
        val nestedScope = JestSuiteScope(
            suitePath = nestedPath,
            runnerImpl = runnerImpl,
            visualOnWindows = visualOnWindows,
            inheritedBeforeEach = inheritedBeforeEach + beforeEachHooks,
            inheritedAfterEach = afterEachHooks + inheritedAfterEach
        )
        nestedScope.block()
        childSuites.add(nestedScope)
    }

    internal fun executeInto(results: MutableList<JestTestResult>) {
        runnerImpl.ensureBackendInitialized()
        for (hook in beforeAllHooks) {
            hook()
        }

        val combinedBeforeEach = inheritedBeforeEach + beforeEachHooks
        val combinedAfterEach = afterEachHooks + inheritedAfterEach

        try {
            for ((testName, testBody) in testEntries) {
                val mark = TimeSource.Monotonic.markNow()
                val scope = JestTestScope(runnerImpl = runnerImpl, visualOnWindows = visualOnWindows)
                val fullLabel = if (suitePath.isBlank()) testName else "$suitePath > $testName"
                runnerImpl.updateVisualTestBanner(fullLabel, "RUNNING")

                var failure: Throwable? = null
                try {
                    for (beforeHook in combinedBeforeEach) {
                        scope.beforeHook()
                    }
                    scope.testBody()
                } catch (t: Throwable) {
                    failure = t
                } finally {
                    for (afterHook in combinedAfterEach) {
                        try {
                            scope.afterHook()
                        } catch (afterErr: Throwable) {
                            if (failure == null) failure = afterErr
                        }
                    }
                    scope.cleanupTestResources()
                }

                val elapsedMs = mark.elapsedNow().inWholeMilliseconds
                if (failure == null) {
                    runnerImpl.updateVisualTestBanner(fullLabel, "PASS")
                    results.add(
                        JestTestResult(
                            suitePath = suitePath,
                            testName = testName,
                            passed = true,
                            durationMs = elapsedMs
                        )
                    )
                } else {
                    runnerImpl.updateVisualTestBanner(fullLabel, "FAIL")
                    results.add(
                        JestTestResult(
                            suitePath = suitePath,
                            testName = testName,
                            passed = false,
                            durationMs = elapsedMs,
                            failureMessage = failure.message ?: failure::class.simpleName,
                            error = failure
                        )
                    )
                }
            }

            for (child in childSuites) {
                child.executeInto(results)
            }
        } finally {
            for (hook in afterAllHooks) {
                runCatching { hook() }
            }
        }
    }
}
