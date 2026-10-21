package ballarch.examples.trip

import ballarch.kernel.AdapterPort
import ballarch.kernel.Ball
import ballarch.kernel.Call
import ballarch.kernel.CallKey
import ballarch.kernel.Completion
import ballarch.kernel.Context
import ballarch.kernel.Decision
import ballarch.kernel.EffectClass
import ballarch.kernel.Knowledge
import ballarch.kernel.Outcome
import ballarch.kernel.RequestContext
import ballarch.kernel.Step
import ballarch.kernel.Tri
import ballarch.kernel.accept
import ballarch.kernel.effect
import ballarch.kernel.outcomeOf
import ballarch.kernel.reject
import ballarch.kernel.step

// ---- external ports

/** Every request is about one trip; the trip id is the reference the providers know. */
data class TripRef(val trip: String)

sealed interface FlightReply {
    data class Booked(val seat: String) : FlightReply
    data object SoldOut : FlightReply
}

/** The airline has no idempotency keys: every delivered booking request books a seat. */
val FlightBook = AdapterPort<TripRef, FlightReply>("airline.book", EffectClass.NonIdempotent)

/**
 * Voids every booking for the trip and refuses later booking requests for it.
 * Repeating it changes nothing. Its `Done` means that no seat is booked for the
 * trip and none can be booked later: an absorbing compensation (Core §4.2).
 */
val FlightVoid = AdapterPort<TripRef, Unit>("airline.void", EffectClass.Idempotent)

sealed interface HotelReply {
    data object Booked : HotelReply
    data object Full : HotelReply
}

/** The hotel deduplicates requests by call identity, so the runtime may repeat them. */
val HotelBook = AdapterPort<TripRef, HotelReply>("hotel.book", EffectClass.Idempotent)

/** Cancels the trip's room and refuses later bookings for it: absorbing, like [FlightVoid]. */
val HotelCancel = AdapterPort<TripRef, Unit>("hotel.cancel", EffectClass.Idempotent)

// ---- messages

sealed interface TripRequest {
    data object Book : TripRequest
    data object GetStatus : TripRequest
}

sealed interface TripReply {
    /** What the trip knows about its bookings. Derived only from call outcomes (rule R14). */
    data class Status(val stage: Stage, val knowledge: Knowledge) : TripReply
    data object AlreadyStarted : TripReply
}

enum class Stage { New, BookingFlight, BookingHotel, CancellingHotel, VoidingFlight, Settled }

// ---- state

/**
 * What is known about one booking: whether the request took effect, and
 * whether one of its compensations took effect. Compensations are absorbing,
 * so the booking's net effect is `effect ∧ ¬undone`. Several compensation
 * attempts combine with `or`: if any of them took effect, the booking is undone.
 */
data class Part(val effect: Tri = Tri.No, val undone: Tri = Tri.No) {
    /** Net effect according to the completions received so far. */
    val net: Tri get() = effect and !undone

    /** Net effect as an observer may claim it while the booking or its compensation is still open. */
    fun view(bookingOpen: Boolean, undoOpen: Boolean): Tri =
        (if (bookingOpen) Tri.Maybe else effect) and !(if (undoOpen) undone or Tri.Maybe else undone)
}

data class TripState(
    val stage: Stage = Stage.New,
    val flight: Part = Part(),
    val hotel: Part = Part(),
    /** The call whose completion the current stage waits for. */
    val call: CallKey? = null,
    /** Compensation calls made in the current stage. */
    val attempts: Int = 0,
) {
    /**
     * The trip's two bookings, combined by the knowledge algebra. A booking never
     * attempted is known `NotDone`; while a call is open, its effect may already
     * have happened, so it counts as `Maybe` until its completion arrives.
     */
    val knowledge: Knowledge
        get() = Knowledge.single(flight.view(stage == Stage.BookingFlight, stage == Stage.VoidingFlight)) +
            Knowledge.single(hotel.view(stage == Stage.BookingHotel, stage == Stage.CancellingHotel))
}

