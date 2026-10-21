package bench.changes

import bench.env.FaultProfile
import bench.harness.experiment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The change requests' requirements must catch a design that lacks the change; this checkout holds the designs without it. */
class ChangeRequestsTest {
    @Test
    fun `every unpatched design violates each change request's own requirements`() {
        for (cr in changeRequests) {
            for ((key, system) in designs) {
                val cell = experiment(key, system, FaultProfile.Stress, crashes = false, runs = 100, extension = cr.extension())
                assertTrue(cell.anyChangeViolation.hits > 0, "${cr.key} $key")
            }
        }
    }

    @Test
    fun `without a change request's extension, no change requirement is checked`() {
        val (key, system) = designs.single { it.first == "hardened" }
        val cell = experiment(key, system, FaultProfile.Stress, crashes = true, runs = 50)
        assertEquals(0, cell.anyChangeViolation.hits)
        assertEquals(0, cell.anyViolation.hits)
    }
}
