package com.nexustools.genericui.webtestrunner

import com.nexustools.genericui.enums.bootstrap.BootstrapType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebTestRunnerTest {

    @Test
    fun testWebViewBidirectionalCommunicationFunctionsAndEvents() {
        val controller = WebTestRunnerController()
        val webView = controller.webView

        var pingPayloadReceived = ""
        controller.registerHostFunction("hostPing") { payload ->
            pingPayloadReceived = payload
            "{\"reply\":\"pong\",\"echo\":${payload.ifEmpty { "\"\"" }}}"
        }
        assertTrue(webView.hasCommunicationFunction("hostPing"))
        assertTrue(webView.getCommunicationFunctionNames().contains("hostPing"))

        controller.loadWebHtml(
            """
            <!DOCTYPE html>
            <html>
            <head><title>Bridge Test</title></head>
            <body>
                <div id="bridge-output">idle</div>
                <script>
                    console.log("Bridge test guest initialized");
                    window.genericUi.registerCommunicationFunction("guestMultiply", function(payload) {
                        var num = parseInt(payload, 10) || 0;
                        return String(num * 3);
                    });
                    var res = window.genericUi.callCommunicationFunction("hostPing", "\"hello-host\"");
                    document.getElementById("bridge-output").textContent = res;
                    window.genericUi.postMessage("guest-ready-signal");
                </script>
            </body>
            </html>
            """.trimIndent(),
            baseUrl = "https://genericui.local/index.html"
        )

        assertEquals("\"hello-host\"", pingPayloadReceived)
        assertEquals(
            "{\"reply\":\"pong\",\"echo\":\"hello-host\"}",
            controller.evaluateJs("document.getElementById('bridge-output').textContent")
        )
        assertTrue(controller.receivedWebMessages.contains("guest-ready-signal"))
        assertTrue(controller.consoleMessages.any { it.message.contains("Bridge test guest initialized") })

        val multiplied = controller.callGuestFunction("guestMultiply", "14")
        assertEquals("42", multiplied)

        controller.unregisterHostFunction("hostPing")
        assertFalse(webView.hasCommunicationFunction("hostPing"))
    }

    @Test
    fun testFullGenericUiWebBuildHtmlLoadingAndDynamicScriptExecution() {
        val controller = WebTestRunnerController()

        controller.loadWebHtml(
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>GenericUI Web App</title>
                <script type="loader-js" src="app.js"></script>
                <link rel="resources" href="resources.json">
            </head>
            <body>
                <genericui:app></genericui:app>
                <script>
                    var loaderTag = document.querySelector('script[type="loader-js"]');
                    var appHost = document.querySelector('genericui:app');
                    if (loaderTag && appHost) {
                        var rootBox = document.createElement('div');
                        rootBox.id = 'genericui-mounted-root';
                        rootBox.className = 'container-fluid';
                        rootBox.textContent = 'Mounted from ' + loaderTag.getAttribute('src');
                        appHost.appendChild(rootBox);
                    }
                </script>
            </body>
            </html>
            """.trimIndent(),
            baseUrl = "file:///app/index.html"
        )

        assertTrue(
            controller.waitForScreenLoad(
                requiredElementIds = listOf("genericui-mounted-root"),
                requiredSelectors = listOf("script[type=\"loader-js\"]", "genericui:app"),
                customPredicateJs = "document.getElementById('genericui-mounted-root').textContent.indexOf('app.js') >= 0"
            )
        )
    }

    @Test
    fun testGuestDescribeItExpectAndExecuteWebviewTestReporting() {
        val controller = WebTestRunnerController()

        controller.loadWebHtml(
            """
            <!DOCTYPE html>
            <html>
            <body>
                <button id="counter-btn">Clicks: 0</button>
                <script>
                    var count = 0;
                    var btn = document.getElementById('counter-btn');
                    btn.addEventListener('click', function() {
                        count++;
                        btn.textContent = 'Clicks: ' + count;
                    });

                    describe('CounterButtonSuite', function() {
                        it('starts at zero', function() {
                            expect(document.getElementById('counter-btn').textContent).toBe('Clicks: 0');
                        });

                        it('increments when clicked', function() {
                            document.getElementById('counter-btn').click();
                            expect(document.getElementById('counter-btn').textContent).toBe('Clicks: 1');
                        });
                    });
                </script>
            </body>
            </html>
            """.trimIndent()
        )

        val suiteReport = controller.runRegisteredGuestTests()
        assertEquals(2, suiteReport.totalCount)
        assertEquals(2, suiteReport.passedCount)
        assertEquals(0, suiteReport.failedCount)
        assertTrue(suiteReport.allPassed)

        val inlineResult = controller.executeWebviewTest(
            testName = "Inline click verification",
            script = """
                document.getElementById('counter-btn').click();
                expect(document.getElementById('counter-btn').textContent).toBe('Clicks: 2');
            """.trimIndent()
        )
        assertTrue(inlineResult.passed)
        assertEquals(3, controller.recordedResults.size)
    }

    @Test
    fun testCliConfigParsingAndLogWriterFormatting() {
        val cliConfig = WebTestRunnerCliConfig.parse(
            listOf(
                "--web-dist", "build/dist/js/productionExecutable",
                "--entry", "index.html",
                "--filter", "CounterButtonSuite",
                "--log-path", "build/reports/desktop-tests.log",
                "--report-dir", "build/reports",
                "--timeout", "45000",
                "--headless"
            )
        )

        assertEquals("build/dist/js/productionExecutable", cliConfig.webDistDir)
        assertEquals("index.html", cliConfig.entryFile)
        assertEquals("build/dist/js/productionExecutable/index.html", cliConfig.resolveLoadUrl())
        assertEquals("CounterButtonSuite", cliConfig.filterPattern)
        assertEquals("build/reports/desktop-tests.log", cliConfig.logFilePath)
        assertEquals("build/reports", cliConfig.reportDirPath)
        assertEquals(45_000L, cliConfig.timeoutMs)
        assertTrue(cliConfig.headless)
        assertTrue(cliConfig.hasTargetToRun)

        val controller = WebTestRunnerController()
        controller.loadWebHtml(
            """
            <!DOCTYPE html>
            <html>
            <body>
                <script>
                    describe('LogWriterSuite', function() {
                        it('passes arithmetic check', function() {
                            expect(2 + 2).toBe(4);
                        });
                    });
                </script>
            </body>
            </html>
            """.trimIndent()
        )
        val report = controller.runRegisteredGuestTests()

        val logWriter = WebTestRunnerLogWriter()
        val interruptedText = logWriter.writeInitialInterruptedMarker(null)
        assertTrue(interruptedText.contains("Status: Tests interupted"))

        val finalLog = logWriter.formatDesktopTestsLog(
            report = report,
            consoleMessages = controller.consoleMessages,
            jsErrors = controller.javaScriptErrors
        )
        assertTrue(finalLog.contains("[PASS] LogWriterSuite > passes arithmetic check"))
        assertTrue(finalLog.contains("SUCCESS: All tests completed cleanly."))

        val junitXml = logWriter.formatJUnitXml(report)
        assertTrue(junitXml.contains("<testsuite"))
        assertTrue(junitXml.contains("name=\"passes arithmetic check\""))
    }

    @Test
    fun testWebTestRunnerFragmentUpdatesBadgesOnTestCompletion() {
        val fragment = WebTestRunnerFragment()
        assertNotNull(fragment.webView)
        assertEquals("Ready", fragment.currentStatusBadge.text)

        fragment.controller.loadWebHtml(
            """
            <!DOCTYPE html>
            <html>
            <body>
                <script>
                    describe('FragmentHudSuite', function() {
                        it('updates HUD badges on pass', function() {
                            expect('genericui').toContain('ui');
                        });
                    });
                </script>
            </body>
            </html>
            """.trimIndent()
        )

        val report = fragment.controller.runRegisteredGuestTests()
        assertTrue(report.allPassed)
        assertEquals("Passed: 1", fragment.passedCountBadge.text)
        assertEquals("Failed: 0", fragment.failedCountBadge.text)
        assertEquals(BootstrapType.SUCCESS, fragment.currentStatusBadge.type)
    }
}
