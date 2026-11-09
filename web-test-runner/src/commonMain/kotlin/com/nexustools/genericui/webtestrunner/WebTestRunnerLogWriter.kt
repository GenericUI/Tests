@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nexustools.genericui.webtestrunner

import com.nexustools.genericui.ext.webview.WebViewConsoleMessage
import com.nexustools.genericui.tests.JestSuiteReport
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

/**
 * Crash-safe log and JUnit XML report writer for the GenericUI Native Desktop Web Test Runner.
 *
 * Produces both:
 * 1. `desktop-tests.log` compatible with `@nexustools/nwjstestrunner` (`Status: Tests interupted`
 *    on startup, `[PASS]`/`[FAIL]` per test, and `SUCCESS: All tests completed cleanly.` or
 *    `FAILURES & ERRORS:` on completion).
 * 2. Standard Gradle JUnit XML (`TEST-genericui-web.xml`).
 */
class WebTestRunnerLogWriter {

    var lastWrittenLogText: String = ""
        private set

    var lastWrittenJUnitXml: String = ""
        private set

    fun writeInitialInterruptedMarker(logFilePath: String?): String {
        val text = buildString {
            appendLine("Status: Tests interupted")
            appendLine("=== GenericUI Web Test Runner Log ===")
        }
        lastWrittenLogText = text
        if (!logFilePath.isNullOrBlank()) {
            writeTextFilePosix(logFilePath, text)
        }
        return text
    }

    fun formatDesktopTestsLog(
        report: JestSuiteReport,
        consoleMessages: List<WebViewConsoleMessage> = emptyList(),
        jsErrors: List<String> = emptyList()
    ): String = buildString {
        appendLine("=== GenericUI Web Test Runner Log ===")
        appendLine("Suite: ${report.suiteName}")
        for (res in report.results) {
            if (res.passed) {
                appendLine("[PASS] ${res.fullName} (${res.durationMs}ms)")
            } else {
                appendLine("[FAIL] ${res.fullName} (${res.durationMs}ms) - ${res.failureMessage ?: "Assertion failed"}")
            }
        }
        if (consoleMessages.isNotEmpty()) {
            appendLine("--- WebView Console Output ---")
            for (msg in consoleMessages) {
                appendLine("[${msg.level.uppercase()}] ${msg.message}")
            }
        }
        appendLine("Tests: ${report.passedCount} passed, ${report.failedCount} failed, ${report.totalCount} total")
        if (report.allPassed && jsErrors.isEmpty()) {
            appendLine("SUCCESS: All tests completed cleanly.")
        } else {
            appendLine("FAILURES & ERRORS:")
            for (failed in report.results.filter { !it.passed }) {
                appendLine(" - ${failed.fullName}: ${failed.failureMessage ?: "Test failed"}")
            }
            for (err in jsErrors) {
                appendLine(" - [JS Error] $err")
            }
        }
    }

    fun formatJUnitXml(report: JestSuiteReport): String = buildString {
        val totalSec = report.results.sumOf { it.durationMs }.toDouble() / 1000.0
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine(
            """<testsuite name="${escapeXml(report.suiteName)}" tests="${report.totalCount}" failures="${report.failedCount}" errors="0" skipped="0" time="$totalSec">"""
        )
        for (res in report.results) {
            val sec = res.durationMs.toDouble() / 1000.0
            val className = escapeXml(res.suitePath.ifBlank { report.suiteName })
            val testName = escapeXml(res.testName)
            if (res.passed) {
                appendLine("""  <testcase classname="$className" name="$testName" time="$sec"/>""")
            } else {
                val msg = escapeXml(res.failureMessage ?: "Test failed")
                appendLine("""  <testcase classname="$className" name="$testName" time="$sec">""")
                appendLine("""    <failure message="$msg">$msg</failure>""")
                appendLine("""  </testcase>""")
            }
        }
        appendLine("""</testsuite>""")
    }

    fun writeFinalReports(
        report: JestSuiteReport,
        logFilePath: String? = null,
        reportDirPath: String? = null,
        consoleMessages: List<WebViewConsoleMessage> = emptyList(),
        jsErrors: List<String> = emptyList()
    ): String {
        val logText = formatDesktopTestsLog(report, consoleMessages, jsErrors)
        val xmlText = formatJUnitXml(report)
        lastWrittenLogText = logText
        lastWrittenJUnitXml = xmlText

        if (!logFilePath.isNullOrBlank()) {
            writeTextFilePosix(logFilePath, logText)
        }
        if (!reportDirPath.isNullOrBlank()) {
            val cleanDir = reportDirPath.trimEnd('/', '\\')
            if (logFilePath.isNullOrBlank()) {
                writeTextFilePosix("$cleanDir/desktop-tests.log", logText)
            }
            writeTextFilePosix("$cleanDir/TEST-genericui-web.xml", xmlText)
        }
        return logText
    }

    private fun escapeXml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun writeTextFilePosix(path: String, content: String): Boolean {
        val file = fopen(path.replace('\\', '/'), "wb") ?: return false
        return try {
            val bytes = content.encodeToByteArray()
            if (bytes.isNotEmpty()) {
                bytes.usePinned { pinned ->
                    fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file)
                }
            }
            true
        } finally {
            fclose(file)
        }
    }
}
