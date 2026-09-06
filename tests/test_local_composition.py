"""Behavior checks for the bounded teaching fixture; no source-text assertions."""
import unittest
from concurrent.futures import ThreadPoolExecutor
from dataclasses import FrozenInstanceError

from examples.local_composition import (
    Accepted, AppendText, Changed, Click, CounterCommands, DocumentState,
    ExecutorFailure, Increment, Metadata, NotAccepted, Phase, RenameTitle,
    SerialOwner, assemble, counter_decide, document_decide, make_source,
)


class LocalCompositionTests(unittest.TestCase):
    def assert_counter_contract(self, decide):
        app = assemble(decide)
        for value in range(1, 4):
            app.source.handle(Click())
            self.assertEqual(app.reader.value(), value)
            self.assertEqual(app.source.read().phase, Phase.CHANGED)
            self.assertEqual(app.source.read().displayed, value)
        app.source.handle(Click())
        self.assertEqual(app.reader.value(), 3)
        self.assertEqual(app.source.read().phase, Phase.NOT_ACCEPTED)
        self.assertEqual(app.source.read().displayed, 3)
        self.assertEqual(app.commands.increment(), NotAccepted("LimitReached"))

    def test_counter_success_and_preacceptance_refusal(self):
        self.assert_counter_contract(counter_decide)

    def test_command_executes_after_source_acceptance_and_return_is_serialized(self):
        app = assemble()
        observations = []

        def increment():
            observations.append(source.read().phase)
            result = app.commands.increment()
            observations.append(app.reader.value())
            return result

        source = make_source(CounterCommands(increment))
        source.handle(Click())
        self.assertEqual(observations, [Phase.WAITING, 1])
        self.assertEqual(source.read().phase, Phase.CHANGED)
        self.assertEqual(source.read().displayed, 1)

    def test_executor_failure_after_target_acceptance_is_not_nonacceptance(self):
        app = assemble()

        def fail_after_increment():
            self.assertEqual(app.commands.increment(), Changed(1))
            raise ExecutorFailure("failure while handing back the accepted result")

        source = make_source(CounterCommands(fail_after_increment))
        source.handle(Click())
        self.assertEqual(app.reader.value(), 1)  # Target acceptance remains.
        self.assertEqual(source.read().phase, Phase.EXECUTOR_FAILED)
        self.assertNotEqual(source.read().phase, Phase.NOT_ACCEPTED)

    def test_extra_consumer_needs_only_wiring(self):
        app = assemble()
        additional_consumer = make_source(app.commands)
        app.source.handle(Click())
        additional_consumer.handle(Click())
        self.assertEqual(app.reader.value(), 2)
        self.assertEqual(app.source.read().displayed, 1)
        self.assertEqual(additional_consumer.read().displayed, 2)
        self.assertFalse(hasattr(app.reader, "increment"))
        self.assertFalse(hasattr(app.commands, "value"))

    def test_small_ball_reuses_binding(self):
        def toggle(enabled, pulse):
            if pulse != "Toggle":
                raise TypeError("expected Toggle")
            return Accepted(not enabled)

        indicator = SerialOwner(False, toggle)
        indicator.handle("Toggle")
        self.assertTrue(indicator.read())
        indicator.handle("Toggle")
        self.assertFalse(indicator.read())

    def test_renaming_and_extracting_limit_check_preserve_behavior(self):
        def at_limit(current):
            return current >= 3

        def refactored(current, pulse):
            if not isinstance(pulse, Increment):
                raise TypeError("expected Increment")
            if at_limit(current):
                return NotAccepted("LimitReached")
            increased = current + 1
            return Accepted(increased, Changed(increased))

        self.assert_counter_contract(refactored)
        for current in range(4):
            self.assertEqual(counter_decide(current, Increment()),
                             refactored(current, Increment()))

    def test_shared_binding_rejects_wrong_thread(self):
        app = assemble()
        with ThreadPoolExecutor(max_workers=1) as pool:
            with self.assertRaises(RuntimeError):
                pool.submit(app.commands.increment).result()
        self.assertEqual(app.reader.value(), 0)

    def test_shared_binding_blocks_reentrant_decide(self):
        def reenter(state, pulse):
            owner.handle(pulse)
            return Accepted(state + 1)

        owner = SerialOwner(0, reenter)
        with self.assertRaises(RuntimeError):
            owner.handle("Go")
        self.assertEqual(owner.read(), 0)

    def test_fault_before_publication_does_not_accept_or_dispatch(self):
        def fail(state, pulse):
            raise ValueError("fault while building a candidate")

        dispatched = []
        owner = SerialOwner(0, fail, dispatched.append)
        with self.assertRaises(ValueError):
            owner.handle("Go")
        self.assertEqual(owner.read(), 0)
        self.assertEqual(dispatched, [])

    def test_fault_after_publication_preserves_complete_accepted_frame(self):
        def fail(output):
            raise ExecutorFailure("declared executor fault")

        owner = SerialOwner(0, lambda state, pulse: Accepted(1, outputs=("Notify",)), fail)
        with self.assertRaises(ExecutorFailure):
            owner.handle("Go")
        self.assertEqual(owner.read(), 1)
        self.assertEqual(owner._failed_frame.outputs, ("Notify",))
        with self.assertRaises(RuntimeError):
            owner.handle("Another input")
        self.assertEqual(owner.read(), 1)


class DocumentIsolationTests(unittest.TestCase):
    def setUp(self):
        self.original = DocumentState(Metadata("Draft"), ("One",))
        self.document = SerialOwner(self.original, document_decide)

    def test_title_change_shares_immutable_paragraphs(self):
        self.document.handle(RenameTitle("Published"))
        current = self.document.read()
        self.assertEqual(current.metadata.title, "Published")
        self.assertEqual(self.original.metadata.title, "Draft")
        self.assertIs(current.paragraphs, self.original.paragraphs)
        with self.assertRaises(FrozenInstanceError):
            current.metadata.title = "Escaped write"

    def test_builder_changes_do_not_mutate_published_document(self):
        candidate = document_decide(self.original, AppendText("Two"))
        self.assertEqual(candidate.state.paragraphs, ("One", "Two"))
        self.assertEqual(self.document.read(), self.original)
        self.document.handle(AppendText("Two"))
        self.assertEqual(self.original.paragraphs, ("One",))
        self.assertEqual(self.document.read().paragraphs, ("One", "Two"))

    def test_rejection_preserves_previously_published_state(self):
        self.document.handle(AppendText("Two"))
        self.document.handle(AppendText("Three"))
        published = self.document.read()
        self.assertEqual(self.document.handle(AppendText("Four")),
                         NotAccepted("DocumentSize"))
        self.assertIs(self.document.read(), published)
        self.assertEqual(self.document.handle(RenameTitle("")),
                         NotAccepted("TitleLength"))
        self.assertIs(self.document.read(), published)
        self.assertEqual(self.original, DocumentState(Metadata("Draft"), ("One",)))


if __name__ == "__main__":
    unittest.main()
