package bench.runner

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import java.io.File

/**
 * Structural metrics from bytecode, defined in benchmark/README.md.
 *
 * A *unit* is a named class: a top-level class, a file facade, or a nested
 * class whose name starts with a capital letter. Compiler-generated classes
 * (lambdas, coroutine continuations, `$WhenMappings`, `$Companion`,
 * `$DefaultImpls`) are merged into the unit that declares them.
 */
class CodeUnit(val name: String) {
    val nodes = ArrayList<ClassNode>()
    val dependsOn = LinkedHashSet<String>()
    val external = LinkedHashSet<String>()
    var abstract = false
    var mutableFields = 0
    var instructions = 0
    var impureReferences = LinkedHashSet<String>()
    val packageName: String get() = name.substringBeforeLast('/', "").replace('/', '.')
}

data class PackageMetrics(val name: String, val ca: Int, val ce: Int, val abstractness: Double, val instability: Double, val distance: Double)

data class StructureMetrics(
    val units: Int,
    val packages: Int,
    val cboMean: Double,
    val cboMax: Int,
    val lcom4Mean: Double,
    val lcom4Cohesive: Double,
    val lcom4Measured: Int,
    val camcMean: Double,
    val camcMeasured: Int,
    /** Mean distance from the main sequence over packages with dependencies; null with fewer than two such packages. */
    val distanceMean: Double?,
    val propagationCost: Double,
    val cyclicGroups: Int,
    val unitsInCycles: Int,
    val frameworkTypes: Int,
    val frameworkTypeNames: List<String>,
    val instructions: Int,
    val pureShare: Double,
    val packagesDetail: List<PackageMetrics>,
)

private val generatedNested = setOf("WhenMappings", "Companion", "DefaultImpls")

/** The unit a binary class name belongs to. */
fun unitOf(internalName: String): String {
    val parts = internalName.split('$')
    val kept = ArrayList<String>()
    kept += parts[0]
    for (p in parts.drop(1)) {
        if (p.isEmpty() || !p[0].isUpperCase() || p in generatedNested) break
        kept += p
    }
    return kept.joinToString("$")
}

/** References that make a unit effectful: concurrency, I/O, time, the simulated environment's services, a runtime. */
private val impurePrefixes = listOf(
    "kotlinx/coroutines/", "kotlin/coroutines/", "java/util/concurrent/", "java/io/", "java/nio/", "java/net/",
    "java/time/Clock", "java/lang/System", "java/lang/Thread", "kotlin/random/", "java/util/Random",
    "bench/env/SimDb", "bench/env/Provider", "bench/env/ProviderClient", "bench/env/Process", "bench/env/World",
    "ballarch/runtime/",
)

/**
 * Library types an author must know: everything outside the JDK, the Kotlin
 * standard library and the simulated environment that all implementations share.
 */
private fun isFramework(internalName: String) =
    listOf("java/", "javax/", "kotlin/", "org/jetbrains/annotations/", "bench/env/", "ballarch/testkit/").none { internalName.startsWith(it) }

private val dataGenerated = Regex("^(component\\d+|copy|copy\\\$default|equals|hashCode|toString)$")
private val accessor = Regex("^(get|set|is)[A-Z].*")

