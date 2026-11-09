package com.nexustools.genericui.tests.web

import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.tests.JestSuiteReport
import com.nexustools.genericui.tests.JestTestResult
import com.nexustools.genericui.web.WebNativeFactory

/**
 * Web HTML5 DOM runner implementation (`:tests:web`) of `JestElectricRunnerNativeImpl`.
 *
 * Automatically forwards suite and test execution lifecycle events to the host
 * GenericUI `WebView` communication functions (`window.genericUi.call(...)`) when
 * running inside the GenericUI Web Test Runner.
 */
class WebJestElectricRunnerNativeImpl : DefaultJestElectricRunnerNativeImpl() {

    private var activeSuiteName: String = ""
    private val activeSuiteResults = ArrayList<JestTestResult>()

    override val backendName: String
        get() = "web-dom"

    override fun ensureBackendInitialized() {
        if (GenericUI.activeFactory !is WebNativeFactory) {
            GenericUI.initialize(WebNativeFactory())
        }
    }

    override fun launchVisualWindowForApp(app: Application, suiteName: String) {
        if (suiteName.isNotBlank() && suiteName != activeSuiteName) {
            onSuiteStart(suiteName)
        }
        super.launchVisualWindowForApp(app, suiteName)
    }

    override fun updateVisualTestBanner(testName: String, status: String) {
        super.updateVisualTestBanner(testName, status)
        val sep = testName.lastIndexOf(" > ")
        val suitePath = if (sep >= 0) testName.substring(0, sep) else activeSuiteName.ifEmpty { "WebSuite" }
        val leafName = if (sep >= 0) testName.substring(sep + 3) else testName
        if (activeSuiteName.isEmpty() && suitePath.isNotEmpty()) {
            onSuiteStart(suitePath)
        }
        when (status.uppercase()) {
            "RUNNING" -> onTestStart(suitePath, leafName)
            "PASS" -> onTestFinish(
                JestTestResult(
                    suitePath = suitePath,
                    testName = leafName,
                    passed = true,
                    durationMs = 1L
                )
            )
            "FAIL" -> onTestFinish(
                JestTestResult(
                    suitePath = suitePath,
                    testName = leafName,
                    passed = false,
                    durationMs = 1L,
                    failureMessage = "Test '$testName' failed"
                )
            )
        }
    }

    override fun closeVisualWindow() {
        if (activeSuiteName.isNotEmpty() || activeSuiteResults.isNotEmpty()) {
            val report = JestSuiteReport(
                suiteName = activeSuiteName.ifEmpty { "WebSuite" },
                results = activeSuiteResults.toList()
            )
            onSuiteFinish(report)
            activeSuiteName = ""
            activeSuiteResults.clear()
        }
        super.closeVisualWindow()
    }

    fun onSuiteStart(suiteName: String) {
        activeSuiteName = suiteName
        activeSuiteResults.clear()
        WebTestHostBridge.notifySuiteStart(suiteName)
    }

    fun onTestStart(suitePath: String, testName: String) {
        WebTestHostBridge.notifyTestStart(suitePath, testName)
    }

    fun onTestFinish(result: JestTestResult) {
        activeSuiteResults.add(result)
        WebTestHostBridge.notifyTestFinish(result)
    }

    fun onSuiteFinish(report: JestSuiteReport) {
        WebTestHostBridge.notifySuiteFinish(report)
    }
}
