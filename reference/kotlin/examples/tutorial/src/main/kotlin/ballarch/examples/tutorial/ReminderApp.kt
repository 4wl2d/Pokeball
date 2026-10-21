package ballarch.examples.tutorial

import ballarch.runtime.Adapter
import ballarch.runtime.Composition

/** The composition root: which Balls exist and which adapter performs each port. */
fun reminderComposition(mailer: Adapter<Email, Unit>): Composition =
    Composition()
        .ball(ReminderBall)
        .adapter(Mailer, mailer)
