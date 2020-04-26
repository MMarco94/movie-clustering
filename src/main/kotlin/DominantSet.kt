import smile.math.MathEx

fun dominantSetWeights(similarityMatrix: List<DoubleArray>, continueCondition: ContinueCondition = DefaultContinueCondition): DoubleArray {
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

object DefaultContinueCondition : ContinueCondition {
	override fun canContinue(iteration: Int, previousDominantSetWeights: DoubleArray, newDominantSetWeights: DoubleArray): Boolean {
		//I evaluate the distances only every 10 steps
		if (iteration % 10 != 0) return true

		val distance = MathEx.norm(newDominantSetWeights - previousDominantSetWeights)
		val previousElements = previousDominantSetWeights.count { it > 0.0 }
		val newElements = newDominantSetWeights.count { it > 0.0 }
		println("#$iteration: distance = $distance (${newElements} elements)")
		return (iteration < 5000 && distance > 1e-6) || (iteration < 50000 && newElements > newDominantSetWeights.size / 2) || newElements < previousElements
	}
}