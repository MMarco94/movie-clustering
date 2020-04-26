import org.junit.Test
import smile.math.distance.EuclideanDistance
import kotlin.random.Random
import kotlin.test.assertEquals

class EuclideanTest {

	private fun List<DoubleArray>.safeEuclideanDistance(): List<List<Double>> {
		val ret = mutableListOf<List<Double>>()
		forEachIndexed { firstIndex, firstEntity ->
			ret.add(List(size) { secondIndex ->
				when {
					secondIndex < firstIndex -> ret[secondIndex][firstIndex] //The distance is symmetric, I can save some calculations
					secondIndex == firstIndex -> 0.0
					else -> EuclideanDistance().d(firstEntity, this[secondIndex])
				}
			})
		}
		return ret
	}

	@Test
	fun testEuclidean() {
		val r = Random(42)
		val n = 20
		val randomMatrix = List(n) {
			DoubleArray(n) { r.nextDouble() }
		}
		assertEquals(randomMatrix.safeEuclideanDistance(), randomMatrix.euclideanDistances().map { it.asList() })
	}

}