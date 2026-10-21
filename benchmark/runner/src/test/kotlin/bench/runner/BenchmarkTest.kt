package bench.runner

import bench.env.FaultProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Keeps the benchmark's headline observations true in every build, on fewer runs than the published results. */
class BenchmarkTest {
    private fun cell(key: String, profile: FaultProfile, crashes: Boolean, runs: Int = 300): Cell {
        val a = architectures.single { it.key == key }
        return experiment(a.key, a.system, profile, crashes, runs, typicalTasks(a.system, profile))
    }

    @Test
    fun `hand-hardened and Ballarch checkouts violate no requirement`() {
        for (key in listOf("hardened", "ballarch")) {
            for (profile in FaultProfile.entries) {
                for (crashes in listOf(false, true)) assertEquals(0, cell(key, profile, crashes).anyViolation.hits, "$key $profile crash=$crashes")
            }
        }
    }

    @Test
    fun `the oracles detect violations in idiomatic baselines and in seeded bugs`() {
        for (key in listOf("layered", "actors", "mvu", "saga")) assertTrue(cell(key, FaultProfile.Stress, crashes = true).anyViolation.hits > 0, key)
        for ((name, system) in seeded) {
            val c = experiment(name, system, FaultProfile.Stress, crashes = true, runs = 300, typicalTasks(system, FaultProfile.Stress))
            assertTrue(c.anyViolation.hits > 0, name)
        }
    }

    @Test
    fun `generated classes belong to the class that declares them`() {
        assertEquals("a/B", unitOf("a/B\$process\$1"))
        assertEquals("a/B\$State", unitOf("a/B\$State"))
        assertEquals("a/B\$State\$Done", unitOf("a/B\$State\$Done"))
        assertEquals("a/B", unitOf("a/B\$WhenMappings"))
        assertEquals("a/FileKt", unitOf("a/FileKt\$checkout\$1\$1"))
    }
}
