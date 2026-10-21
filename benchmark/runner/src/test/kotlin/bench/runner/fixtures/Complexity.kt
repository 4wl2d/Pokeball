package bench.runner.fixtures

import kotlinx.coroutines.delay

// Fixtures for BenchmarkTest: small functions whose cyclomatic complexity was counted by hand
// under the rules in benchmark/README.md (1 + decisions; `catch` adds nothing; the branches the
// compiler generates do not count; lambdas count toward the function that declares them).

sealed interface Shape {
    data class Circle(val r: Int) : Shape
    data class Square(val a: Int) : Shape
    data object Point : Shape
}

enum class Color { RED, GREEN, BLUE }

class Ccn(private val flag: Boolean) {
    lateinit var name: String

    /** 1: no decision. */
    fun straight(x: Int) = x + 1

    /** 2: one `if`. */
    fun branch(x: Int) = if (x > 0) 1 else 2

    /** 3: the loop condition and one `if`. */
    fun loop(xs: List<Int>): Int {
        var s = 0
        for (x in xs) if (x > 0) s += x
        return s
    }

    /** 3: `if` and `&&`. */
    fun both(a: Boolean, b: Boolean) = if (a && b) 1 else 0

    /** 2: the elvis operator. */
    fun elvis(s: String?) = s ?: "none"

    /** 2: the safe call. */
    fun safeCall(s: String?) = s?.length

    /** 1: exception handlers add nothing. */
    fun caught(f: () -> Int): Int = try {
        f()
    } catch (e: IllegalStateException) {
        -1
    } catch (e: RuntimeException) {
        -2
    }

    /** 3: three cases of an exhaustive `when`; the check of the last one is generated. */
    fun whenSealed(s: Shape) = when (s) {
        is Shape.Circle -> 1
        is Shape.Square -> 2
        Shape.Point -> 3
    }

    /** 3: two cases and `else` over a nullable enum; the null check is generated. */
    fun whenEnum(c: Color?) = when (c) {
        Color.RED -> 1
        Color.GREEN -> 2
        else -> 3
    }

    /** 2: the `if` in the lambda passed to the inline `map`; the loop inside `map` is library code. */
    fun mapped(xs: List<Int>) = xs.map { if (it > 0) it else 0 }

    /** 2: the `if` in a lambda compiled to its own method counts toward this function. */
    fun lambda(): () -> Int = { if (flag) 1 else 2 }

    /** 2: one `if`; the coroutine state machine is generated. */
    suspend fun suspending(x: Int): Int {
        delay(1)
        if (x > 0) delay(2)
        return x
    }

    /** 1: the check that a `lateinit` property is initialised is generated. */
    fun usesLateinit() = name.length

    /** 1: casting a nullable value to a non-null type checks for null, which is generated. */
    fun cast(x: Any?) = (x as String).length

    /**
     * 2: the branch inside the standard library's `getOrNull`. It is an
     * `@InlineOnly` function, whose inlined code keeps the line of the call,
     * so it counts as a decision of the caller.
     */
    fun resultOrNull(r: Result<Int>) = r.getOrNull()

    /** 2: the branch inside the `@InlineOnly` `fold` of `Result`. */
    fun folded(r: Result<Int>) = r.fold({ it }, { -1 })

    /** 2: the `?:` inside the `@InlineOnly` `orEmpty`. */
    fun orEmptyMap(m: Map<String, Int>?) = m.orEmpty()

    /** 2: the branch inside the `@InlineOnly` `getOrDefault` of `Result`. */
    fun orDefault(r: Result<Int>) = r.getOrDefault(-1)

    /** 2: the branch inside the `@InlineOnly` `takeIf`; the lambda itself has none. */
    fun taken(s: String, keep: Boolean) = s.takeIf { keep }

    /** 2: the loop inside the `@InlineOnly` `repeat`. */
    fun repeated(n: Int): Int {
        var s = 0
        repeat(n) { s += it }
        return s
    }
}
