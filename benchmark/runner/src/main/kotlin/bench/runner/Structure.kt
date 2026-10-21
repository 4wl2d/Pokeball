package bench.runner

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
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

    /** Library types the unit names (see [isFramework]). */
    val external = LinkedHashSet<String>()

    /** Every type outside the feature code that the unit names, except the JDK's and the Kotlin standard library's. */
    val outside = LinkedHashSet<String>()
    var abstract = false
    var mutableFields = 0
    var instructions = 0
    var impureReferences = LinkedHashSet<String>()

    /** The source file that declares the unit, with its package path. */
    var sourceFile = ""
    val packageName: String get() = name.substringBeforeLast('/', "").replace('/', '.')
}

/**
 * Martin's package metrics. [ce], [instability] and [distance] count only
 * dependencies on other feature packages; [ceAll], [instabilityAll] and
 * [distanceAll] also count dependencies on every type outside the feature code
 * except the JDK's and the Kotlin standard library's (libraries, toolkit
 * packages and the shared simulation), as Martin's definition does.
 */
data class PackageMetrics(
    val name: String,
    val ca: Int,
    val ce: Int,
    val ceAll: Int,
    val abstractness: Double,
    val instability: Double,
    val instabilityAll: Double,
    val distance: Double,
    val distanceAll: Double,
)

/** Cohesion of one measured class. */
/** Cohesion of one measured class; [camc] is null when its counted methods have no parameter types at all. */
data class ClassCohesion(val name: String, val methods: Int, val lcom4: Int, val camc: Double?)

