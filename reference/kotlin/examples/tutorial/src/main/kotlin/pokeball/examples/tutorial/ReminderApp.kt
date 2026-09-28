package pokeball.examples.tutorial

import pokeball.runtime.Adapter
import pokeball.runtime.Composition

/** The composition root: which Balls exist and which adapter performs each port. */
fun reminderComposition(mailer: Adapter<Email, Unit>): Composition =
    Composition()
        .ball(ReminderBall)
        .adapter(Mailer, mailer)
