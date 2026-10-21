package bench.runner.fixtures

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Fixtures for BenchmarkTest: classes whose LCOM4 and CAMC were worked out by hand.

/** A value class: properties of this type mangle the accessor names of their owners. */
@JvmInline
value class Key(val value: String)

/** Only compiler-generated members (accessors `getKey-…`, `component1-…`, `copy-…`): not measured. */
data class Keyed(val key: Key, val n: Int)

/** Only compiler-generated members (`values`, `valueOf`, `getEntries`): not measured. */
enum class Light { RED, GREEN }

/** LCOM4 1: `add` uses `total` and `step` only inside its lambda, `read` uses `total`. */
class Counter(private val step: Int) {
    private var total = 0

    fun add(): () -> Unit = { total += step }

    fun read() = total
}

/**
 * LCOM4 1: `start` uses the private field `count` only inside a coroutine,
 * whose class reads and writes it through `access$getCount$p` and
 * `access$setCount$p`; `read` uses it directly.
 */
class Launcher(private val scope: CoroutineScope) {
    private var count = 0

    fun start() {
        scope.launch { count++ }
    }

    fun read() = count
}

/** LCOM4 1: both methods call the data class's `copy`, whose default-argument helper reads every field. */
data class Ticket(val id: String, val state: String) {
    fun paid() = copy(state = "paid")

    fun failed() = copy(state = "failed")
}

/**
 * Bansiya et al.'s `Employee` example without its constructor. With `this`:
 * M = {this, int}, {this, double, int}, {this, double}, {this, String};
 * T = {this, int, double, String}; CAMC = (2 + 3 + 2 + 2) / (4 · 4) = 9/16.
 */
class Employee {
    fun pay(hours: Int): Double = hours * 1.0

    fun tax(rate: Double, deductions: Int): Double = rate * deductions

    fun setPayRate(rate: Double): Double = rate

    fun changeAddress(address: String) = address.length
}
