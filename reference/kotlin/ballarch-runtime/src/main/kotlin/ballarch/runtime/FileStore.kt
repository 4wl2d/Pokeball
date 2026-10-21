package ballarch.runtime

import ballarch.kernel.BallId
import ballarch.kernel.CallId
import ballarch.kernel.CallKey
import ballarch.kernel.NotDoneReason
import ballarch.kernel.Outcome
import ballarch.kernel.RequestId
import ballarch.kernel.UnknownReason
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.zip.CRC32

/**
 * Turns an [InstanceRecord] into bytes and back. The runtime's own types are
 * encoded by the store; application values (state, messages, replies, notice
 * payloads) are encoded by the codec.
 */
public interface RecordCodec {
    public fun encode(record: InstanceRecord): ByteArray

    public fun decode(bytes: ByteArray): InstanceRecord
}

/**
 * A durable [Store] on the local file system: one file per instance.
 *
 * [save] is the commit point (rule R4). It writes the encoded record to a
 * temporary file in the same directory and forces it to the device. It then
 * renames the file over the previous one with an atomic move, and forces the
 * directory. After a crash of the process or of the machine, each file holds
 * either the previous or the new record, never a mixture. A CRC-32 over the
 * contents detects media corruption on load.
 *
 * Durability across power loss relies on the operating system and the device
 * honouring `fsync`; the tests cover crashes of the process (a JVM killed
 * mid-write), not power loss. A directory belongs to one runtime at a time.
 */
