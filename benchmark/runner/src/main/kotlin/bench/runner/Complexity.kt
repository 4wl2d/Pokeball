package bench.runner

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * The class files of one implementation, indexed so that a hand-written
 * function can be followed into the code the Kotlin compiler split off from it.
 */
class ClassModel(val nodes: List<ClassNode>) {
    private val enclosed: Map<String, List<ClassNode>> =
        nodes.filter { it.outerMethod != null }.groupBy { "${it.outerClass}.${it.outerMethod}${it.outerMethodDesc}" }

    /**
     * [method] of [owner] together with the code compiled out of it, recursively:
     * its lambda bodies and local functions (methods whose name contains `$`
     * that it calls or references, of its own class or, from a nested class, of
     * [owner]), and every method of the lambda, coroutine and anonymous classes
     * it encloses. With [helpers], also the `$default` and `access$` helpers it
     * calls, whose field reads belong to the call in the source: a data class's
     * `copy` reads every field, and a coroutine reads a private field of
     * [owner] through `access$getX$p`.
     */
    fun parts(owner: ClassNode, method: MethodNode, helpers: Boolean): List<Pair<ClassNode, MethodNode>> {
        val out = ArrayList<Pair<ClassNode, MethodNode>>()
        val seen = HashSet<MethodNode>()
        // A method of the class being visited or of [owner]; nothing else is part of [method].
        fun method(c: ClassNode, targetOwner: String, name: String, desc: String): Pair<ClassNode, MethodNode>? {
            val target = if (targetOwner == c.name) c else if (targetOwner == owner.name) owner else return null
            return target.methods.firstOrNull { it.name == name && it.desc == desc }?.let { target to it }
        }
        fun visit(c: ClassNode, m: MethodNode) {
            if (!seen.add(m)) return
            out += c to m
            for (insn in m.instructions) {
                when (insn) {
                    is MethodInsnNode -> if ('$' in insn.name && (helpers || !isHelper(insn.name))) {
                        method(c, insn.owner, insn.name, insn.desc)?.let { visit(it.first, it.second) }
                    }
                    is InvokeDynamicInsnNode -> for (h in insn.bsmArgs.filterIsInstance<Handle>()) {
                        if ('$' in h.name) method(c, h.owner, h.name, h.desc)?.let { visit(it.first, it.second) }
                    }
                }
            }
            for (inner in enclosed["${c.name}.${m.name}${m.desc}"].orEmpty()) for (im in inner.methods) visit(inner, im)
        }
        visit(owner, method)
        return out
    }

    private fun isHelper(name: String) = name.endsWith("\$default") || name.startsWith("access\$")
}

/** A hand-written function and its cyclomatic complexity. */
data class FunctionComplexity(val name: String, val ccn: Int)

/**
 * The hand-written functions of an implementation: the methods that the
 * cohesion metrics count ([countedMethods]) in named classes, companion objects
 * and interface default implementations. Constructors and compiler-generated
 * members are not functions.
 */
fun functions(model: ClassModel): List<Pair<ClassNode, MethodNode>> =
    model.nodes.filter { unitOf(it.name) == it.name || it.name.substringAfterLast('$') in setOf("Companion", "DefaultImpls") }
        .flatMap { c -> countedMethods(c).map { c to it } }

/**
 * Cyclomatic complexity per hand-written function, as JaCoCo defines it for
 * bytecode: v(G) = B − D + 1, that is 1 plus, for every instruction with more
 * than one successor, the number of its distinct successors minus 1. A
 * conditional jump adds 1, a switch adds its distinct targets minus 1, and an
 * exception handler adds nothing. The decisions in the function's lambdas,
 * local functions, and coroutine and anonymous classes count toward it;
 * branches the compiler generates are not counted ([decisions]).
 */
fun complexity(model: ClassModel): List<FunctionComplexity> = functions(model).map { (c, m) ->
    FunctionComplexity(functionName(c, m), 1 + model.parts(c, m, helpers = false).sumOf { (pc, pm) -> decisions(pc, pm) })
}

/** `pkg.Class.name(Param, ...)`, without Kotlin's value-class mangling and the coroutine continuation. */
fun functionName(c: ClassNode, m: MethodNode): String {
    val params = Type.getArgumentTypes(m.desc).filter { it.descriptor != CONTINUATION }
        .joinToString(", ") { it.className.substringAfterLast('.').substringAfterLast('$') }
    return "${c.name.replace('/', '.')}.${baseName(m.name)}($params)"
}

private const val CONTINUATION = "Lkotlin/coroutines/Continuation;"
private const val INTRINSICS = "kotlin/jvm/internal/Intrinsics"
private const val COROUTINE_INTRINSICS = "kotlin/coroutines/intrinsics/IntrinsicsKt"
private const val NO_WHEN_BRANCH = "kotlin/NoWhenBranchMatchedException"

