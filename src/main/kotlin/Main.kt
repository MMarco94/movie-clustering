import smile.clustering.SpectralClustering
import smile.clustering.XMeans
import java.io.File
import javax.imageio.ImageIO
import kotlin.concurrent.thread

val outputDir = File("out")

//https://github.com/erogol/DominantSetClustering/blob/master/clusterDS.m
fun main() {
	val data = CachedLoader.loadGraph(CosinDistance)
	println("Created a graph with ${data.users.size} users, ${data.entities.size} entities")

	testSpectral(data)
	//textXMeansAdjacency(data)
	//textXMeansSimilarity(data)
	//testDominantSets(data)
}


private fun testXMeansAdjacency(data: ClusterInput) {
	testXMeans(data, "adj", data.xMeansOnAdjacencyMatrix(), data.adjacencyMatrix, false)
}

private fun testXMeansSimilarity(data: ClusterInput) {
	testXMeans(data, "sim", data.xMeansOnSimilarity(), data.entitySimilarityMatrix, true)
}

private fun testXMeans(data: ClusterInput, testName: String, xMeans: XMeans, matrix: List<DoubleArray>, sortBothAxis: Boolean) {
	println("Testing X-Means $testName")
	val entitiesWithClusterId = data.entities.withIndex().associateWith { (eIndex, _) -> xMeans.predict(matrix[eIndex]) }
	val sorting = entitiesWithClusterId.entries.sortedBy { it.value }.map { it.key.index }
	val sortedMatrix = if (sortBothAxis) {
		matrix.sortUsing(sorting).map { it.asList().sortUsing(sorting).toDoubleArray() }
	} else {
		matrix.sortUsing(sorting)
	}

	ImageIO.write(sortedMatrix.toImage(), "png", File(outputDir, "xmeans-$testName/sorted.png"))

	val clusters = entitiesWithClusterId.entries.groupBy { it.value }.mapValues { it.value.map { it.key.value } }
	clusters.forEach { (cluster, entities) ->
		saveFile(File(outputDir, "xmeans-$testName/cluster-$cluster.txt"), entities)
	}
}

private fun testSpectral(data: ClusterInput) {
	println("Testing Spectral")
	val spectral = SpectralClustering.fit(data.entitySimilarityMatrix.toTypedArray(), 7, 1.0)//TODO: search for best sigma
	val entitiesWithClusterId = data.entities.withIndex().associateWith { (eIndex, _) -> spectral.y[eIndex] }
	val sorting = entitiesWithClusterId.entries.sortedBy { it.value }.map { it.key.index }
	val sortedMatrix = data.entitySimilarityMatrix.sortUsing(sorting).map { it.asList().sortUsing(sorting).toDoubleArray() }

	ImageIO.write(sortedMatrix.toImage(), "png", File(outputDir, "spectral/sorted.png"))

	return
	val clusters = entitiesWithClusterId.entries.groupBy { it.value }.mapValues { it.value.map { it.key.value } }
	clusters.forEach { (cluster, entities) ->
		saveFile(File(outputDir, "spectral/cluster-$cluster.txt"), entities)
	}
}

private fun testDominantSets(data: ClusterInput) {
	println("Testing Dominant sets")
	data.dominantSetClusters().forEachIndexed { index, output ->
		thread {
			saveFile(File(outputDir, "ds/cluster-$index.txt"), output.dominantSet)
		}

		ImageIO.write(output.input.userEntityHeatMap(), "png", File(outputDir, "ds/heatMap-$index.png"))
		ImageIO.write(output.input.entitySimilarityMatrix.toImage(), "png", File(outputDir, "ds/similarities-$index.png"))

		val sim = output.input.entitySimilarityMatrix
		val idx = output.dominantSetWeights.withIndex().sortedByDescending { it.value }.map { it.index }
		val sortedSim = sim.sortUsing(idx).map { it.asList().sortUsing(idx).toDoubleArray() }
		ImageIO.write(sortedSim.toImage { row, col ->
			if (row < output.dominantSet.size || col < output.dominantSet.size) {
				303F / 360F //Dominant set
			} else {
				303F / 360F
				//124F / 360F
			}
		}, "png", File(outputDir, "ds/similarities-sorted-$index.png"))

	}
}

private fun saveFile(file: File, entities: List<Node.Entity>) {
	file.printWriter().use { w ->
		entities.forEach { entity ->
			w.println("${entity.id}: ${entity.name()}")
		}
	}
}
