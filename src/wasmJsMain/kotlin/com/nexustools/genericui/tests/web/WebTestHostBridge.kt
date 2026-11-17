@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.nexustools.genericui.tests.web

import com.nexustools.genericui.tests.JestSuiteReport
import com.nexustools.genericui.tests.JestTestResult

@JsFun("() => typeof window !== 'undefined' && typeof window.genericUi === 'object' && window.genericUi !== null && typeof window.genericUi.call === 'function'")
private external fun wasmHasGenericUiHostBridge(): Boolean

@JsFun("(channel, payloadJson) => { try { if (typeof window !== 'undefined' && window.genericUi && typeof window.genericUi.call === 'function') { window.genericUi.call(channel, payloadJson); } } catch (_e) {} }")
private external fun wasmInvokeGenericUiHostBridge(channel: String, payloadJson: String)

internal actual object WebTestHostBridge {

    actual val isHostBridgeAvailable: Boolean
        get() = wasmHasGenericUiHostBridge()

    actual fun notifySuiteStart(suiteName: String) {
        if (!isHostBridgeAvailable) return
        val payload = "{\"suiteName\":${quoteJson(suiteName)}}"
        wasmInvokeGenericUiHostBridge("__jestElectricSuiteStart", payload)
    }

    actual fun notifyTestStart(suitePath: String, testName: String) {
        if (!isHostBridgeAvailable) return
        val payload = "{\"suitePath\":${quoteJson(suitePath)},\"testName\":${quoteJson(testName)}}"
        wasmInvokeGenericUiHostBridge("__jestElectricTestStart", payload)
    }

    actual fun notifyTestFinish(result: JestTestResult) {
        if (!isHostBridgeAvailable) return
        val payload = buildString {
            append('{')
            append("\"suitePath\":").append(quoteJson(result.suitePath)).append(',')
            append("\"testName\":").append(quoteJson(result.testName)).append(',')
            append("\"passed\":").append(result.passed).append(',')
            append("\"durationMs\":").append(result.durationMs).append(',')
            append("\"failureMessage\":").append(quoteJson(result.failureMessage ?: ""))
            append('}')
        }
        wasmInvokeGenericUiHostBridge("__jestElectricTestFinish", payload)
    }

    actual fun notifySuiteFinish(report: JestSuiteReport) {
        if (!isHostBridgeAvailable) return
        val payload = buildString {
            append('{')
            append("\"suiteName\":").append(quoteJson(report.suiteName)).append(',')
            append("\"totalCount\":").append(report.totalCount).append(',')
            append("\"passedCount\":").append(report.passedCount).append(',')
            append("\"failedCount\":").append(report.failedCount)
            append('}')
        }
        wasmInvokeGenericUiHostBridge("__jestElectricSuiteFinish", payload)
    }

    private fun quoteJson(value: String): String {
        val sb = StringBuilder("\"")
        for (ch in value) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
