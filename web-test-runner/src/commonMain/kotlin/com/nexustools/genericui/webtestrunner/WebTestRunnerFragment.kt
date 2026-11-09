package com.nexustools.genericui.webtestrunner

import com.nexustools.genericui.component.fragment.Fragment
import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.fragment.MainAppFragment
import com.nexustools.genericui.component.layout.HStack
import com.nexustools.genericui.component.layout.VStack
import com.nexustools.genericui.component.typography.Badge
import com.nexustools.genericui.component.typography.Heading
import com.nexustools.genericui.component.typography.Text
import com.nexustools.genericui.enums.bootstrap.AlignItems
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.bootstrap.FontWeight
import com.nexustools.genericui.enums.bootstrap.JustifyContent
import com.nexustools.genericui.enums.bootstrap.SpacingScale
import com.nexustools.genericui.ext.webview.WebView
import com.nexustools.genericui.ext.webview.WebViewConfig
import com.nexustools.genericui.tests.JestSuiteReport

/**
 * Primary `@MainAppFragment` for the **GenericUI Web Test Runner** native desktop application.
 *
 * Configured strictly for native desktop backends (`backends { allNativeDesktop() }`:
 * `:windows`, `:macos`, `:gtk`, `:qt`) and uses `:extensions:webview` ([WebView]) and its
 * communication functions to load and test GenericUI web builds.
 */
@MainAppFragment
class WebTestRunnerFragment : Fragment() {

    val webView: WebView = WebView(
        config = WebViewConfig(
            enableJavaScript = true,
            allowFileAccess = true
        )
    ).apply {
        id = "runner-webview"
    }

    val controller: WebTestRunnerController = WebTestRunnerController(webView)
    val logWriter: WebTestRunnerLogWriter = WebTestRunnerLogWriter()

    val suiteTitleHeading: Heading = Heading(
        text = "GenericUI Web Test Runner",
        level = 5
    ).apply {
        id = "runner-suite-title"
        getBootstrap().fontWeight = FontWeight.BOLD
    }

    val currentStatusBadge: Badge = Badge(
        text = "Ready",
        type = BootstrapType.INFO,
        pill = true
    ).apply {
        id = "runner-status-badge"
    }

    val passedCountBadge: Badge = Badge(
        text = "Passed: 0",
        type = BootstrapType.SUCCESS,
        pill = true
    ).apply {
        id = "runner-passed-badge"
    }

    val failedCountBadge: Badge = Badge(
        text = "Failed: 0",
        type = BootstrapType.SECONDARY,
        pill = true
    ).apply {
        id = "runner-failed-badge"
    }

    val latestConsoleText: Text = Text(
        text = "Waiting for web build or test suite..."
    ).apply {
        id = "runner-console-text"
    }

    var lastReport: JestSuiteReport? = null
        private set

    private var uiBuilt: Boolean = false

    init {
        controller.onStatusUpdated = { suiteName, banner, passed, failed, latestLog ->
            suiteTitleHeading.text = suiteName
            currentStatusBadge.text = banner
            currentStatusBadge.type = when {
                failed > 0 -> BootstrapType.DANGER
                passed > 0 -> BootstrapType.SUCCESS
                else -> BootstrapType.INFO
            }
            passedCountBadge.text = "Passed: $passed"
            failedCountBadge.text = "Failed: $failed"
            failedCountBadge.type = if (failed > 0) BootstrapType.DANGER else BootstrapType.SECONDARY
            latestConsoleText.text = latestLog
        }
        ensureUiBuilt()
    }

    override fun onCreate(savedState: FragmentBundle?) {
        super.onCreate(savedState)
        if (!uiBuilt || children.isEmpty()) {
            uiBuilt = false
            ensureUiBuilt()
        }
        val args = savedState?.getCommandLineArgs().orEmpty()
        if (args.isNotEmpty()) {
            val cliConfig = WebTestRunnerCliConfig.parse(args)
            if (cliConfig.hasTargetToRun) {
                executeCliSession(cliConfig)
            }
        }
    }

    fun executeCliSession(cliConfig: WebTestRunnerCliConfig): JestSuiteReport {
        logWriter.writeInitialInterruptedMarker(cliConfig.logFilePath)
        val report = controller.runFromCliConfig(cliConfig)
        lastReport = report
        val logOutput = logWriter.writeFinalReports(
            report = report,
            logFilePath = cliConfig.logFilePath,
            reportDirPath = cliConfig.reportDirPath,
            consoleMessages = controller.consoleMessages,
            jsErrors = controller.javaScriptErrors
        )
        println(report.formatSummary())
        println(logOutput)
        if (!cliConfig.keepOpen) {
            setTimeout(16) {
                hostingWindow?.close()
            }
        }
        return report
    }

    private fun ensureUiBuilt() {
        if (uiBuilt) return
        uiBuilt = true
        clearChildren()

        val statusHeader = HStack(gap = SpacingScale.THREE).apply {
            id = "runner-hud-bar"
            setAlignItems(AlignItems.CENTER)
            setJustifyContent(JustifyContent.BETWEEN)
            getBootstrap().apply {
                paddingX = SpacingScale.THREE
                paddingY = SpacingScale.TWO
            }
            addChild(suiteTitleHeading)
            addChild(
                HStack(gap = SpacingScale.TWO).apply {
                    setAlignItems(AlignItems.CENTER)
                    addChild(currentStatusBadge)
                    addChild(passedCountBadge)
                    addChild(failedCountBadge)
                }
            )
        }

        val rootLayout = VStack(gap = SpacingScale.TWO).apply {
            id = "runner-root-layout"
            getBootstrap().apply {
                paddingAll = SpacingScale.TWO
            }
            addChild(statusHeader)
            addChild(webView)
            addChild(latestConsoleText)
        }

        addChild(rootLayout)
    }
}
