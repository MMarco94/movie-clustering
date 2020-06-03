import com.femastudios.kotlin.core.mapToSet
import smile.clustering.XMeans
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.exp

class DSClusterOutput(
	val input: ClusterInput,
	val dominantSet: Set<Node.Entity>,
	val dominantSetWeights: DoubleArray
)

class ClusterInput private constructor(
	val distanceAlgorithm: Distance,
	val users: List<Node.User>,
	val entities: List<Node.Entity>,
	val adjacencyMatrix: List<DoubleArray>,//Entity x User map
	val entitySimilarityMatrix: List<DoubleArray>//entity x entity
) {

	fun reduce(entityLimit: Int): ClusterInput {
		return reduce(entities.withIndex().sortedByDescending { getEntityWeight(it.index) }.take(entityLimit))
	}

	private fun reduce(entitiesToKeep: List<IndexedValue<Node.Entity>>): ClusterInput {
		val entityIndexesToKeep = entitiesToKeep.mapToSet { it.index }
		return ClusterInput(
			distanceAlgorithm,
			users,
			entitiesToKeep.sortedBy { it.index }.map { it.value },
			adjacencyMatrix.filterIndexed { index, _ -> index in entityIndexesToKeep },
			entitySimilarityMatrix.mapIndexedNotNull { i1, similarities ->
				if (i1 in entityIndexesToKeep) {
					similarities.filterIndexed { i2, _ -> i2 in entityIndexesToKeep }.toDoubleArray()
				} else null
			}
		)
	}

	private fun getEntityWeight(entityIndex: Int): Double {
		return adjacencyMatrix[entityIndex].sum()
	}

	fun withoutEntities(toRemove: Set<Node.Entity>): ClusterInput {
		return reduce(entities.withIndex().filter { it.value !in toRemove })
	}

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

	fun extractDominantSet(continueCondition: ContinueCondition = DefaultContinueCondition()): DSClusterOutput {
		val dominantSetWeights = dominantSetWeights(entitySimilarityMatrix, continueCondition)
		return DSClusterOutput(
			this,
			dominantSetWeights
				.withIndex()
				.filter { it.value > ZERO }
				.sortedByDescending { it.value }
				.mapToSet { entities[it.index] },
			dominantSetWeights
		)
	}

	fun dominantSetClusters(continueCondition: ContinueCondition = DefaultContinueCondition()): Sequence<DSClusterOutput> {
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
						val userIndex = split[0].toInt()
						val weight = split[1].toDouble()
						builder.addRelation(eIndex, userIndex, weight)
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