package com.nexustools.genericui.tests.android

import com.nexustools.genericui.tests.JestElectricNativeRegistry
import com.nexustools.genericui.tests.JestElectricRunner
import com.nexustools.genericui.tests.JestSuiteReport
import com.nexustools.genericui.tests.JestSuiteScope
import org.junit.After
import org.junit.Before

/**
 * Base class for Android Robolectric / JUnit tests running against `:mobile:android` with `JestElectricRunner`.
 */
abstract class JestElectricRobolectricTestBase {

    protected lateinit var androidRunnerImpl: AndroidRobolectricRunnerNativeImpl

    @Before
    fun setUpJestElectricAndroidRunner() {
        androidRunnerImpl = AndroidRobolectricRunnerNativeImpl()
        androidRunnerImpl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(androidRunnerImpl)
    }

    @After
    fun tearDownJestElectricAndroidRunner() {
        androidRunnerImpl.closeVisualWindow()
        JestElectricNativeRegistry.resetToDefault()
    }

    protected fun describeAndroidSuite(
        suiteName: String,
        block: JestSuiteScope.() -> Unit
    ): JestSuiteReport = JestElectricRunner.describe(
        suiteName = suiteName,
        visualOnWindows = false,
        throwOnFailure = true,
        runnerImpl = androidRunnerImpl,
        block = block
    )
}
