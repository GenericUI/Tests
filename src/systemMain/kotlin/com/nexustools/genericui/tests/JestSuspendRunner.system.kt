package com.nexustools.genericui.tests

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

actual fun <T> runJestSuspend(timeoutMs: Long, block: suspend () -> T): T =
    runBlocking {
        withTimeout(timeoutMs) {
            block()
        }
    }
