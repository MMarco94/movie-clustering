import smile.math.MathEx
import kotlin.math.min

val ZERO = 1e-100

fun dominantSetWeights(similarityMatrix: List<DoubleArray>, continueCondition: ContinueCondition = DefaultContinueCondition()): DoubleArray {
	var dominantSetWeights = DoubleArray(similarityMatrix.size) { 1.0 / similarityMatrix.size }
	var secondaryArray = DoubleArray(similarityMatrix.size)

	var iteration = 0
	do {
		var sum = 0.0
		for (index in secondaryArray.indices) {
			val newValue = MathEx.dot(similarityMatrix[index], dominantSetWeights) * dominantSetWeights[index]
			sum += newValue
			secondaryArray[index] = newValue
		}
		for (index in secondaryArray.indices) {
			secondaryArray[index] /= sum
		}

		iteration++
		val canContinue = continueCondition.canContinue(iteration, dominantSetWeights, secondaryArray)

		val tmp = dominantSetWeights
		dominantSetWeights = secondaryArray
		secondaryArray = tmp
	} while (canContinue)
	return dominantSetWeights
}

interface ContinueCondition {
	fun canContinue(
		iteration: Int,
		previousDominantSetWeights: DoubleArray,
		newDominantSetWeights: DoubleArray
	): Boolean
}

class DefaultContinueCondition : ContinueCondition {
	private var lastSize = -1
	override fun canContinue(iteration: Int, previousDominantSetWeights: DoubleArray, newDominantSetWeights: DoubleArray): Boolean {
		//I evaluate the distances only every 10 steps
		if (iteration % 25 != 0) return true

		val distance = MathEx.norm(newDominantSetWeights - previousDominantSetWeights)
		var newElements = 0
		var min = Double.MAX_VALUE
		newDominantSetWeights.forEach { d ->
			if (d > ZERO) {
				newElements++
				min = min(min, d)
			}
		}
		println("#$iteration: distance = $distance, elements = $newElements, worstElementWeight = $min")
		val ret = (iteration < 5000 && distance > 1e-6) || (iteration < 50000 && newElements > newDominantSetWeights.size / 2) || (lastSize == -1 || newElements < lastSize)
		lastSize = newElements
		return ret
	}
}