/**
 * Decision points of one bytecode method: the sum, over its instructions with
 * more than one successor, of the distinct successors minus 1. Not counted, as
 * in JaCoCo 0.8.15's Kotlin filters:
 * - code inlined from other files (lines beyond the source file in the SMAP);
 * - the coroutine state machine: the continuation checks before the state
 *   switch, the state switch, and the checks for `COROUTINE_SUSPENDED`;
 * - the checks for an uninitialised `lateinit` property, for a null value cast
 *   to a non-null type, and the `!!` check of older compilers;
 * - the branch that an exhaustive `when` takes to `NoWhenBranchMatchedException`,
 *   and the null check of a `when` over a nullable enum.
 * One deviation from JaCoCo 0.8.15: JaCoCo removes the `NoWhenBranchMatchedException`
 * branch only when it is the target of an `IFEQ` or of a switch's default; this
 * count also removes it when another conditional jump leads to it (the `IFNONNULL`
 * of a last case `null`) or when the last case's check falls through into it, as
 * the compiler emits for a last case that compares with an `object`.
 */
fun decisions(owner: ClassNode, method: MethodNode): Int {
    val insns = method.instructions.toArray()
    if (insns.isEmpty()) return 0
    val ignored = HashSet<AbstractInsnNode>()
    val ignoredTargets = HashMap<AbstractInsnNode, AbstractInsnNode>() // switch -> default target not counted
    coroutineMachinery(insns, ignored)
    for ((i, insn) in insns.withIndex()) {
        when (insn.opcode) {
            Opcodes.IFNONNULL -> {
                val after = opcodesFrom(insns, i + 1, 4)
                if (isUninitialisedCheck(after) || isNullCastCheck(after) || isNpeCheck(after)) ignored += insn
                if (isNullableEnumWhen(insn as JumpInsnNode, after)) ignored += insn
            }
            Opcodes.IFNULL -> if (isUninitialisedCheck(opcodesFrom(insns, insns.indexOf(real((insn as JumpInsnNode).label)), 3))) ignored += insn
            Opcodes.NEW -> if ((insn as TypeInsnNode).desc == NO_WHEN_BRANCH) {
                for (j in insns) {
                    if (j is JumpInsnNode && j.opcode != Opcodes.GOTO && real(j.label) === insn) ignored += j
                    defaultLabel(j)?.let { if (real(it) === insn) ignoredTargets[j] = insn }
                }
                val previous = previousOpcode(insns, i)
                if (previous is JumpInsnNode && previous.opcode != Opcodes.GOTO) ignored += previous
            }
        }
    }
    val firstInlined = firstInlinedLine(owner)
    var line = 0
    var sum = 0
    for (insn in insns) {
        if (insn is LineNumberNode) line = insn.line
        if (insn.opcode < 0 || insn in ignored || line >= firstInlined) continue
        val successors = when (insn) {
            is JumpInsnNode -> if (insn.opcode == Opcodes.GOTO) 1 else 2
            is TableSwitchInsnNode -> (insn.labels + insn.dflt).map { real(it) }.filter { it !== ignoredTargets[insn] }.toSet().size
            is LookupSwitchInsnNode -> (insn.labels + insn.dflt).map { real(it) }.filter { it !== ignoredTargets[insn] }.toSet().size
            else -> 1
        }
        sum += maxOf(0, successors - 1)
    }
    return sum
}

/** The first real instruction at or after [node]. */
private fun real(node: AbstractInsnNode): AbstractInsnNode? {
    var n: AbstractInsnNode? = node
    while (n != null && n.opcode < 0) n = n.next
    return n
}

private fun opcodesFrom(insns: Array<AbstractInsnNode>, from: Int, count: Int): List<AbstractInsnNode> {
    val out = ArrayList<AbstractInsnNode>()
    var i = from
    while (i in insns.indices && out.size < count) {
        if (insns[i].opcode >= 0) out += insns[i]
        i++
    }
    return out
}

private fun previousOpcode(insns: Array<AbstractInsnNode>, index: Int): AbstractInsnNode? {
    var i = index - 1
    while (i >= 0 && insns[i].opcode < 0) i--
    return if (i >= 0) insns[i] else null
}

private fun defaultLabel(i: AbstractInsnNode): LabelNode? = when (i) {
    is TableSwitchInsnNode -> i.dflt
    is LookupSwitchInsnNode -> i.dflt
    else -> null
}

private fun AbstractInsnNode.calls(owner: String, name: String) = this is MethodInsnNode && this.owner == owner && this.name == name

/** `[POP] LDC name; INVOKESTATIC Intrinsics.throwUninitializedPropertyAccessException`. */
private fun isUninitialisedCheck(next: List<AbstractInsnNode>): Boolean {
    val rest = if (next.firstOrNull()?.opcode == Opcodes.POP) next.drop(1) else next
    return rest.size >= 2 && rest[0] is LdcInsnNode && rest[1].calls(INTRINSICS, "throwUninitializedPropertyAccessException")
}

