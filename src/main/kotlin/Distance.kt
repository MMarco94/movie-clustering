import smile.math.MathEx
import smile.math.distance.EuclideanDistance
import kotlin.math.exp

interface Distance {
	fun distance(a: DoubleArray, b: DoubleArray): Double
	val isSymmetric: Boolean
	fun serialize(): String

	companion object {
		fun deserialize(str: String): Distance {
			return when (str) {
				"Euclidean" -> EuclideanDistance
				"Cosin" -> CosinDistance
				else -> throw IllegalArgumentException("Unknown distance $str")
			}
		}
	}
}

object EuclideanDistance : Distance {
	override fun distance(a: DoubleArray, b: DoubleArray): Double {
		return EuclideanDistance().d(a, b)
	}

	override val isSymmetric = true
	override fun serialize() = "Euclidean"
}

object CosinDistance : Distance {
	override fun distance(a: DoubleArray, b: DoubleArray): Double {
		return (1 - MathEx.dot(a, b) / (MathEx.norm(a) * MathEx.norm(b))) / 2
	}

	override val isSymmetric = true
	override fun serialize() = "Cosin"
}

fun List<DoubleArray>.similarityMatrix(distance: Distance = EuclideanDistance): List<DoubleArray> {
	if (isEmpty()) return this
	val distanceMatrix = distance(distance).mapOnEach { it * it }
	val sigma2 = MathEx.median(distanceMatrix.flatMap { it.toList() }.toDoubleArray())
	return distanceMatrix.mapOnEach {
		exp(-it / sigma2)
	}
}

fun List<DoubleArray>.distance(distance: Distance = EuclideanDistance): List<DoubleArray> {
	println("Calculating Distances...")
	val ret = mutableListOf<DoubleArray>()
	forEachIndexed { firstIndex, firstEntity ->
		println("$firstIndex / $size")
		ret.add(DoubleArray(size) { secondIndex ->
			when {
				distance.isSymmetric && secondIndex < firstIndex -> ret[secondIndex][firstIndex] //The distance is symmetric, I can save some calculations
				else -> distance.distance(firstEntity, this[secondIndex])
			}
		})
	}
	return ret
}