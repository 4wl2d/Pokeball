package ballarch.kernel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Exhaustive checks of the knowledge algebra (Core §4.2): the connectives are
 * exact, composition is a commutative monoid, and composition of honest
 * constituents is honest and coordinate-wise most informative.
 */
class KnowledgeTest {
    private val tris = Tri.entries
    private val outcomes: List<Outcome<Unit>> = listOf(
        Outcome.Done(Unit),
        Outcome.NotDone(NotDoneReason.NoAnswer),
        Outcome.Unknown(UnknownReason.DeadlineAfterSend),
    )

    /** The ground truths a three-valued value admits. */
    private fun completions(t: Tri): List<Boolean> = listOf(true, false).filter { t.admits(it) }

    private fun completions(o: Outcome<*>?): List<Boolean> = when (o) {
        null -> listOf(false) // no compensation: nothing was undone
        is Outcome.Done -> listOf(true)
        is Outcome.NotDone -> listOf(false)
        is Outcome.Unknown -> listOf(true, false)
    }

    /** The most informative value that admits every element of [values]. */
    private fun supremum(values: Collection<Boolean>): Tri = when (values.toSet()) {
        setOf(true) -> Tri.Yes
        setOf(false) -> Tri.No
        else -> Tri.Maybe
    }

    @Test
    fun `connectives equal supervaluation over completions`() {
        for (a in tris) {
            assertEquals(supremum(completions(a).map { !it }), !a, "not $a")
            for (b in tris) {
                val ands = completions(a).flatMap { x -> completions(b).map { y -> x && y } }
                val ors = completions(a).flatMap { x -> completions(b).map { y -> x || y } }
                assertEquals(supremum(ands), a and b, "$a and $b")
                assertEquals(supremum(ors), a or b, "$a or $b")
            }
        }
    }

    @Test
    fun `plus is a commutative monoid on the seven values`() {
        val all = Knowledge.values
        for (x in all) {
            assertEquals(x, x + Knowledge.NoEffects)
            assertEquals(x, Knowledge.NoEffects + x)
            for (y in all) {
                assertTrue(x + y in all)
                assertEquals(x + y, y + x, "$x + $y")
                for (z in all) assertEquals((x + y) + z, x + (y + z), "($x + $y) + $z")
            }
        }
    }

    private data class Constituent(val forward: Outcome<Unit>, val undo: Outcome<Unit>?)

    private val constituents: List<Constituent> =
        outcomes.flatMap { f -> (listOf<Outcome<Unit>?>(null) + outcomes).map { u -> Constituent(f, u) } }

    private fun sequences(n: Int): List<List<Constituent>> =
        if (n == 0) listOf(emptyList()) else sequences(n - 1).flatMap { prefix -> constituents.map { prefix + it } }

    /** Every combination of real effects consistent with the constituents' outcomes. */
    private fun truths(parts: List<Constituent>): List<List<Boolean>> =
        parts.fold(listOf(emptyList())) { acc, c ->
            val nets = completions(c.forward).flatMap { applied -> completions(c.undo).map { undone -> applied && !undone } }.distinct()
            acc.flatMap { prefix -> nets.map { prefix + it } }
        }

    @Test
    fun `composition of honest constituents is honest and coordinate-wise exact`() {
        var checked = 0
        for (n in 0..4) {
            for (parts in sequences(n)) {
                val k = parts.map { it.forward.compensatedBy(it.undo) }.combine()
                val worlds = truths(parts)
                for (net in worlds) {
                    assertTrue(k.admits(allApplied = net.all { it }, anyApplied = net.any { it }), "$parts: $k does not admit $net")
                    checked++
                }
                assertEquals(supremum(worlds.map { w -> w.all { it } }), k.all, "all of $parts")
                assertEquals(supremum(worlds.map { w -> w.any { it } }), k.any, "any of $parts")
            }
        }
        assertEquals(69_905, checked, "worlds explored for up to four constituents")
    }

    @Test
    fun `an absorbing compensation turns an unknown effect into a definite one`() {
        val unknown = Outcome.Unknown(UnknownReason.LostAfterSend)
        assertEquals(Knowledge.NotDone, unknown.compensatedBy(Outcome.Done(Unit)))
        assertEquals(Knowledge.Unknown, unknown.compensatedBy(Outcome.NotDone<Unit>(NotDoneReason.NoAnswer)))
        assertEquals(Knowledge.Unknown, unknown.compensatedBy(Outcome.Unknown(UnknownReason.DeadlineAfterSend)))
    }

    @Test
    fun `business refusals inside Done are not effects`() {
        val declined: Outcome<String> = Outcome.Done("declined")
        assertEquals(Knowledge.NotDone, declined.knowledge { it == "charged" })
        assertEquals(Knowledge.Done, Outcome.Done("charged").knowledge { it == "charged" })
    }

    @Test
    fun `a composite projects to an honest three-valued outcome`() {
        val expected = mapOf(
            Knowledge.Done to "Done",
            Knowledge.NotDone to "NotDone",
            Knowledge.NoEffects to "NotDone",
            Knowledge.Partial to "Unknown",
            Knowledge.NotAllDone to "Unknown",
            Knowledge.SomeDone to "Unknown",
            Knowledge.Unknown to "Unknown",
        )
        for ((k, kind) in expected) {
            val o = k.toOutcome(Unit, NotDoneReason.NoAnswer, UnknownReason.LostAfterSend)
            assertEquals(kind, o::class.simpleName, "$k")
        }
        assertEquals(Knowledge.Partial, Outcome.Done(Unit).knowledge() + Outcome.NotDone<Unit>(NotDoneReason.NoAnswer).knowledge())
    }

    @Test
    fun `pairs no operation can produce are rejected`() {
        assertFailsWith<IllegalArgumentException> { Knowledge.of(Tri.Yes, Tri.Maybe) }
        assertFailsWith<IllegalArgumentException> { Knowledge.of(Tri.Maybe, Tri.No) }
        assertEquals(7, Knowledge.values.toSet().size)
    }
}
