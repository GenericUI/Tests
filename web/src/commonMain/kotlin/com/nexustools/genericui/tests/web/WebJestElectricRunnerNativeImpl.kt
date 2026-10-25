package com.nexustools.genericui.tests.web

import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.tests.DefaultJestElectricRunnerNativeImpl
import com.nexustools.genericui.web.WebNativeFactory

/**
 * Web HTML5 DOM runner implementation (`:tests:web`) of `JestElectricRunnerNativeImpl`.
 */
class WebJestElectricRunnerNativeImpl : DefaultJestElectricRunnerNativeImpl() {

    override val backendName: String
        get() = "web-dom"

    override fun ensureBackendInitialized() {
        if (GenericUI.activeFactory !is WebNativeFactory) {
            GenericUI.initialize(WebNativeFactory())
        }
    }
}
