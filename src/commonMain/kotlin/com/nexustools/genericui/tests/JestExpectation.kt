package com.nexustools.genericui.tests

/**
 * Jest-style fluent expectation matcher (`expect(value).toBe(...)`, `expect(value).not.toBe(...)`).
 */
class JestExpectation<T>(
    private val actual: T,
    private val inverted: Boolean = false
) {
    /**
     * Returns an inverted expectation that negates subsequent matcher checks.
     */
    val not: JestExpectation<T>
        get() = JestExpectation(actual, !inverted)

    private fun checkCondition(condition: Boolean, positiveMessage: () -> String, negativeMessage: () -> String) {
        if (!inverted && !condition) {
            throw AssertionError(positiveMessage())
        }
        if (inverted && condition) {
            throw AssertionError(negativeMessage())
        }
    }

    fun toBe(expected: T) {
        checkCondition(
            condition = actual == expected,
            positiveMessage = { "Expected <$expected> but received <$actual>." },
            negativeMessage = { "Expected value not to be <$expected>, but it was." }
        )
    }

    fun toEqual(expected: Any?) {
        checkCondition(
            condition = actual == expected,
            positiveMessage = { "Expected value equal to <$expected> but received <$actual>." },
            negativeMessage = { "Expected value not equal to <$expected>, but it was." }
        )
    }

    fun toBeNull() {
        checkCondition(
            condition = actual == null,
            positiveMessage = { "Expected <null> but received <$actual>." },
            negativeMessage = { "Expected non-null value, but received <null>." }
        )
    }

    fun toBeNotNull() {
        checkCondition(
            condition = actual != null,
            positiveMessage = { "Expected non-null value, but received <null>." },
            negativeMessage = { "Expected <null> but received <$actual>." }
        )
    }

    fun toBeTruthy() {
        val truthy = when (val v = actual) {
            null -> false
            is Boolean -> v
            is Number -> v.toDouble() != 0.0 && !v.toDouble().isNaN()
            is CharSequence -> v.isNotEmpty()
            is Collection<*> -> v.isNotEmpty()
            else -> true
        }
        checkCondition(
            condition = truthy,
            positiveMessage = { "Expected <$actual> to be truthy." },
            negativeMessage = { "Expected <$actual> not to be truthy." }
        )
    }

    fun toBeFalsy() {
        val truthy = when (val v = actual) {
            null -> false
            is Boolean -> v
            is Number -> v.toDouble() != 0.0 && !v.toDouble().isNaN()
            is CharSequence -> v.isNotEmpty()
            is Collection<*> -> v.isNotEmpty()
            else -> true
        }
        checkCondition(
            condition = !truthy,
            positiveMessage = { "Expected <$actual> to be falsy." },
            negativeMessage = { "Expected <$actual> not to be falsy." }
        )
    }

    fun toContain(element: Any?) {
        val contains = when (val v = actual) {
            is CharSequence -> element is CharSequence && v.contains(element)
            is Iterable<*> -> v.contains(element)
            is Array<*> -> v.contains(element)
            is Map<*, *> -> v.containsKey(element) || v.containsValue(element)
            else -> false
        }
        checkCondition(
            condition = contains,
            positiveMessage = { "Expected <$actual> to contain <$element>." },
            negativeMessage = { "Expected <$actual> not to contain <$element>." }
        )
    }

    fun toHaveLength(expectedLength: Int) {
        val length = when (val v = actual) {
            is CharSequence -> v.length
            is Collection<*> -> v.size
            is Array<*> -> v.size
            is Map<*, *> -> v.size
            else -> throw AssertionError("Cannot determine length of <$actual>.")
        }
        checkCondition(
            condition = length == expectedLength,
            positiveMessage = { "Expected length <$expectedLength> but received <$length> for <$actual>." },
            negativeMessage = { "Expected length not to be <$expectedLength> for <$actual>." }
        )
    }

    fun toBeGreaterThan(bound: Number) {
        val num = (actual as? Number)?.toDouble()
            ?: throw AssertionError("Expected a numeric value for toBeGreaterThan, but got <$actual>.")
        val target = bound.toDouble()
        checkCondition(
            condition = num > target,
            positiveMessage = { "Expected <$num> to be greater than <$target>." },
            negativeMessage = { "Expected <$num> not to be greater than <$target>." }
        )
    }

    fun toBeLessThan(bound: Number) {
        val num = (actual as? Number)?.toDouble()
            ?: throw AssertionError("Expected a numeric value for toBeLessThan, but got <$actual>.")
        val target = bound.toDouble()
        checkCondition(
            condition = num < target,
            positiveMessage = { "Expected <$num> to be less than <$target>." },
            negativeMessage = { "Expected <$num> not to be less than <$target>." }
        )
    }
}
