package ballarch.kernel

/**
 * A truth value of Kleene's strong three-valued logic: [Yes], [No], or
 * [Maybe] when the available evidence does not decide.
 *
 * The connectives are the most informative ones that never contradict a
 * refinement: replacing a [Maybe] operand by [Yes] or [No] never turns a
 * definite result into its opposite. This monotonicity is what makes
 * [Knowledge] compose soundly.
 */
public enum class Tri {
    Yes,
    No,
    Maybe,
    ;

    public infix fun and(other: Tri): Tri = when {
        this == No || other == No -> No
        this == Yes && other == Yes -> Yes
        else -> Maybe
    }

    public infix fun or(other: Tri): Tri = when {
        this == Yes || other == Yes -> Yes
        this == No && other == No -> No
        else -> Maybe
    }

    public operator fun not(): Tri = when (this) {
        Yes -> No
        No -> Yes
        Maybe -> Maybe
    }

    /** Whether [actual] is consistent with this value. [Maybe] admits both. */
    public fun admits(actual: Boolean): Boolean = this == Maybe || (this == Yes) == actual

    public companion object {
        public fun of(value: Boolean): Tri = if (value) Yes else No
    }
}

/**
 * What is known about the effects of an operation made of constituent
 * effects: the calls a Ball made for it, or nested composite operations.
 *
 * - [all]: did every constituent take effect (net of compensation)?
 * - [any]: did at least one?
 *
 * The knowledge of one call follows from its [Outcome]: `Done` is
 * [Knowledge.Done], `NotDone` is [Knowledge.NotDone], `Unknown` is
 * [Knowledge.Unknown]. Constituents combine with [plus]; [NoEffects], the
 * knowledge of an operation with no constituents, is its identity.
 *
 * **Composition closure.** If every constituent's knowledge is honest (it
 * admits what really happened), the combined knowledge is honest too, and each
 * of its two coordinates is the most informative honest value. A Ball that
 * derives every claim about a composite operation from this algebra (rule R14)
 * can therefore be used, one level up, as a single port with honest outcomes.
 * The kernel tests check both properties exhaustively for up to four
 * constituents; associativity extends them to any number.
 */
public class Knowledge private constructor(public val all: Tri, public val any: Tri) {
    /** Combine with an independent constituent (or composite) of the same operation. */
    public operator fun plus(other: Knowledge): Knowledge = of(all and other.all, any or other.any)

    /** Every constituent took effect. */
    public val isDone: Boolean get() = all == Tri.Yes && any == Tri.Yes

    /** No constituent took effect. Also true for [NoEffects]. */
    public val isNotDone: Boolean get() = any == Tri.No

    /** Some constituents took effect and others did not: a definite partial effect. */
    public val isPartial: Boolean get() = all == Tri.No && any == Tri.Yes

    /** Neither "all" nor "none" is established. */
    public val isUncertain: Boolean get() = all == Tri.Maybe || any == Tri.Maybe

    /** Whether a ground truth (every constituent applied / at least one applied) is consistent with this knowledge. */
    public fun admits(allApplied: Boolean, anyApplied: Boolean): Boolean = all.admits(allApplied) && any.admits(anyApplied)

    /**
     * The honest three-valued outcome of this composite seen as one call:
     * `Done` only if every constituent took effect, `NotDone` only if none did,
     * otherwise `Unknown`. A partial effect has no three-valued description, so a
     * composite that can end [isPartial] should report this knowledge itself.
     */
    public fun <R> toOutcome(value: R, notDone: NotDoneReason<R>, unknown: UnknownReason): Outcome<R> = when {
        isDone -> Outcome.Done(value)
        isNotDone -> Outcome.NotDone(notDone)
        else -> Outcome.Unknown(unknown)
    }

    override fun equals(other: Any?): Boolean = other is Knowledge && other.all == all && other.any == any

    override fun hashCode(): Int = all.hashCode() * 31 + any.hashCode()

    override fun toString(): String = name

