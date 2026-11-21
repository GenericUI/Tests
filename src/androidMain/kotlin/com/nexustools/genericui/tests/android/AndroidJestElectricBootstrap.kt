package com.nexustools.genericui.tests.android

import com.nexustools.genericui.android.AndroidViewHandle
import com.nexustools.genericui.tests.JestElectricNativeRegistry
import org.robolectric.RuntimeEnvironment

/**
 * Bootstrapper that installs [AndroidRobolectricRunnerNativeImpl] into [JestElectricNativeRegistry]
 * for Android test runs powered by [RoboElectricRunner].
 */
object AndroidJestElectricBootstrap {

    /**
     * Installs and returns a fresh [AndroidRobolectricRunnerNativeImpl].
     */
    fun install(): AndroidRobolectricRunnerNativeImpl {
        runCatching { RuntimeEnvironment.getApplication() }.getOrNull()?.let { appCtx ->
            AndroidViewHandle.activeContext = appCtx
        }
        val impl = AndroidRobolectricRunnerNativeImpl()
        impl.ensureBackendInitialized()
        JestElectricNativeRegistry.register(impl)
        return impl
    }

    /**
     * Installs [AndroidRobolectricRunnerNativeImpl] into [JestElectricNativeRegistry] if not already active.
     */
    fun installIfEnabled(): AndroidRobolectricRunnerNativeImpl {
        val current = JestElectricNativeRegistry.current()
        if (current is AndroidRobolectricRunnerNativeImpl) {
            return current
        }
        return install()
    }
}
