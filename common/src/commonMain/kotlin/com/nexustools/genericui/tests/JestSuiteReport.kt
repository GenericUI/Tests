package com.nexustools.genericui.tests

/**
 * Aggregated report produced by running a `JestElectricRunner` suite.
 */
data class JestSuiteReport(
    val suiteName: String,
    val results: List<JestTestResult>,
    val totalCount: Int = results.size,
    val passedCount: Int = results.count { it.passed },
    val failedCount: Int = results.count { !it.passed },
    val allPassed: Boolean = results.all { it.passed }
) {
    /**
     * Formats a Jest-style human-readable summary of all test results in the suite.
     */
    fun formatSummary(): String = buildString {
        appendLine("=== JestElectricRunner Suite: $suiteName ===")
        for (res in results) {
            val badge = if (res.passed) "PASS" else "FAIL"
            appendLine("  [$badge] ${res.fullName} (${res.durationMs}ms)")
            if (!res.passed && res.failureMessage != null) {
                appendLine("         Reason: ${res.failureMessage}")
            }
        }
        appendLine("Tests: $passedCount passed, $failedCount failed, $totalCount total")
    }

    /**
     * Throws an [AssertionError] with the full suite summary if any test in the report failed.
     */
    fun assertAllPassed() {
        if (!allPassed) {
            val failedSummary = results.filter { !it.passed }
                .joinToString("\n") { " - ${it.fullName}: ${it.failureMessage ?: it.error?.message}" }
            throw AssertionError(
                "JestElectricRunner suite '$suiteName' had $failedCount failing test(s) out of $totalCount:\n$failedSummary"
            )
        }
    }
}
