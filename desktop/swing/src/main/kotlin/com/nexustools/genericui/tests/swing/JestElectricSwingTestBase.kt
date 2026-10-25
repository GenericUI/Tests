package com.nexustools.genericui.tests.swing

import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.tests.JestSuiteReport
import com.nexustools.genericui.tests.JestSuiteScope
import org.junit.After
import org.junit.Before

/**
 * Base class for JUnit 4 tests running against the `:desktop:swing` backend using `JestElectricRunner`.
 */
abstract class JestElectricSwingTestBase(
    private val headless: Boolean = true
) {
    protected lateinit var swingRunnerImpl: SwingJUnitRunnerNativeImpl

    @Before
    fun setUpJestElectricSwingRunner() {
        swingRunnerImpl = SwingJUnitRunnerNativeImpl(headless = headless)
        swingRunnerImpl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(swingRunnerImpl)
    }

    @After
    fun tearDownJestElectricSwingRunner() {
        swingRunnerImpl.closeVisualWindow()
        JestElectricNativeRegistry.resetToDefault()
    }

    protected fun describeSwingSuite(
        suiteName: String,
        block: JestSuiteScope.() -> Unit
    ): JestSuiteReport = JestElectricRunner.describe(
        suiteName = suiteName,
        visualOnWindows = false,
        throwOnFailure = true,
        runnerImpl = swingRunnerImpl,
        block = block
    )
}