/**
 * A trip is a flight plus a hotel room, or nothing. This Flow books both and
 * compensates when it cannot have both. Whatever happens, it never claims more
 * than the outcomes of its calls establish: its status is the knowledge algebra
 * applied to them, so a caller one level up can treat the whole trip as one
 * honest operation.
 *
 * The flight request is never repeated (its port is NonIdempotent). An
 * `Unknown` booking is voided, which turns it into a definite `NotDone`.
 */
class TripFlow(
    private val timeoutMillis: Long = 1_000,
    private val maxCompensations: Int = 2,
) : Ball<TripState, TripRequest, TripReply> {
    override val type = "trip"

    override fun initial(key: String) = TripState()

    override fun decide(state: TripState, request: TripRequest, ctx: RequestContext): Decision<TripState, TripReply> =
        when (request) {
            TripRequest.Book ->
                if (state.stage != Stage.New) {
                    reject(TripReply.AlreadyStarted)
                } else {
                    val next = call(state.copy(stage = Stage.BookingFlight), FlightBook, ctx)
                    accept(next.state, status(next.state), *next.outputs.toTypedArray())
                }
            TripRequest.GetStatus -> accept(state, status(state))
        }

    override fun complete(state: TripState, completion: Completion<*>, ctx: Context): Step<TripState, TripReply> {
        if (completion.key != state.call) return step(state) // stale
        return when (state.stage) {
            Stage.BookingFlight -> {
                val booked = completion.outcomeOf(FlightBook)!!.effect { it is FlightReply.Booked }
                val next = state.copy(flight = Part(effect = booked))
                when (booked) {
                    Tri.Yes -> call(next.copy(stage = Stage.BookingHotel), HotelBook, ctx)
                    Tri.No -> settle(next)
                    Tri.Maybe -> compensate(next, Stage.VoidingFlight, ctx)
                }
            }
            Stage.BookingHotel -> {
                val booked = completion.outcomeOf(HotelBook)!!.effect { it == HotelReply.Booked }
                val next = state.copy(hotel = Part(effect = booked))
                when (booked) {
                    Tri.Yes -> settle(next)
                    Tri.No -> compensate(next, Stage.VoidingFlight, ctx)
                    Tri.Maybe -> compensate(next, Stage.CancellingHotel, ctx)
                }
            }
            Stage.CancellingHotel -> {
                val next = state.copy(hotel = undo(state.hotel, completion.outcomeOf(HotelCancel)!!))
                when {
                    next.hotel.undone == Tri.Yes || state.attempts >= maxCompensations -> compensate(next, Stage.VoidingFlight, ctx)
                    else -> compensate(next, Stage.CancellingHotel, ctx)
                }
            }
            Stage.VoidingFlight -> {
                val next = state.copy(flight = undo(state.flight, completion.outcomeOf(FlightVoid)!!))
                when {
                    next.flight.undone == Tri.Yes || state.attempts >= maxCompensations -> settle(next)
                    else -> compensate(next, Stage.VoidingFlight, ctx)
                }
            }
            Stage.New, Stage.Settled -> step(state)
        }
    }

    private fun undo(part: Part, outcome: Outcome<Unit>): Part = part.copy(undone = part.undone or outcome.effect())

    /** Starts (or repeats) a compensation stage; a booking with no possible effect needs none. */
    private fun compensate(state: TripState, stage: Stage, ctx: Context): Step<TripState, TripReply> {
        val part = if (stage == Stage.VoidingFlight) state.flight else state.hotel
        if (part.net == Tri.No) return if (stage == Stage.CancellingHotel) compensate(state, Stage.VoidingFlight, ctx) else settle(state)
        val attempts = if (stage == state.stage) state.attempts + 1 else 1
        val port = if (stage == Stage.VoidingFlight) FlightVoid else HotelCancel
        return call(state.copy(stage = stage, attempts = attempts), port, ctx)
    }

    private fun call(state: TripState, port: AdapterPort<TripRef, *>, ctx: Context): Step<TripState, TripReply> {
        val key = ctx.newKey()
        return step(state.copy(call = key), Call(key, port, TripRef(ctx.self.key), timeoutMillis))
    }

    private fun settle(state: TripState) = step<TripState, TripReply>(state.copy(stage = Stage.Settled, call = null, attempts = 0))

    private fun status(state: TripState) = TripReply.Status(state.stage, state.knowledge)
}