    /** The name of this value: one of the seven constants below. */
    public val name: String
        get() = when (this) {
            NoEffects -> "NoEffects"
            Done -> "Done"
            NotDone -> "NotDone"
            Partial -> "Partial"
            NotAllDone -> "NotAllDone"
            SomeDone -> "SomeDone"
            else -> "Unknown"
        }

    public companion object {
        /** No constituents (all vacuously true, any false): the identity of [plus]. */
        public val NoEffects: Knowledge = Knowledge(Tri.Yes, Tri.No)

        /** Every constituent took effect. */
        public val Done: Knowledge = Knowledge(Tri.Yes, Tri.Yes)

        /** No constituent took effect. */
        public val NotDone: Knowledge = Knowledge(Tri.No, Tri.No)

        /** Some constituents took effect, and some did not. */
        public val Partial: Knowledge = Knowledge(Tri.No, Tri.Yes)

        /** Not every constituent took effect; whether any did is unknown. */
        public val NotAllDone: Knowledge = Knowledge(Tri.No, Tri.Maybe)

        /** At least one constituent took effect; whether all did is unknown. */
        public val SomeDone: Knowledge = Knowledge(Tri.Maybe, Tri.Yes)

        /** Nothing is established. */
        public val Unknown: Knowledge = Knowledge(Tri.Maybe, Tri.Maybe)

        /** The seven values the algebra can produce. */
        public val values: List<Knowledge> = listOf(NoEffects, Done, NotDone, Partial, NotAllDone, SomeDone, Unknown)

        /**
         * The knowledge with the given coordinates. Pairs that no operation can
         * produce, (Yes, Maybe) and (Maybe, No), are rejected.
         */
        public fun of(all: Tri, any: Tri): Knowledge {
            require(!(all == Tri.Yes && any == Tri.Maybe) && !(all == Tri.Maybe && any == Tri.No)) {
                "no operation has knowledge (all=$all, any=$any)"
            }
            return when (all) {
                Tri.Yes -> if (any == Tri.No) NoEffects else Done
                Tri.No -> when (any) {
                    Tri.No -> NotDone
                    Tri.Yes -> Partial
                    Tri.Maybe -> NotAllDone
                }
                Tri.Maybe -> if (any == Tri.Yes) SomeDone else Unknown
            }
        }

        /** The knowledge of one constituent whose net effect is [applied]. */
        public fun single(applied: Tri): Knowledge = of(applied, applied)
    }
}

/**
 * Whether the effect this call requested took place: `NotDone` is [Tri.No],
 * `Unknown` is [Tri.Maybe], and `Done(value)` is [effective] of the value.
 *
 * `Done` means that the destination processed the request; its value may
 * still describe a business refusal (a declined charge). Pass [effective] to
 * say which answers mean the requested effect happened.
 */
public fun <R> Outcome<R>.effect(effective: (R) -> Boolean = { true }): Tri = when (this) {
    is Outcome.Done -> Tri.of(effective(value))
    is Outcome.NotDone -> Tri.No
    is Outcome.Unknown -> Tri.Maybe
}

/** What this outcome establishes about the effect of its call (see [effect]). */
public fun <R> Outcome<R>.knowledge(effective: (R) -> Boolean = { true }): Knowledge = Knowledge.single(effect(effective))

/**
 * The net effect of this call after a compensating call whose outcome is [undo]
 * (or of the call alone when [undo] is `null`).
 *
 * Only an *absorbing* compensation may be used here: its effect must mean that
 * no effect of the original request exists or can still take effect, for
 * example because the destination fences the request's reference, or because
 * the original request's delivery horizon has passed. Then the net effect is
 * `effect ∧ ¬undone`, which turns an `Unknown` call into a definite `NotDone`
 * once its compensation took effect.
 */
public fun <R, U> Outcome<R>.compensatedBy(
    undo: Outcome<U>?,
    effective: (R) -> Boolean = { true },
    undone: (U) -> Boolean = { true },
): Knowledge = Knowledge.single(if (undo == null) effect(effective) else effect(effective) and !undo.effect(undone))

/** The combined knowledge of independent constituents. */
public fun Iterable<Knowledge>.combine(): Knowledge = fold(Knowledge.NoEffects, Knowledge::plus)
