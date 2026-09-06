"""Executable teaching evidence, not a reusable runtime or conformance implementation.

One trusted owning thread; immediate calls; transient immutable state; no queues,
retries, persistence, detached operations, or external side effects. Every source
click has one command and one terminal source observation. Python capability views
limit the ordinary API; they do not contain hostile reflection or arbitrary code.
"""
from dataclasses import dataclass, replace
from enum import Enum, auto
from threading import get_ident
from typing import Any, Callable, Tuple, Union


@dataclass(frozen=True)
class NotAccepted:
    reason: str


@dataclass(frozen=True)
class Accepted:
    state: Any
    result: Any = None
    outputs: Tuple[Any, ...] = ()


class SerialOwner:
    """Shared acceptance mechanism for these finite teaching decisions."""

    def __init__(self, state, decide, execute=None):
        self._frame = Accepted(state)
        self._failed_frame = None
        self._decide, self._execute = decide, execute
        self._thread, self._deciding = get_ident(), False

    def _check(self):
        if get_ident() != self._thread or self._deciding:
            raise RuntimeError("outside the owner's serialized entry point")

    def read(self):
        self._check()
        return self._frame.state

    def handle(self, pulse):
        self._check()
        if self._failed_frame is not None:
            raise RuntimeError("accepted output fault requires supervision")
        self._deciding = True
        try:
            candidate = self._decide(self._frame.state, pulse)
        finally:
            self._deciding = False
        if isinstance(candidate, NotAccepted):
            return candidate
        if candidate.outputs and self._execute is None:
            raise RuntimeError("no executor: candidate was not accepted")
        self._frame = candidate  # Publish state and all outputs together.
        # This call retains candidate while every output is handled. No queue.
        try:
            for output in candidate.outputs:
                self._execute(output)  # Outside decide.
        except Exception:
            self._failed_frame = candidate  # Retain obligations; stop new mutation.
            raise
        return candidate.result


@dataclass(frozen=True)
class Increment:
    pass


@dataclass(frozen=True)
class Changed:
    value: int


IncrementResult = Union[Changed, NotAccepted]


def counter_decide(value: int, pulse: Increment):
    if not isinstance(pulse, Increment):
        raise TypeError("expected Increment")
    if value >= 3:
        return NotAccepted("LimitReached")
    return Accepted(value + 1, Changed(value + 1))


@dataclass(frozen=True)
class CounterRead:
    value: Callable[[], int]


@dataclass(frozen=True)
class CounterCommands:
    increment: Callable[[], IncrementResult]


class Phase(Enum):
    IDLE = auto()
    WAITING = auto()
    CHANGED = auto()
    NOT_ACCEPTED = auto()
    EXECUTOR_FAILED = auto()


@dataclass(frozen=True)
class SourceState:
    phase: Phase = Phase.IDLE
    displayed: int = 0


@dataclass(frozen=True)
class Click:
    pass


@dataclass(frozen=True)
class Returned:
    result: IncrementResult


@dataclass(frozen=True)
class ExecutionFailed:
    pass


class ExecutorFailure(Exception):
    """Declared local executor failure; never a target nonacceptance result."""


def source_decide(state: SourceState, pulse):
    if isinstance(pulse, Click):
        return Accepted(replace(state, phase=Phase.WAITING), outputs=(Increment(),))
    if isinstance(pulse, Returned):
        if isinstance(pulse.result, Changed):
            return Accepted(SourceState(Phase.CHANGED, pulse.result.value))
        return Accepted(replace(state, phase=Phase.NOT_ACCEPTED))
    if isinstance(pulse, ExecutionFailed):
        return Accepted(replace(state, phase=Phase.EXECUTOR_FAILED))
    raise TypeError("unexpected source input")


def make_source(commands: CounterCommands):
    def execute(output):
        if not isinstance(output, Increment):
            raise TypeError("unexpected accepted source output")
        try:
            result = commands.increment()
        except ExecutorFailure:
            source.handle(ExecutionFailed())
        else:
            source.handle(Returned(result))

    source = SerialOwner(SourceState(), source_decide, execute)
    return source


@dataclass(frozen=True)
class Assembly:
    reader: CounterRead
    commands: CounterCommands
    source: SerialOwner


def assemble(decide=counter_decide):
    counter = SerialOwner(0, decide)
    reader = CounterRead(counter.read)
    commands = CounterCommands(lambda: counter.handle(Increment()))
    return Assembly(reader, commands, make_source(commands))


@dataclass(frozen=True)
class Metadata:
    title: str


@dataclass(frozen=True)
class DocumentState:
    metadata: Metadata
    paragraphs: Tuple[str, ...]


@dataclass(frozen=True)
class RenameTitle:
    title: str


@dataclass(frozen=True)
class AppendText:
    text: str


def document_decide(state: DocumentState, pulse):
    if isinstance(pulse, RenameTitle):
        if not 1 <= len(pulse.title) <= 40:
            return NotAccepted("TitleLength")
        return Accepted(replace(state, metadata=Metadata(pulse.title)))
    if isinstance(pulse, AppendText):
        builder = list(state.paragraphs)  # Candidate-private mutable builder.
        builder.append(pulse.text)
        if len(builder) > 3 or any(len(part) > 80 for part in builder):
            return NotAccepted("DocumentSize")
        return Accepted(replace(state, paragraphs=tuple(builder)))
    raise TypeError("unexpected document input")
