package ballarch.examples.trip

import ballarch.examples.trip.app.tripComposition
import ballarch.kernel.BallId
import ballarch.kernel.Knowledge
import ballarch.kernel.Tri
import ballarch.testkit.ExhaustiveChooser
import ballarch.testkit.Fate
import ballarch.testkit.Harness
import ballarch.testkit.Profile
import ballarch.testkit.SimPort
import ballarch.testkit.explore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A provider that fences a trip's reference once it is voided: later bookings for it have no effect. */
private class Provider {
    val booked = HashMap<String, Int>()
    private val fenced = HashSet<String>()

    fun book(ref: TripRef): Boolean {
        if (ref.trip in fenced) return false
        booked.merge(ref.trip, 1, Int::plus)
        return true
    }

    fun undo(ref: TripRef) {
        booked.remove(ref.trip)
        fenced += ref.trip
    }

    fun holds(trip: String) = (booked[trip] ?: 0) > 0
}

class TripFlowTest {
    private val id = BallId("trip", "t1")

    /**
     * Composition closure (rule R14) under faults: whatever the network and a
     * crash do, the trip's settled knowledge admits the real net bookings, and
     * the flight is never booked twice.
     */
    @Test
    fun `the trip's knowledge admits the real bookings under every fate and crash point`() {
        val settled = LinkedHashMap<Knowledge, Int>()
        var twoValuedLies = 0
        val forward = listOf(Fate.Reply, Fate.LoseRequest, Fate.LoseReply, Fate.ErrorAfterApply, Fate.RefuseConnection)
        val undo = listOf(Fate.Reply, Fate.LoseReply, Fate.RefuseConnection)
        val runs = explore(ExhaustiveChooser(maxChoices = 12)) { chooser ->
            val airline = Provider()
            val hotel = Provider()
            val h = Harness(chooser, Profile.Durable) {
                tripComposition(
                    flightBook = SimPort(FlightBook, scheduler, chooser, truth, forward) {
                        if (airline.book(it)) FlightReply.Booked("12A") else FlightReply.SoldOut
                    },
                    flightVoid = SimPort(FlightVoid, scheduler, chooser, truth, undo) { airline.undo(it) },
                    hotelBook = SimPort(HotelBook, scheduler, chooser, truth, forward) {
                        if (hotel.book(it)) HotelReply.Booked else HotelReply.Full
                    },
                    hotelCancel = SimPort(HotelCancel, scheduler, chooser, truth, undo) { hotel.undo(it) },
                )
            }
            h.maybeCrashOnce(window = 10)
            h.start()
            h.send(id, TripRequest.Book)
            h.runUntilIdle()
            h.assertInvariants()
            assertTrue((airline.booked["t1"] ?: 0) <= 1, "flight booked twice: ${chooser.history}")
            val state = h.runtime.inspect(id) as TripState
            if (state.stage == Stage.New) return@explore // the crash came before the request was accepted
            assertEquals(Stage.Settled, state.stage, "unfinished: ${chooser.history}")
            val flightHeld = airline.holds("t1")
            val hotelHeld = hotel.holds("t1")
            assertTrue(
                state.knowledge.admits(allApplied = flightHeld && hotelHeld, anyApplied = flightHeld || hotelHeld),
                "claims ${state.knowledge} but flight=$flightHeld hotel=$hotelHeld: ${chooser.history}",
            )
            settled.merge(state.knowledge, 1, Int::plus)
            // Control: the usual two-valued status ("booked" only if both bookings answered Booked,
            // otherwise "failed", meaning nothing is held) on the same runs.
            val claimsBooked = state.flight.effect == Tri.Yes && state.hotel.effect == Tri.Yes
            if (claimsBooked != (flightHeld && hotelHeld) || (!claimsBooked && (flightHeld || hotelHeld))) twoValuedLies++
        }
        println("trip exploration: $runs runs, settled knowledge $settled, two-valued status wrong in $twoValuedLies runs")
        assertTrue(twoValuedLies > 0, "the control should be wrong somewhere")
        // Every honest end is reachable, and compensation removes most ambiguity.
        assertTrue(settled.keys.containsAll(listOf(Knowledge.Done, Knowledge.NotDone)), "$settled")
        assertTrue(settled.getValue(Knowledge.NotDone) > (settled[Knowledge.Unknown] ?: 0))
    }
}
