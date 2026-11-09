package com.nexustools.genericui.webtestrunner

/**
 * Command-line configuration for the GenericUI Native Desktop Web Test Runner app.
 *
 * Supports both `JestElectricRunner` (`--web-dist`, `--filter`, `--report-dir`, `--headless`)
 * and `@nexustools/nwjstestrunner` (`--project-root`, `-r`, `--webview-entry`, `--test-files`,
 * `--log-path`, `-o`, `--timeout`, `--keep-open`) CLI arguments.
 */
data class WebTestRunnerCliConfig(
    val webDistDir: String? = null,
    val entryFile: String = "index.html",
    val targetUrl: String? = null,
    val testFiles: List<String> = emptyList(),
    val filterPattern: String? = null,
    val logFilePath: String? = null,
    val reportDirPath: String? = null,
    val timeoutMs: Long = 120_000L,
    val headless: Boolean = false,
    val keepOpen: Boolean = false
) {
    val hasTargetToRun: Boolean
        get() = !webDistDir.isNullOrBlank() || !targetUrl.isNullOrBlank() || testFiles.isNotEmpty()

    fun resolveLoadUrl(): String? {
        if (!targetUrl.isNullOrBlank()) return targetUrl.trim()
        if (!webDistDir.isNullOrBlank()) {
            val cleanDir = webDistDir.trim().trimEnd('/', '\\')
            val cleanEntry = entryFile.trim().trimStart('/', '\\').ifEmpty { "index.html" }
            return "$cleanDir/$cleanEntry"
        }
        return null
    }

    companion object {
        fun parse(args: Array<String>): WebTestRunnerCliConfig =
            parse(args.toList())

        fun parse(args: List<String>): WebTestRunnerCliConfig {
            var webDistDir: String? = null
            var entryFile = "index.html"
            var targetUrl: String? = null
            val testFiles = ArrayList<String>()
            var filterPattern: String? = null
            var logFilePath: String? = null
            var reportDirPath: String? = null
            var timeoutMs = 120_000L
            var headless = false
            var keepOpen = false

            var i = 0
            while (i < args.size) {
                val raw = args[i].trim()
                if (raw.isEmpty()) {
                    i++
                    continue
                }
                val eqIdx = raw.indexOf('=')
                val flag = if (eqIdx > 0 && raw.startsWith("-")) raw.substring(0, eqIdx) else raw
                val inlineVal = if (eqIdx > 0 && raw.startsWith("-")) raw.substring(eqIdx + 1) else null

                fun nextValue(): String? {
                    if (inlineVal != null) return inlineVal
                    if (i + 1 < args.size) {
                        i++
                        return args[i].trim()
                    }
                    return null
                }

                when (flag) {
                    "--web-dist", "--project-root", "-r" -> {
                        webDistDir = nextValue()
                    }
                    "--entry", "--webview-entry" -> {
                        nextValue()?.takeIf { it.isNotEmpty() }?.let { entryFile = it }
                    }
                    "--url" -> {
                        targetUrl = nextValue()
                    }
                    "--test-files" -> {
                        nextValue()?.split(',', ';')
                            ?.map { it.trim() }
                            ?.filter { it.isNotEmpty() }
                            ?.let { testFiles.addAll(it) }
                    }
                    "--filter", "-t", "--test-name-pattern" -> {
                        filterPattern = nextValue()
                    }
                    "--log-path", "-o" -> {
                        logFilePath = nextValue()
                    }
                    "--report-dir" -> {
                        reportDirPath = nextValue()
                    }
                    "--timeout" -> {
                        nextValue()?.toLongOrNull()?.let { timeoutMs = it.coerceAtLeast(1L) }
                    }
                    "--headless", "--no-visual" -> {
                        headless = true
                    }
                    "--keep-open" -> {
                        keepOpen = true
                    }
                    else -> {
                        if (!raw.startsWith("-")) {
                            when {
                                raw.endsWith(".js", ignoreCase = true) -> testFiles.add(raw)
                                raw.endsWith(".html", ignoreCase = true) ||
                                    raw.endsWith(".htm", ignoreCase = true) ||
                                    raw.startsWith("http://", ignoreCase = true) ||
                                    raw.startsWith("https://", ignoreCase = true) ||
                                    raw.startsWith("file://", ignoreCase = true) -> targetUrl = raw
                                webDistDir == null -> webDistDir = raw
                            }
                        }
                    }
                }
                i++
            }

            return WebTestRunnerCliConfig(
                webDistDir = webDistDir,
                entryFile = entryFile,
                targetUrl = targetUrl,
                testFiles = testFiles,
                filterPattern = filterPattern,
                logFilePath = logFilePath,
                reportDirPath = reportDirPath,
                timeoutMs = timeoutMs,
                headless = headless,
                keepOpen = keepOpen
            )
        }
    }
}
