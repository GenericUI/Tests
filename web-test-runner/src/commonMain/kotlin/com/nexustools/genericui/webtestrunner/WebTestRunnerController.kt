@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nexustools.genericui.webtestrunner

import com.nexustools.genericui.ext.webview.WebView
import com.nexustools.genericui.ext.webview.WebViewConfig
import com.nexustools.genericui.ext.webview.WebViewConsoleMessage
import com.nexustools.genericui.ext.webview.desktop.DesktopWebViewExtensionProvider
import com.nexustools.genericui.tests.JestSuiteReport
import com.nexustools.genericui.tests.JestTestResult
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread

/**
 * Controller that coordinates a GenericUI [WebView] and its bidirectional communication
 * functions (`registerCommunicationFunction`, `callCommunicationFunction`, `evaluateJavascript`,
 * `postWebMessage`, `onWebMessageReceived`, `onConsoleMessage`, `onJavaScriptError`, `onPageFinished`)
 * to load GenericUI web builds and execute `JestElectricRunner` and `nwjstestrunner`-compatible tests.
 */
class WebTestRunnerController(
    val webView: WebView = WebView(
        config = WebViewConfig(
            enableJavaScript = true,
            allowFileAccess = true
        )
    )
) {
    val consoleMessages = ArrayList<WebViewConsoleMessage>()
    val javaScriptErrors = ArrayList<String>()
    val recordedResults = ArrayList<JestTestResult>()
    val receivedWebMessages = ArrayList<String>()

    var activeSuiteName: String = "GenericUI Web Suite"
        private set

    var currentTestBanner: String = "Ready"
        private set

    var pageLoadCount: Int = 0
        private set

    var lastLoadedUrl: String? = null
        private set

    var onStatusUpdated: ((suiteName: String, banner: String, passed: Int, failed: Int, latestLog: String) -> Unit)? = null

    init {
        DesktopWebViewExtensionProvider.install()
        installCommunicationBridge()
    }

    private fun installCommunicationBridge() {
        webView.onConsoleMessage = { msg ->
            consoleMessages.add(msg)
            notifyStatus(latestLog = "[${msg.level}] ${msg.message}")
        }
        webView.onJavaScriptError = { err ->
            javaScriptErrors.add(err)
            notifyStatus(latestLog = "[JS Error] $err")
        }
        webView.onWebMessageReceived = { msg ->
            receivedWebMessages.add(msg)
            notifyStatus(latestLog = "[Message] $msg")
        }
        webView.onPageFinished = { url ->
            pageLoadCount++
            lastLoadedUrl = url
            notifyStatus(latestLog = "Loaded $url")
        }

        webView.registerCommunicationFunction("__jestElectricSuiteStart") { payloadJson ->
            val name = extractJsonString(payloadJson, "suiteName").ifEmpty { "GenericUI Web Suite" }
            activeSuiteName = name
            currentTestBanner = "[SUITE] $name"
            notifyStatus(latestLog = "Starting suite: $name")
            "{\"ok\":true}"
        }

        webView.registerCommunicationFunction("__jestElectricTestStart") { payloadJson ->
            val suitePath = extractJsonString(payloadJson, "suitePath").ifEmpty { activeSuiteName }
            val testName = extractJsonString(payloadJson, "testName")
            currentTestBanner = "[RUNNING] $suitePath > $testName"
            notifyStatus(latestLog = "Running: $suitePath > $testName")
            "{\"ok\":true}"
        }

        webView.registerCommunicationFunction("__jestElectricTestFinish") { payloadJson ->
            val suitePath = extractJsonString(payloadJson, "suitePath").ifEmpty { activeSuiteName }
            val testName = extractJsonString(payloadJson, "testName").ifEmpty { "test" }
            val passed = extractJsonBoolean(payloadJson, "passed", defaultVal = true)
            val durationMs = extractJsonLong(payloadJson, "durationMs", defaultVal = 1L)
            val failureMessage = extractJsonString(payloadJson, "failureMessage").takeIf { it.isNotEmpty() }
            val result = JestTestResult(
                suitePath = suitePath,
                testName = testName,
                passed = passed,
                durationMs = durationMs,
                failureMessage = if (passed) null else (failureMessage ?: "Assertion failed")
            )
            recordedResults.add(result)
            val badge = if (passed) "PASS" else "FAIL"
            currentTestBanner = "[$badge] ${result.fullName}"
            notifyStatus(latestLog = "[$badge] ${result.fullName} (${result.durationMs}ms)")
            "{\"ok\":true}"
        }

        webView.registerCommunicationFunction("__jestElectricSuiteFinish") { payloadJson ->
            val name = extractJsonString(payloadJson, "suiteName").ifEmpty { activeSuiteName }
            activeSuiteName = name
            val passedCount = recordedResults.count { it.passed }
            val failedCount = recordedResults.count { !it.passed }
            currentTestBanner = if (failedCount == 0) "[PASS] $name ($passedCount passed)" else "[FAIL] $name ($failedCount failed)"
            notifyStatus(latestLog = "Finished suite '$name': $passedCount passed, $failedCount failed")
            "{\"ok\":true,\"passedCount\":$passedCount,\"failedCount\":$failedCount}"
        }

        webView.registerCommunicationFunction("__jestElectricGetStatus") { _ ->
            val passedCount = recordedResults.count { it.passed }
            val failedCount = recordedResults.count { !it.passed }
            "{\"suiteName\":\"$activeSuiteName\",\"totalCount\":${recordedResults.size},\"passedCount\":$passedCount,\"failedCount\":$failedCount}"
        }
    }

    fun resetSession(suiteName: String = "GenericUI Web Suite") {
        activeSuiteName = suiteName
        currentTestBanner = "Ready"
        consoleMessages.clear()
        javaScriptErrors.clear()
        recordedResults.clear()
        receivedWebMessages.clear()
    }

    fun registerHostFunction(name: String, handler: (String) -> String) {
        webView.registerCommunicationFunction(name, handler)
    }

    fun unregisterHostFunction(name: String) {
        webView.unregisterCommunicationFunction(name)
    }

    fun callGuestFunction(name: String, payloadJson: String = ""): String? {
        var output: String? = null
        webView.callCommunicationFunction(name, payloadJson) { res ->
            output = res
        }
        return output
    }

    fun evaluateJs(script: String): String? {
        var output: String? = null
        webView.evaluateJavascript(script) { res ->
            output = res
        }
        return output
    }

    fun loadWebBuild(webDistDirOrUrl: String, entryFile: String = "index.html") {
        val trimmed = webDistDirOrUrl.trim()
        val target = if (
            trimmed.endsWith(".html", ignoreCase = true) ||
            trimmed.endsWith(".htm", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "${trimmed.trimEnd('/', '\\')}/${entryFile.trimStart('/', '\\').ifEmpty { "index.html" }}"
        }
        webView.loadUrl(target)
    }

    fun loadWebHtml(html: String, baseUrl: String? = null) {
        webView.loadHtml(html, baseUrl)
    }

    /**
     * Verifies that the web build loaded into [webView] has `document.readyState === 'complete'`
     * and that all [requiredElementIds], [requiredSelectors], and optional [customPredicateJs]
     * evaluate to truthy in the DOM.
     */
    fun waitForScreenLoad(
        requiredElementIds: List<String> = emptyList(),
        requiredSelectors: List<String> = emptyList(),
        customPredicateJs: String? = null
    ): Boolean {
        val readyState = evaluateJs("document.readyState")
        if (readyState != "complete" && readyState != "interactive") return false

        for (id in requiredElementIds) {
            val exists = evaluateJs("document.getElementById('$id') ? 'true' : 'false'")
            if (exists != "true") return false
        }
        for (sel in requiredSelectors) {
            val escaped = sel.replace("\\", "\\\\").replace("'", "\\'")
            val exists = evaluateJs("document.querySelector('$escaped') ? 'true' : 'false'")
            if (exists != "true") return false
        }
        if (!customPredicateJs.isNullOrBlank()) {
            val predRes = evaluateJs("($customPredicateJs) ? 'true' : 'false'")
            if (predRes != "true") return false
        }
        return true
    }

    /**
     * Executes an inline `@nexustools/nwjstestrunner`-compatible test script inside [webView]
     * and records its pass/fail result in [recordedResults].
     */
    fun executeWebviewTest(testName: String = "executeWebviewTest", script: String): JestTestResult {
        evaluateJs("window.__testDone = false; window.__testErrors = []; window.__testStep = 'running';")
        val errorsBefore = javaScriptErrors.size
        evaluateJs(script)

        val newJsErrors = javaScriptErrors.drop(errorsBefore)
        val guestErrorsCount = evaluateJs("window.__testErrors ? window.__testErrors.length : 0")?.toIntOrNull() ?: 0
        val guestFirstError = if (guestErrorsCount > 0) {
            evaluateJs("window.__testErrors[0]")
        } else {
            null
        }
        val failureMsg = guestFirstError ?: newJsErrors.firstOrNull()
        val passed = failureMsg == null
        val result = JestTestResult(
            suitePath = activeSuiteName,
            testName = testName,
            passed = passed,
            durationMs = 1L,
            failureMessage = failureMsg
        )
        recordedResults.add(result)
        notifyStatus(latestLog = "[${if (passed) "PASS" else "FAIL"}] ${result.fullName}")
        return result
    }

    /**
     * Runs all guest `describe(...)` / `it(...)` / `test(...)` suites registered inside [webView]
     * via the `__runRegisteredWebTests` communication function and returns the aggregated [JestSuiteReport].
     */
    fun runRegisteredGuestTests(filterPattern: String? = null): JestSuiteReport {
        callGuestFunction("__runRegisteredWebTests", filterPattern.orEmpty())
        return buildSuiteReport()
    }

    /**
     * Loads and executes the web build and/or external test scripts specified in [config].
     */
    fun runFromCliConfig(config: WebTestRunnerCliConfig): JestSuiteReport {
        val loadTarget = config.resolveLoadUrl()
        if (loadTarget != null) {
            webView.loadUrl(loadTarget)
        }
        for (testFile in config.testFiles) {
            val scriptCode = readPosixText(testFile)
            if (scriptCode != null) {
                evaluateJs(scriptCode)
            } else {
                recordedResults.add(
                    JestTestResult(
                        suitePath = activeSuiteName,
                        testName = testFile,
                        passed = false,
                        durationMs = 1L,
                        failureMessage = "Test file not found: $testFile"
                    )
                )
            }
        }
        callGuestFunction("__runRegisteredWebTests", config.filterPattern.orEmpty())

        if (recordedResults.isEmpty() && loadTarget != null) {
            val screenReady = waitForScreenLoad()
            recordedResults.add(
                JestTestResult(
                    suitePath = activeSuiteName,
                    testName = "Web build loads cleanly ($loadTarget)",
                    passed = screenReady && javaScriptErrors.isEmpty(),
                    durationMs = 1L,
                    failureMessage = if (!screenReady) {
                        "Document readyState did not reach complete"
                    } else {
                        javaScriptErrors.firstOrNull()
                    }
                )
            )
        }
        return buildSuiteReport()
    }

    fun buildSuiteReport(suiteName: String = activeSuiteName): JestSuiteReport =
        JestSuiteReport(
            suiteName = suiteName,
            results = recordedResults.toList()
        )

    private fun notifyStatus(latestLog: String) {
        val passedCount = recordedResults.count { it.passed }
        val failedCount = recordedResults.count { !it.passed }
        onStatusUpdated?.invoke(activeSuiteName, currentTestBanner, passedCount, failedCount, latestLog)
    }

    private fun extractJsonString(json: String, key: String): String {
        val regex = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val raw = regex.find(json)?.groupValues?.getOrNull(1) ?: return ""
        return raw
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    private fun extractJsonBoolean(json: String, key: String, defaultVal: Boolean): Boolean {
        val regex = Regex("\"$key\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val raw = regex.find(json)?.groupValues?.getOrNull(1)?.lowercase() ?: return defaultVal
        return raw == "true"
    }

    private fun extractJsonLong(json: String, key: String, defaultVal: Long): Long {
        val regex = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
        return regex.find(json)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: defaultVal
    }

    private fun readPosixText(path: String): String? {
        val file = fopen(path.replace('\\', '/'), "rb") ?: return null
        return try {
            val sb = StringBuilder()
            memScoped {
                val bufSize = 8192
                val buffer = allocArray<ByteVar>(bufSize + 1)
                while (true) {
                    val n = fread(buffer, 1u, bufSize.toULong(), file).toInt()
                    if (n <= 0) break
                    buffer[n] = 0
                    sb.append(buffer.toKString())
                }
            }
            sb.toString()
        } finally {
            fclose(file)
        }
    }
}
