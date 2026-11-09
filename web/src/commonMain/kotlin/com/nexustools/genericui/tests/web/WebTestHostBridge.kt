package com.nexustools.genericui.tests.web

import com.nexustools.genericui.tests.JestSuiteReport
import com.nexustools.genericui.tests.JestTestResult

/**
 * Multiplatform bridge from `:tests:web` (`jsMain` & `wasmJsMain`) to the host
 * GenericUI `WebView` communication functions (`window.genericUi.call(...)`).
 */
internal expect object WebTestHostBridge {
    val isHostBridgeAvailable: Boolean
    fun notifySuiteStart(suiteName: String)
    fun notifyTestStart(suitePath: String, testName: String)
    fun notifyTestFinish(result: JestTestResult)
    fun notifySuiteFinish(report: JestSuiteReport)
}
