package com.nexustools.genericui.tests

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

actual fun <T> runJestSuspend(timeoutMs: Long, block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(r: Result<T>) {
            result = r
        }
    })
    val resolved = result ?: throw AssertionError(
        "Web JestElectric suspend block did not complete synchronously within ${timeoutMs}ms"
    )
    return resolved.getOrThrow()
}