/** `[POP] NEW NullPointerException|TypeCastException; DUP; LDC "null cannot be cast to non-null type ..."`. */
private fun isNullCastCheck(next: List<AbstractInsnNode>): Boolean {
    val rest = if (next.firstOrNull()?.opcode == Opcodes.POP) next.drop(1) else next
    return rest.size >= 3 && rest[0].opcode == Opcodes.NEW &&
        (rest[0] as TypeInsnNode).desc in setOf("java/lang/NullPointerException", "kotlin/TypeCastException") &&
        ((rest[2] as? LdcInsnNode)?.cst as? String)?.startsWith("null cannot be cast to non-null type") == true
}

/** `INVOKESTATIC Intrinsics.throwNpe` (the `!!` of Kotlin before 1.4). */
private fun isNpeCheck(next: List<AbstractInsnNode>) = next.firstOrNull()?.calls(INTRINSICS, "throwNpe") == true

/** `DUP; IFNONNULL a; POP; ICONST_M1; GOTO b; a: GETSTATIC $WhenMappings...`: the null case of a `when` over a nullable enum. */
private fun isNullableEnumWhen(jump: JumpInsnNode, next: List<AbstractInsnNode>): Boolean {
    if (next.size < 3 || next[0].opcode != Opcodes.POP || next[1].opcode != Opcodes.ICONST_M1 || next[2].opcode != Opcodes.GOTO) return false
    val mapping = real(jump.label) as? FieldInsnNode ?: return false
    return mapping.opcode == Opcodes.GETSTATIC && mapping.owner.endsWith("\$WhenMappings") && mapping.name.startsWith("\$EnumSwitchMapping\$")
}

/**
 * Adds the branches of the coroutine machinery to [ignored]: everything before
 * the state switch (the continuation checks), the state switch, and each
 * comparison with `COROUTINE_SUSPENDED`, either loaded from the local that holds
 * it or fetched right before the comparison (tail calls).
 */
private fun coroutineMachinery(insns: Array<AbstractInsnNode>, ignored: MutableSet<AbstractInsnNode>) {
    val suspendedLocals = HashSet<Int>()
    for ((i, insn) in insns.withIndex()) {
        if (!insn.calls(COROUTINE_INTRINSICS, "getCOROUTINE_SUSPENDED")) continue
        val next = opcodesFrom(insns, i + 1, 4)
        val stored = next.firstOrNull()?.opcode
        if (stored == Opcodes.ASTORE) suspendedLocals += (next[0] as VarInsnNode).`var`
        // The state switch: (ASTORE s | POP) ALOAD; GETFIELD label; TABLESWITCH.
        val switch = if (
            (stored == Opcodes.ASTORE || stored == Opcodes.POP) && next.size == 4 &&
            next[1].opcode == Opcodes.ALOAD && next[2].opcode == Opcodes.GETFIELD && next[3].opcode == Opcodes.TABLESWITCH
        ) next[3] else null
        if (switch != null) {
            for (j in insns) {
                ignored += j
                if (j === switch) break
            }
        }
    }
    for ((i, insn) in insns.withIndex()) {
        if (insn.opcode != Opcodes.IF_ACMPNE && insn.opcode != Opcodes.IF_ACMPEQ) continue
        val previous = previousOpcode(insns, i)
        if (previous is VarInsnNode && previous.opcode == Opcodes.ALOAD && previous.`var` in suspendedLocals) ignored += insn
        if (previous != null && previous.calls(COROUTINE_INTRINSICS, "getCOROUTINE_SUSPENDED")) ignored += insn
    }
}

/**
 * The first line number that belongs to code inlined from another file: the
 * smallest output line of the class's SMAP that is not the identity mapping of
 * its own source file. Int.MAX_VALUE without an SMAP.
 */
private fun firstInlinedLine(owner: ClassNode): Int {
    val smap = owner.sourceDebug ?: return Int.MAX_VALUE
    val lines = smap.lines()
    val start = lines.indexOf("*L")
    if (lines.firstOrNull() != "SMAP" || start < 0) return Int.MAX_VALUE
    var min = Int.MAX_VALUE
    var file = 1
    for (l in lines.drop(start + 1)) {
        if (l == "*E" || l.startsWith("*S")) break
        // InputStartLine[#LineFileID][,RepeatCount]:OutputStartLine[,OutputLineIncrement]
        val m = Regex("""(\d+)(#(\d+))?(,\d+)?:(\d+)(,\d+)?""").matchEntire(l) ?: error("unexpected SMAP line in ${owner.name}: $l")
        m.groupValues[3].takeIf { it.isNotEmpty() }?.let { file = it.toInt() }
        val input = m.groupValues[1].toInt()
        val output = m.groupValues[5].toInt()
        if (file == 1 && input == output) continue
        min = minOf(min, output)
    }
    return min
}
