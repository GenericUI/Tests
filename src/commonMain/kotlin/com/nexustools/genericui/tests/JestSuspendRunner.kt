package com.nexustools.genericui.tests

/**
 * Runs a suspending test block synchronously until completion or [timeoutMs] on platforms that
 * support blocking test dispatchers (JVM, Android, Native), or synchronously up to completion on Web.
 */
expect fun <T> runJestSuspend(timeoutMs: Long = 10_000L, block: suspend () -> T): T