public class FileStore(
    private val directory: Path,
    private val codec: RecordCodec = JavaSerializationCodec,
) : Store {
    init {
        Files.createDirectories(directory)
        // Temporary files left by a crash were never renamed, so they were never committed.
        Files.list(directory).use { files -> files.filter { it.fileName.toString().startsWith(TEMPORARY) }.toList() }
            .forEach { Files.deleteIfExists(it) }
    }

    private fun fileOf(id: BallId): Path = directory.resolve(nameOf(id) + EXTENSION)

    override fun load(id: BallId): InstanceRecord? {
        val file = fileOf(id)
        if (!Files.exists(file)) return null
        return codec.decode(unframe(Files.readAllBytes(file), file))
    }

    override fun save(record: InstanceRecord) {
        writeAtomically(fileOf(record.id), frame(codec.encode(record)))
    }

    override fun ids(): Set<BallId> = Files.list(directory).use { files ->
        files.filter { it.fileName.toString().endsWith(EXTENSION) }
            .map { codec.decode(unframe(Files.readAllBytes(it), it)).id }
            .toList()
            .toSet()
    }

    @Synchronized
    override fun nextIncarnation(): Long {
        val file = directory.resolve(INCARNATION)
        val next = (if (Files.exists(file)) String(unframe(Files.readAllBytes(file), file)).toLong() else 0L) + 1
        writeAtomically(file, frame(next.toString().toByteArray()))
        return next
    }

    private fun writeAtomically(target: Path, bytes: ByteArray) {
        val temporary = Files.createTempFile(directory, TEMPORARY, ".part")
        try {
            FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                val buffer = ByteBuffer.wrap(bytes)
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
        forceDirectory()
    }

    /** Makes the rename itself durable. Some platforms cannot open a directory; there the rename is left to the file system. */
    private fun forceDirectory() {
        try {
            FileChannel.open(directory, StandardOpenOption.READ).use { it.force(true) }
        } catch (e: IOException) {
            // not supported on this platform
        }
    }

    private companion object {
        const val EXTENSION = ".record"
        const val INCARNATION = "incarnation"
        const val TEMPORARY = ".tmp-"
        const val MAGIC = 0x42414C4C // "BALL"

        /** File names are digests, so any type or key is safe on any file system. */
        fun nameOf(id: BallId): String {
            val digest = MessageDigest.getInstance("SHA-256").digest((id.type + "\u0000" + id.key).toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun frame(payload: ByteArray): ByteArray {
            val crc = CRC32().apply { update(payload) }.value
            val out = ByteArrayOutputStream(payload.size + 16)
            DataOutputStream(out).use { data ->
                data.writeInt(MAGIC)
                data.writeInt(payload.size)
                data.writeLong(crc)
                data.write(payload)
            }
            return out.toByteArray()
        }

        fun unframe(bytes: ByteArray, file: Path): ByteArray {
            DataInputStream(ByteArrayInputStream(bytes)).use { data ->
                if (bytes.size < 16 || data.readInt() != MAGIC) throw IOException("$file is not a Ballarch record")
                val size = data.readInt()
                val crc = data.readLong()
                if (size != bytes.size - 16) throw IOException("$file is truncated")
                val payload = ByteArray(size).also { data.readFully(it) }
                if (CRC32().apply { update(payload) }.value != crc) throw IOException("$file failed its checksum")
                return payload
            }
        }
    }
}

/**
 * Encodes records with Java serialization. Application values (state,
 * messages, replies and notice payloads) must be [Serializable]; Kotlin data
 * classes and objects only need to declare it. Kernel values inside records
 * are converted by the codec, so the kernel stays free of `java.io`.
 */
public object JavaSerializationCodec : RecordCodec {
    override fun encode(record: InstanceRecord): ByteArray {
        val out = ByteArrayOutputStream()
        ObjectOutputStream(out).use { it.writeObject(record.toDto()) }
        return out.toByteArray()
    }

    override fun decode(bytes: ByteArray): InstanceRecord =
        ObjectInputStream(ByteArrayInputStream(bytes)).use { (it.readObject() as RecordDto).toRecord() }
}

// ---- serializable mirrors of the runtime's records

private class RecordDto(
    val type: String,
    val key: String,
    val state: Any?,
    val revision: Long,
    val calls: List<CallDto>,
    val inbound: List<InboundDto>,
    val outbox: List<NoticeDto>,
    val nextNoticeSequence: Long,
    val lastSeen: List<SeenDto>,
) : Serializable

private class CallDto(
    val key: String,
    val portName: String,
    val target: String?,
    val request: Any?,
    val deadlineAt: Long,
    val phase: String,
    val attempts: Int,
    val cancelRequested: Boolean,
) : Serializable

private class InboundDto(
    val requestId: String,
    val external: Boolean,
    val callerType: String?,
    val callerKey: String?,
    val callKey: String?,
    val message: Any?,
    val deadlineAt: Long,
    val reply: OutcomeDto?,
    val repliedAt: Long?,
) : Serializable

private class OutcomeDto(val kind: String, val value: Any?, val reason: String?, val detail: String?) : Serializable

private class NoticeDto(val sequence: Long, val topic: String, val payload: Any?, val subscribers: List<IdDto>) : Serializable

private class IdDto(val type: String, val key: String) : Serializable

private class SeenDto(val type: String, val key: String, val sequence: Long) : Serializable

private fun InstanceRecord.toDto() = RecordDto(
    type = id.type,
    key = id.key,
    state = state,
    revision = revision,
    calls = calls.values.map { CallDto(it.key.value, it.portName, it.target, it.request, it.deadlineAt, it.phase.name, it.attempts, it.cancelRequested) },
    inbound = inbound.values.map { r ->
        val call = (r.replyTo as? ReplyTarget.ToCall)?.callId
        InboundDto(r.requestId.value, call == null, call?.caller?.type, call?.caller?.key, call?.key?.value, r.message, r.deadlineAt, r.reply?.toDto(), r.repliedAt)
    },
    outbox = outbox.map { n -> NoticeDto(n.sequence, n.topic, n.payload, n.pendingSubscribers.map { IdDto(it.type, it.key) }) },
    nextNoticeSequence = nextNoticeSequence,
    lastSeen = lastSeen.map { (source, sequence) -> SeenDto(source.type, source.key, sequence) },
)

private fun RecordDto.toRecord() = InstanceRecord(
    id = BallId(type, key),
    state = state,
    revision = revision,
    calls = calls.associate { c ->
        CallKey(c.key) to CallRecord(CallKey(c.key), c.portName, c.target, c.request, c.deadlineAt, Phase.valueOf(c.phase), c.attempts, c.cancelRequested)
    },
    inbound = inbound.associate { r ->
        val replyTo = if (r.external) ReplyTarget.External else ReplyTarget.ToCall(CallId(BallId(r.callerType!!, r.callerKey!!), CallKey(r.callKey!!)))
        RequestId(r.requestId) to InboundRecord(RequestId(r.requestId), replyTo, r.message, r.deadlineAt, r.reply?.toOutcome(), r.repliedAt)
    },
    outbox = outbox.map { n -> NoticeRecord(n.sequence, n.topic, n.payload, n.subscribers.mapTo(LinkedHashSet()) { BallId(it.type, it.key) }) },
    nextNoticeSequence = nextNoticeSequence,
    lastSeen = lastSeen.associate { BallId(it.type, it.key) to it.sequence },
)

private fun Outcome<Any?>.toDto(): OutcomeDto = when (this) {
    is Outcome.Done -> OutcomeDto("Done", value, null, null)
    is Outcome.Unknown -> OutcomeDto("Unknown", null, reason.name, null)
    is Outcome.NotDone -> when (val r = reason) {
        is NotDoneReason.Refused -> OutcomeDto("NotDone", r.reply, "Refused", null)
        NotDoneReason.Overloaded -> OutcomeDto("NotDone", null, "Overloaded", null)
        NotDoneReason.Cancelled -> OutcomeDto("NotDone", null, "Cancelled", null)
        NotDoneReason.DeadlineBeforeSend -> OutcomeDto("NotDone", null, "DeadlineBeforeSend", null)
        NotDoneReason.NoAnswer -> OutcomeDto("NotDone", null, "NoAnswer", null)
        is NotDoneReason.NotApplied -> OutcomeDto("NotDone", null, "NotApplied", r.evidence)
        NotDoneReason.IdempotencyConflict -> OutcomeDto("NotDone", null, "IdempotencyConflict", null)
        is NotDoneReason.Fault -> OutcomeDto("NotDone", null, "Fault", r.message)
    }
}

private fun OutcomeDto.toOutcome(): Outcome<Any?> = when (kind) {
    "Done" -> Outcome.Done(value)
    "Unknown" -> Outcome.Unknown(UnknownReason.valueOf(reason!!))
    else -> Outcome.NotDone(
        when (reason) {
            "Refused" -> NotDoneReason.Refused(value)
            "Overloaded" -> NotDoneReason.Overloaded
            "Cancelled" -> NotDoneReason.Cancelled
            "DeadlineBeforeSend" -> NotDoneReason.DeadlineBeforeSend
            "NoAnswer" -> NotDoneReason.NoAnswer
            "NotApplied" -> NotDoneReason.NotApplied(detail!!)
            "IdempotencyConflict" -> NotDoneReason.IdempotencyConflict
            "Fault" -> NotDoneReason.Fault(detail!!)
            else -> throw IOException("unknown outcome reason $reason")
        },
    )
}
