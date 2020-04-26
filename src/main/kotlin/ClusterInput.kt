import smile.clustering.XMeans
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.exp

class ClusterOutput(
	val input: ClusterInput,
	val dominantSet: List<Node.Entity>,
	val dominantSetWeights: DoubleArray
)

class ClusterInput private constructor(
	val distanceAlgorithm: Distance,
	val users: List<Node.User>,
	val entities: List<Node.Entity>,
	val adjacencyMatrix: List<DoubleArray>,//Entity x User map
	val entitySimilarityMatrix: List<DoubleArray>//entity x entity
) {

	fun userEntityHeatMap(): BufferedImage {
		println("Computing userEntityHeatMap")
		val img = BufferedImage(users.size, entities.size, BufferedImage.TYPE_INT_RGB)
		adjacencyMatrix.forEachIndexed { eIndex, similarities ->
			similarities.forEachIndexed { uIndex, similarity ->
				if (similarity > 0) {
					img.setRGB(uIndex, eIndex, Color.HSBtoRGB(313F / 360F, 1F, 1 - exp(-similarity).toFloat()))
				}
			}
		}
		return img
	}

	fun xMeansOnSimilarity(): XMeans {
		return XMeans.fit(entitySimilarityMatrix.toTypedArray(), 50)
	}

	fun xMeansOnAdjacencyMatrix(): XMeans {
		return XMeans.fit(adjacencyMatrix.toTypedArray(), 50)
	}

	fun extractDominantSet(continueCondition: ContinueCondition = DefaultContinueCondition): ClusterOutput {
		val dominantSetWeights = dominantSetWeights(entitySimilarityMatrix, continueCondition)
		return ClusterOutput(
			this,
			dominantSetWeights
				.withIndex()
				.filter { it.value > 0 }
				.sortedByDescending { it.value }
				.map { entities[it.index] },
			dominantSetWeights
		)
	}

	fun withoutEntities(toRemove: List<Node.Entity>): ClusterInput {
		val builder = Builder(distanceAlgorithm, users, entities.minus(toRemove))
		val sim = mutableListOf<DoubleArray>()
		entities.forEachIndexed { eIndex, e ->
			if (e !in toRemove) {
				sim.add(entitySimilarityMatrix[eIndex].filterIndexed { index, _ -> entities[index] !in toRemove }.toDoubleArray())
				adjacencyMatrix[eIndex].forEachIndexed { uIndex, value ->
					//can use uIndex, since users didn't change
					builder.addRelation(e, uIndex, value)
				}
			}
		}
		builder.similarityMatrix = sim
		return builder.build()
	}

	fun dominantSetClusters(continueCondition: ContinueCondition = DefaultContinueCondition): Sequence<ClusterOutput> {
		return generateSequence(extractDominantSet(continueCondition)) { prev ->
			val newInput = prev.input.withoutEntities(prev.dominantSet)
			if (newInput.entities.isNotEmpty()) {
				newInput.extractDominantSet(continueCondition)
			} else null
		}
	}

	companion object {
		fun load(file: File): ClusterInput {
			file.useLines { ls ->
				val lines = ls.iterator()
				val distance = Distance.deserialize(lines.next())
				val users = mutableListOf<Node.User>()
				repeat(lines.next().toInt()) {
					users.add(Node.User.deserialize(lines.next()))
				}
				val entities = mutableListOf<Node.Entity>()
				repeat(lines.next().toInt()) {
					entities.add(Node.Entity.deserialize(lines.next()))
				}
				val builder = Builder(distance, users, entities)
				for (eIndex in 0 until lines.next().toInt()) {
					repeat(lines.next().toInt()) {
						val split = lines.next().split(" ")
						builder.addRelation(eIndex, split[0].toInt(), split[1].toDouble())
					}
				}
				val similarity = mutableListOf<DoubleArray>()
				repeat(lines.next().toInt()) {
					similarity.add(lines.next().split(" ").map { it.toDouble() }.toDoubleArray())
				}
				builder.similarityMatrix = similarity
				return builder.build()
			}
		}
	}


	class Builder(
		private var distanceAlgorithm: Distance,
		private val users: List<Node.User>,
		private val entities: List<Node.Entity>
	) {
		var similarityMatrix: List<DoubleArray>? = null

		private val relations = List(entities.size) {
			DoubleArray(users.size)
		}

		fun addRelation(entity: Node.Entity, user: Node.User, weight: Double) {
			addRelation(entity, users.indexOf(user), weight)
		}

		fun addRelation(entity: Node.Entity, userIndex: Int, weight: Double) {
			addRelation(entities.indexOf(entity), userIndex, weight)
		}

		fun addRelation(entityIndex: Int, userIndex: Int, weight: Double) {
			relations[entityIndex][userIndex] += weight
		}

		fun build(): ClusterInput {
			return ClusterInput(
				distanceAlgorithm,
				users,
				entities,
				relations,
				similarityMatrix ?: relations.similarityMatrix(distanceAlgorithm)
			)
		}
	}

	fun save(file: File) {
		file.outputStream().bufferedWriter().use { os ->
			os.write(distanceAlgorithm.serialize())
			os.write("\n")
			os.write("${users.size}\n")
			users.forEach {
				os.write(it.serialize())
				os.write("\n")
			}
			os.write("${entities.size}\n")
			entities.forEach {
				os.write(it.serialize())
				os.write("\n")
			}
			os.write("${adjacencyMatrix.size}\n")
			adjacencyMatrix.forEach { rel ->
				val edges = rel.withIndex().filter { it.value > 0 }
				os.write("${edges.size}\n")
				edges.forEach { (uIndex, value) ->
					os.write("$uIndex $value\n")
				}
			}
			os.write("${entitySimilarityMatrix.size}\n")
			entitySimilarityMatrix.forEach { rel ->
				os.write(rel.joinToString(" ") + "\n")
			}
		}
	}

}