fun analyze(classesDir: File, rootPackage: String, excludedPackages: List<String>): StructureMetrics {
    val root = rootPackage.replace('.', '/') + "/"
    val excluded = excludedPackages.map { it.replace('.', '/') + "/" }
    // Sorted, so that results do not depend on the file system's listing order.
    val nodes = classesDir.walkTopDown().filter { it.isFile && it.extension == "class" }.sortedBy { it.relativeTo(classesDir).path }.map { f ->
        ClassNode().also { ClassReader(f.readBytes()).accept(it, 0) }
    }.filter { n -> n.name.startsWith(root) && excluded.none { n.name.startsWith(it) } }.toList()

    val units = LinkedHashMap<String, CodeUnit>()
    for (n in nodes) units.getOrPut(unitOf(n.name)) { CodeUnit(unitOf(n.name)) }.nodes += n
    fun feature(internalName: String) = internalName.startsWith(root) && excluded.none { internalName.startsWith(it) }

    for (u in units.values) {
        val declared = u.nodes.firstOrNull { it.name == u.name } ?: u.nodes.first()
        u.abstract = declared.access and (Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT) != 0
        val refs = LinkedHashSet<String>()
        for (n in u.nodes) {
            n.superName?.let { refs += it }
            refs += n.interfaces
            for (f in n.fields) {
                refs += typesIn(f.desc)
                if (f.access and Opcodes.ACC_FINAL == 0 && f.access and Opcodes.ACC_SYNTHETIC == 0 && n.name == u.name) u.mutableFields++
            }
            for (m in n.methods) {
                refs += typesIn(m.desc)
                u.instructions += m.instructions.size()
                for (insn in m.instructions) {
                    when (insn) {
                        is MethodInsnNode -> { refs += insn.owner; refs += typesIn(insn.desc) }
                        is FieldInsnNode -> { refs += insn.owner; refs += typesIn(insn.desc) }
                        is TypeInsnNode -> refs += typesIn(if (insn.desc.startsWith("[")) insn.desc else "L${insn.desc};")
                        is LdcInsnNode -> (insn.cst as? Type)?.let { refs += typesIn(it.descriptor) }
                        is InvokeDynamicInsnNode -> refs += typesIn(insn.desc)
                        is MultiANewArrayInsnNode -> refs += typesIn(insn.desc)
                    }
                }
            }
        }
        for (r in refs) {
            val target = unitOf(r)
            if (feature(r)) {
                if (target != u.name) u.dependsOn += target
            } else {
                if (isFramework(r)) u.external += r
                if (impurePrefixes.any { r.startsWith(it) }) u.impureReferences += r
            }
        }
    }
    units.values.forEach { u -> u.dependsOn.retainAll(units.keys) }

    // CBO: coupled in either direction.
    val dependents = units.keys.associateWith { k -> units.values.filter { k in it.dependsOn }.map { it.name }.toSet() }
    val cbo = units.values.map { (it.dependsOn + dependents.getValue(it.name)).size }

    // Cohesion per unit with at least one counted method: LCOM4 (shared fields) and CAMC (shared parameter types).
    val declaredClasses = units.values.mapNotNull { u -> u.nodes.firstOrNull { it.name == u.name } }
    val lcom = declaredClasses.mapNotNull(::lcom4)
    val camc = declaredClasses.mapNotNull(::camc)

    // Martin's package metrics over feature packages.
    val packages = units.values.groupBy { it.packageName }
    val packageMetrics = packages.map { (p, members) ->
        val ce = members.count { u -> u.dependsOn.any { units.getValue(it).packageName != p } }
        val ca = units.values.count { u -> u.packageName != p && u.dependsOn.any { units.getValue(it).packageName == p } }
        val a = members.count { it.abstract }.toDouble() / members.size
        val i = if (ca + ce == 0) 0.0 else ce.toDouble() / (ca + ce)
        PackageMetrics(p, ca, ce, a, i, kotlin.math.abs(a + i - 1))
    }
    val connected = packageMetrics.filter { it.ca + it.ce > 0 }

    // Propagation cost: density of the reflexive-transitive closure of the unit dependency graph.
    val names = units.keys.toList()
    val index = names.withIndex().associate { it.value to it.index }
    val n = names.size
    val reach = Array(n) { i -> BooleanArray(n).also { it[i] = true } }
    for ((i, name) in names.withIndex()) for (d in units.getValue(name).dependsOn) reach[i][index.getValue(d)] = true
    for (k in 0 until n) for (i in 0 until n) if (reach[i][k]) for (j in 0 until n) if (reach[k][j]) reach[i][j] = true
    val propagation = reach.sumOf { row -> row.count { it } }.toDouble() / (n.toDouble() * n)

    val sccs = stronglyConnected(names) { units.getValue(it).dependsOn }.filter { it.size > 1 }
    val framework = units.values.flatMap { it.external }.toSortedSet()
    val total = units.values.sumOf { it.instructions }
    val pure = units.values.filter { it.impureReferences.isEmpty() && it.mutableFields == 0 }.sumOf { it.instructions }

    return StructureMetrics(
        units = n,
        packages = packages.size,
        cboMean = cbo.average(),
        cboMax = cbo.maxOrNull() ?: 0,
        lcom4Mean = lcom.average(),
        lcom4Cohesive = lcom.count { it == 1 }.toDouble() / lcom.size,
        lcom4Measured = lcom.size,
        camcMean = camc.average(),
        camcMeasured = camc.size,
        distanceMean = if (connected.size < 2) null else connected.map { it.distance }.average(),
        propagationCost = propagation,
        cyclicGroups = sccs.size,
        unitsInCycles = sccs.sumOf { it.size },
        frameworkTypes = framework.size,
        frameworkTypeNames = framework.map { it.replace('/', '.') },
        instructions = total,
        pureShare = if (total == 0) 0.0 else pure.toDouble() / total,
        packagesDetail = packageMetrics.sortedBy { it.name },
    )
}