data class StructureMetrics(
    val units: Int,
    val topLevelClasses: Int,
    val sourceFiles: Int,
    val packages: Int,
    val cboMean: Double,
    val cboMax: Int,
    val lcom4Mean: Double,
    val lcom4CohesiveClasses: Int,
    val lcom4Measured: Int,
    val camcMean: Double,
    val camcMeasured: Int,
    /** Mean distance from the main sequence over packages with dependencies; null with fewer than two such packages. */
    val distanceMean: Double?,
    /** The same with [PackageMetrics.distanceAll]. */
    val distanceMeanAll: Double?,
    val propagationCost: Double,
    val propagationCostTopLevel: Double,
    val propagationCostFile: Double,
    val cyclicGroups: Int,
    val unitsInCycles: Int,
    val frameworkTypes: Int,
    val frameworkTypeNames: List<String>,
    val instructions: Int,
    val pureShare: Double,
    val functions: List<FunctionComplexity>,
    val cohesion: List<ClassCohesion>,
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

/** The JDK and the Kotlin standard library, which every implementation uses and no metric counts. */
private val platform = listOf("java/", "javax/", "kotlin/", "org/jetbrains/annotations/")

/**
 * Library types the feature code names: everything outside the JDK, the
 * Kotlin standard library and the simulated environment that all
 * implementations share. Compiler-written bridges and the descriptors of
 * default-argument helpers are not read; caught exception types are.
 */
private fun isFramework(internalName: String) =
    (platform + listOf("bench/env/", "ballarch/testkit/")).none { internalName.startsWith(it) }

private val dataGenerated = Regex("^(component\\d+|copy|equals|hashCode|toString)$")
private val enumGenerated = setOf("values", "valueOf", "getEntries")
private val accessor = Regex("^(get|set|is)[A-Z].*")
private const val DEFAULT_MARKER = "Lkotlin/jvm/internal/DefaultConstructorMarker;"

/** A method name without Kotlin's value-class mangling suffix: `getCall-izTbEP8` → `getCall`, `copy-Zn-eFaA` → `copy`. */
fun baseName(name: String): String = name.substringBefore('-')

/**
 * The backing field of property accessor [name], if the class has one: `getX`
 * and `setX` belong to field `x`, or `X` for a PascalCase top-level `val`;
 * `isX` belongs to field `isX` or `x`.
 */
private fun ownField(fields: Set<String>, name: String): String? {
    val property = name.substring(if (name.startsWith("is")) 2 else 3)
    val candidates = listOfNotNull(name.takeIf { it.startsWith("is") }, property.replaceFirstChar { it.lowercase() }, property)
    return candidates.firstOrNull { it in fields }
}

fun analyze(classesDir: File, rootPackage: String, excludedPackages: List<String>): StructureMetrics {
    val root = rootPackage.replace('.', '/') + "/"
    val excluded = excludedPackages.map { it.replace('.', '/') + "/" }
    // Sorted, so that results do not depend on the file system's listing order.
    val nodes = classesDir.walkTopDown().filter { it.isFile && it.extension == "class" }.sortedBy { it.relativeTo(classesDir).path }.map { f ->
        ClassNode().also { ClassReader(f.readBytes()).accept(it, 0) }
    }.filter { n -> n.name.startsWith(root) && excluded.none { n.name.startsWith(it) } }.toList()
    val model = ClassModel(nodes)

    val units = LinkedHashMap<String, CodeUnit>()
    for (n in nodes) units.getOrPut(unitOf(n.name)) { CodeUnit(unitOf(n.name)) }.nodes += n
    fun feature(internalName: String) = internalName.startsWith(root) && excluded.none { internalName.startsWith(it) }

    for (u in units.values) {
        val declared = u.nodes.firstOrNull { it.name == u.name } ?: u.nodes.first()
        u.abstract = declared.access and (Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT) != 0
        u.sourceFile = u.name.substringBeforeLast('/') + "/" + (declared.sourceFile ?: declared.name.substringAfterLast('/'))
        val refs = LinkedHashSet<String>()
        for (n in u.nodes) {
            n.superName?.let { refs += it }
            refs += n.interfaces
            for (f in n.fields) {
                refs += typesIn(f.desc)
                if (f.access and Opcodes.ACC_FINAL == 0 && f.access and Opcodes.ACC_SYNTHETIC == 0 && n.name == u.name) u.mutableFields++
            }
            for (m in n.methods) {
                // Real JVM opcodes; ASM's pseudo-nodes for labels, line numbers and stack-map frames are not instructions.
                u.instructions += m.instructions.count { it.opcode >= 0 }
                // A bridge is a compiler-written forwarding stub: no type in it was written by the author.
                if (m.access and Opcodes.ACC_BRIDGE != 0) continue
                refs += typesIn(m.desc)
                // Types named only in a catch clause live in the exception table, not in instructions.
                for (tc in m.tryCatchBlocks) tc.type?.let { refs += it }
                for (insn in m.instructions) {
                    when (insn) {
                        is MethodInsnNode -> {
                            refs += insn.owner
                            // A call that omits default arguments goes through a `$default` helper or a constructor
                            // taking a DefaultConstructorMarker, whose descriptor lists every parameter, written or not.
                            if (!insn.name.endsWith("\$default") && DEFAULT_MARKER !in insn.desc) refs += typesIn(insn.desc)
                        }
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
                if (platform.none { r.startsWith(it) }) u.outside += r
                if (impurePrefixes.any { r.startsWith(it) }) u.impureReferences += r
            }
        }
    }
    units.values.forEach { u -> u.dependsOn.retainAll(units.keys) }

    // CBO: coupled in either direction.
    val dependents = units.keys.associateWith { k -> units.values.filter { k in it.dependsOn }.map { it.name }.toSet() }
    val cbo = units.values.map { (it.dependsOn + dependents.getValue(it.name)).size }

    // Cohesion of each unit's declared class that has at least one counted method.
    val cohesion = units.values.mapNotNull { u -> u.nodes.firstOrNull { it.name == u.name } }.mapNotNull { node ->
        val lcom = lcom4(model, node) ?: return@mapNotNull null
        ClassCohesion(node.name.replace('/', '.'), countedMethods(node).size, lcom, camc(node))
    }

    // Martin's package metrics over feature packages.
    val packages = units.values.groupBy { it.packageName }
    val packageMetrics = packages.map { (p, members) ->
        val ce = members.count { u -> u.dependsOn.any { units.getValue(it).packageName != p } }
        val ceAll = members.count { u -> u.outside.isNotEmpty() || u.dependsOn.any { units.getValue(it).packageName != p } }
        val ca = units.values.count { u -> u.packageName != p && u.dependsOn.any { units.getValue(it).packageName == p } }
        val a = members.count { it.abstract }.toDouble() / members.size
        val i = if (ca + ce == 0) 0.0 else ce.toDouble() / (ca + ce)
        val iAll = if (ca + ceAll == 0) 0.0 else ceAll.toDouble() / (ca + ceAll)
        PackageMetrics(p, ca, ce, ceAll, a, i, iAll, kotlin.math.abs(a + i - 1), kotlin.math.abs(a + iAll - 1))
    }
    val connected = packageMetrics.filter { it.ca + it.ce > 0 }
    val connectedAll = packageMetrics.filter { it.ca + it.ceAll > 0 }

    // Propagation cost at three granularities: named classes, top-level classes, source files.
    fun regroup(key: (CodeUnit) -> String): Map<String, Set<String>> {
        val keyOf = units.values.associate { it.name to key(it) }
        return units.values.groupBy(key).mapValues { (k, us) -> us.flatMap { it.dependsOn }.map { keyOf.getValue(it) }.filter { it != k }.toSet() }
    }
    val unitGraph = units.mapValues { it.value.dependsOn.toSet() }
    val topLevelGraph = regroup { it.name.substringBefore('$') }
    val fileGraph = regroup { it.sourceFile }

    val names = units.keys.toList()
    val sccs = stronglyConnected(names) { units.getValue(it).dependsOn }.filter { it.size > 1 }
    val framework = units.values.flatMap { it.external }.toSortedSet()
    val total = units.values.sumOf { it.instructions }
    val pure = units.values.filter { it.impureReferences.isEmpty() && it.mutableFields == 0 }.sumOf { it.instructions }

    return StructureMetrics(
        units = units.size,
        topLevelClasses = topLevelGraph.size,
        sourceFiles = fileGraph.size,
        packages = packages.size,
        cboMean = cbo.average(),
        cboMax = cbo.maxOrNull() ?: 0,
        lcom4Mean = cohesion.map { it.lcom4 }.average(),
        lcom4CohesiveClasses = cohesion.count { it.lcom4 == 1 },
        lcom4Measured = cohesion.size,
        camcMean = cohesion.mapNotNull { it.camc }.average(),
        camcMeasured = cohesion.count { it.camc != null },
        distanceMean = if (connected.size < 2) null else connected.map { it.distance }.average(),
        distanceMeanAll = if (connectedAll.size < 2) null else connectedAll.map { it.distanceAll }.average(),
        propagationCost = propagationCost(unitGraph),
        propagationCostTopLevel = propagationCost(topLevelGraph),
        propagationCostFile = propagationCost(fileGraph),
        cyclicGroups = sccs.size,
        unitsInCycles = sccs.sumOf { it.size },
        frameworkTypes = framework.size,
        frameworkTypeNames = framework.map { it.replace('/', '.') },
        instructions = total,
        pureShare = if (total == 0) 0.0 else pure.toDouble() / total,
        functions = complexity(model),
        cohesion = cohesion,
        packagesDetail = packageMetrics.sortedBy { it.name },
    )
}

/** Density of the reflexive-transitive closure of a dependency graph: the average share of the nodes a change can reach. */
fun propagationCost(graph: Map<String, Set<String>>): Double {
    val names = graph.keys.toList()
    val index = names.withIndex().associate { it.value to it.index }
    val n = names.size
    val reach = Array(n) { i -> BooleanArray(n).also { it[i] = true } }
    for ((i, name) in names.withIndex()) for (d in graph.getValue(name)) reach[i][index.getValue(d)] = true
    for (k in 0 until n) for (i in 0 until n) if (reach[i][k]) for (j in 0 until n) if (reach[k][j]) reach[i][j] = true
    return reach.sumOf { row -> row.count { it } }.toDouble() / (n.toDouble() * n)
}

/**
 * The methods that the cohesion and complexity metrics count: hand-written
 * methods with a body. Not counted: constructors and static initialisers,
 * synthetic and bridge methods, methods whose name contains `$` (lambda bodies,
 * local functions and default-argument helpers, which count as part of the
 * method they come from), the members Kotlin generates for data classes and
 * enums, and the accessors of properties with a backing field.
 */
fun countedMethods(node: ClassNode): List<MethodNode> {
    val fields = node.fields.map { it.name }.toSet()
    val isEnum = node.access and Opcodes.ACC_ENUM != 0
    return node.methods.filter { m ->
        val name = baseName(m.name)
        m.name != "<init>" && m.name != "<clinit>" &&
            m.access and (Opcodes.ACC_SYNTHETIC or Opcodes.ACC_BRIDGE or Opcodes.ACC_ABSTRACT) == 0 &&
            '$' !in m.name &&
            !dataGenerated.matches(name) &&
            !(isEnum && name in enumGenerated) &&
            !(accessor.matches(name) && ownField(fields, name) != null)
    }
}

/**
 * CAMC, Cohesion Among Methods of a Class (Bansiya et al. 1999): with M_i the
 * set of parameter types of counted method i, T their union and n the number
 * of methods, CAMC = Σ|M_i| / (n·|T|). As in the paper's worked example, the
 * receiver (`this`) is a parameter of every instance method and primitive
 * types count; the continuation of a suspending function does not. One
 * deviation: constructors are not counted, as for LCOM4, because Kotlin
 * generates them from the property list. Null for a class without counted
 * methods or without any parameter type.
 */
fun camc(node: ClassNode): Double? {
    val methods = countedMethods(node)
    if (methods.isEmpty()) return null
    val types = methods.map { m ->
        val params = Type.getArgumentTypes(m.desc).map { it.descriptor }.filter { it != "Lkotlin/coroutines/Continuation;" }
        (if (m.access and Opcodes.ACC_STATIC == 0) listOf("L${node.name};") + params else params).toSet()
    }
    val all = types.flatten().toSet()
    if (all.isEmpty()) return null
    return types.sumOf { it.size }.toDouble() / (methods.size * all.size)
}

/**
 * LCOM4 (Hitz and Montazeri): connected components of the graph whose nodes are
 * the class's counted methods ([countedMethods]) and whose edges join methods
 * that use a common field of the class or call each other. A method's body
 * includes its lambdas, local functions, coroutine and anonymous classes, and
 * the default-argument and accessor helpers it calls ([ClassModel.parts]);
 * calling a property accessor uses its field. Null for a class with no counted method.
 */
fun lcom4(model: ClassModel, node: ClassNode): Int? {
    val fields = node.fields.map { it.name }.toSet()
    val methods = countedMethods(node)
    if (methods.isEmpty()) return null
    val bodies = methods.map { m -> model.parts(node, m, helpers = true).flatMap { it.second.instructions.toList() } }
    val uses = bodies.map { fieldUses(it, node.name, fields) }
    val parent = IntArray(methods.size) { it }
    fun find(x: Int): Int = if (parent[x] == x) x else find(parent[x]).also { parent[x] = it }
    fun union(a: Int, b: Int) { parent[find(a)] = find(b) }
    for (i in methods.indices) {
        for (j in i + 1 until methods.size) if (uses[i].any { it in uses[j] }) union(i, j)
        for (insn in bodies[i]) {
            if (insn is MethodInsnNode && insn.owner == node.name) {
                val j = methods.indexOfFirst { it.name == insn.name && it.desc == insn.desc }
                if (j >= 0) union(i, j)
            }
        }
    }
    return methods.indices.map { find(it) }.toSet().size
}

/** Own fields that [insns] read or write, directly or through the class's own property accessors. */
private fun fieldUses(insns: List<AbstractInsnNode>, owner: String, fields: Set<String>): Set<String> {
    val used = HashSet<String>()
    for (insn in insns) {
        if (insn is FieldInsnNode && insn.owner == owner && insn.name in fields) used += insn.name
        if (insn is MethodInsnNode && insn.owner == owner) {
            val name = baseName(insn.name)
            if (accessor.matches(name)) ownField(fields, name)?.let { used += it }
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
