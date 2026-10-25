package com.nexustools.genericui.tests

/**
 * Execution result of a single `test(...)` / `it(...)` block in a `JestElectricRunner` suite.
 */
data class JestTestResult(
    val suitePath: String,
    val testName: String,
    val passed: Boolean,
    val durationMs: Long,
    val failureMessage: String? = null,
    val error: Throwable? = null
) {
    val fullName: String
        get() = if (suitePath.isBlank()) testName else "$suitePath > $testName"
}
