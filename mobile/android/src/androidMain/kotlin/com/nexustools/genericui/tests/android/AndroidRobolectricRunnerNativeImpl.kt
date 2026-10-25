package com.nexustools.genericui.tests.android

import com.nexustools.genericui.android.AndroidNativeFactory
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.tests.mobile.MobileSharedRunnerNativeImpl

/**
 * Android Robolectric / JUnit runner implementation (`:tests:mobile:android`) of `JestElectricRunnerNativeImpl`.
 */
class AndroidRobolectricRunnerNativeImpl : MobileSharedRunnerNativeImpl({ AndroidNativeFactory() }) {

    override val backendName: String
        get() = "android-robolectric"

    override fun ensureBackendInitialized() {
        if (GenericUI.activeFactory !is AndroidNativeFactory) {
            GenericUI.initialize(AndroidNativeFactory())
        }
    }
}