/** The methods counted by the cohesion metrics. */
private fun countedMethods(node: ClassNode): List<MethodNode> {
    val fields = node.fields.map { it.name }.toSet()
    return node.methods.filter { m ->
        m.name != "<init>" && m.name != "<clinit>" &&
            m.access and (Opcodes.ACC_SYNTHETIC or Opcodes.ACC_BRIDGE or Opcodes.ACC_ABSTRACT) == 0 &&
            !dataGenerated.matches(m.name) && !m.name.contains('$') &&
            !(accessor.matches(m.name) && fields.any { f -> m.name.substring(if (m.name.startsWith("is")) 2 else 3).replaceFirstChar { it.lowercase() } == f })
    }
}

/**
 * CAMC, Cohesion Among Methods of a Class (Bansiya et al.): the share of the
 * (method, parameter type) pairs that occur, over the class's counted methods
 * and the distinct object types of their parameters (the continuation of a
 * suspending function is not counted). It credits methods that work on the same
 * types, as pure functions over a state type do. Null without methods or types.
 */
fun camc(node: ClassNode): Double? {
    val methods = countedMethods(node)
    val types = methods.map { m ->
        Type.getArgumentTypes(m.desc).filter { it.sort == Type.OBJECT && it.internalName != "kotlin/coroutines/Continuation" }.map { it.internalName }.toSet()
    }
    val all = types.flatten().toSet()
    if (methods.isEmpty() || all.isEmpty()) return null
    return types.sumOf { it.size }.toDouble() / (methods.size * all.size)
}

/**
 * LCOM4 (Hitz and Montazeri): connected components of the graph whose nodes are
 * the class's own methods and whose edges join methods that use a common field
 * of the class or call each other. Constructors, compiler-generated members,
 * data-class members and property accessors are not counted. Returns null for
 * a class with no counted method.
 */
fun lcom4(node: ClassNode): Int? {
    val fields = node.fields.map { it.name }.toSet()
    val methods = countedMethods(node)
    if (methods.isEmpty()) return null
    val uses = methods.map { m -> m.fieldUses(node.name, fields) }
    val parent = IntArray(methods.size) { it }
    fun find(x: Int): Int = if (parent[x] == x) x else find(parent[x]).also { parent[x] = it }
    fun union(a: Int, b: Int) { parent[find(a)] = find(b) }
    for (i in methods.indices) {
        for (j in i + 1 until methods.size) if (uses[i].any { it in uses[j] }) union(i, j)
        for (insn in methods[i].instructions) {
            if (insn is MethodInsnNode && insn.owner == node.name) {
                val j = methods.indexOfFirst { it.name == insn.name && it.desc == insn.desc }
                if (j >= 0) union(i, j)
            }
        }
    }
    return methods.indices.map { find(it) }.toSet().size
}

/** Own fields a method reads or writes, directly or through its own property accessors. */
private fun MethodNode.fieldUses(owner: String, fields: Set<String>): Set<String> {
    val used = HashSet<String>()
    for (insn in instructions) {
        if (insn is FieldInsnNode && insn.owner == owner && insn.name in fields) used += insn.name
        if (insn is MethodInsnNode && insn.owner == owner && accessor.matches(insn.name)) {
            val f = insn.name.substring(if (insn.name.startsWith("is")) 2 else 3).replaceFirstChar { it.lowercase() }
            if (f in fields) used += f
        }
    }
    return used
}

private fun typesIn(desc: String): List<String> {
    val out = ArrayList<String>()
    var i = 0
    while (i < desc.length) {
        if (desc[i] == 'L') {
            val end = desc.indexOf(';', i)
            if (end < 0) break
            out += desc.substring(i + 1, end)
            i = end + 1
        } else {
            i++
        }
    }
    return out
}

/** Tarjan's strongly connected components. */
private fun stronglyConnected(nodes: List<String>, edges: (String) -> Set<String>): List<List<String>> {
    var index = 0
    val indices = HashMap<String, Int>()
    val low = HashMap<String, Int>()
    val stack = ArrayList<String>()
    val onStack = HashSet<String>()
    val result = ArrayList<List<String>>()
    fun visit(v: String) {
        indices[v] = index
        low[v] = index
        index++
        stack += v
        onStack += v
        for (w in edges(v)) {
            if (w !in indices) {
                visit(w)
                low[v] = minOf(low.getValue(v), low.getValue(w))
            } else if (w in onStack) {
                low[v] = minOf(low.getValue(v), indices.getValue(w))
            }
        }
        if (low[v] == indices[v]) {
            val component = ArrayList<String>()
            while (true) {
                val w = stack.removeAt(stack.size - 1)
                onStack -= w
                component += w
                if (w == v) break
            }
            result += component
        }
    }
    for (v in nodes) if (v !in indices) visit(v)
    return result
}
