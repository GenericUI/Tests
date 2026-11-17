package com.nexustools.genericui.tests.android

import com.nexustools.genericui.android.AndroidNativeFactory
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.system.BackendTarget
import com.nexustools.genericui.tests.mobile.MobileSharedRunnerNativeImpl

/**
 * Android Robolectric / JUnit runner implementation of `JestElectricRunnerNativeImpl`.
 */
class AndroidRobolectricRunnerNativeImpl : MobileSharedRunnerNativeImpl(
    factoryProvider = { AndroidNativeFactory() },
    expectedTarget = BackendTarget.Android
) {

    override val backendTarget: BackendTarget
        get() = BackendTarget.Android

    override val backendName: String
        get() = "android-robolectric"

    override fun ensureBackendInitialized() {
        if (GenericUI.activeFactory?.backendTarget != BackendTarget.Android) {
            GenericUI.initialize(AndroidNativeFactory())
        }
    }
}
