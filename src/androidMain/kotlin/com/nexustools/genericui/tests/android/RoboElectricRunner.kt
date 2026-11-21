package com.nexustools.genericui.tests.android

import com.nexustools.genericui.android.AndroidViewHandle
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import org.junit.runners.model.InitializationError
import org.robolectric.DefaultTestLifecycle
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.TestLifecycle
import org.robolectric.annotation.Config
import org.robolectric.pluginapi.config.GlobalConfigProvider
import java.lang.reflect.Method

/**
 * Unified JUnit4 / Robolectric test runner for Android (`:android`) powering `JestElectricRunner`.
 *
 * Binds a [GlobalConfigProvider] targeting SDK 35 (compatible with Robolectric 4.16.1 when `compileSdk = 37`),
 * binds the live Robolectric Android `Application` context into [AndroidViewHandle.activeContext],
 * and automatically installs [AndroidJestElectricBootstrap] inside Robolectric's sandboxed
 * [TestLifecycle].
 */
open class RoboElectricRunner @Throws(InitializationError::class) constructor(
    testClass: Class<*>
) : RobolectricTestRunner(
    testClass,
    defaultInjector()
        .bind(
            GlobalConfigProvider::class.java,
            GlobalConfigProvider { Config.Builder().setSdk(35).build() }
        )
        .build()
) {

    override fun getTestLifecycleClass(): Class<out TestLifecycle> =
        RoboElectricTestLifecycle::class.java

    class RoboElectricTestLifecycle : DefaultTestLifecycle() {
        override fun beforeTest(method: Method?) {
            super.beforeTest(method)
            runCatching { RuntimeEnvironment.getApplication() }.getOrNull()?.let { appCtx ->
                AndroidViewHandle.activeContext = appCtx
            }
            AndroidJestElectricBootstrap.install()
        }

        override fun afterTest(method: Method?) {
            try {
                (JestElectricNativeRegistry.current() as? AndroidRobolectricRunnerNativeImpl)?.closeVisualWindow()
                JestElectricNativeRegistry.resetToDefault()
            } finally {
                super.afterTest(method)
            }
        }
    }
}
