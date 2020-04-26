import com.femastudios.entity.entities.ConsumedEntity
import com.femastudios.entity.entities.User
import java.io.File
import java.util.*

fun readCluster(file: File): List<Node.Entity> {
	return file.useLines { line ->
		line.map { Node.Entity(it.split(":")[0].toLong()) }.toList()
	}
}

fun main() {
	val clusterAlgorithm = "spectral"
	val skipFirst = false

	val clusters = File(outputDir, clusterAlgorithm).listFiles()!!.mapNotNull { file ->
		val matchResult = "cluster-(\\d+)\\.txt".toRegex().matchEntire(file.name)
		if (matchResult != null) {
			val clusterId = matchResult.groupValues[1].toInt()
			clusterId to readCluster(file)
		} else null
	}.toMap()


	ConsumedEntity.Get.allByUser(
		connection,
		User.Get.byCurrentUsername(connection, "MMarco")
	).map { Node.Entity(it.get().entity(connection).id()) }.distinct().associateWith {
		getClusterForEntity(clusters, it)
	}.filterValues { it != null && (!skipFirst || it.key > 0) }.mapValues { it.value!! }.forEach { (entity, cluster) ->
		println("${entity.name()} (cluster ${cluster.key}):")
		getSimilarEntities(cluster, entity).take(15).forEachIndexed { index, otherEntity ->
			println("\t$index) ${otherEntity.name()} (${otherEntity.id})")
		}
	}


	val scanner = Scanner(System.`in`)
	while (true) {
		println("Insert an entity Id:")
		val id = scanner.nextLine().toLongOrNull()
		if (id != null) {
			val entity = Node.Entity(id)
			println("Searching for ${entity.name()}")
			val cluster = getClusterForEntity(clusters, entity)
			if (cluster == null) {
				println("Not found :(")
			} else {
				println("Found in cluster ${cluster.key}. Neighbors:")
				getSimilarEntities(cluster, entity).take(15).forEachIndexed { index, otherEntity ->
					println("$index) ${otherEntity.name()}")
				}
			}
		}
	}
}

private fun getSimilarEntities(cluster: Map.Entry<Int, List<Node.Entity>>, entity: Node.Entity): List<Node.Entity> {
	val indexOf = cluster.value.indexOf(entity)
	val after = cluster.value.drop(indexOf + 1)
	val before = cluster.value.take(indexOf).reversed()
	return (after.withIndex() + before.withIndex()).sortedBy { it.index }.map { it.value }
}

private fun getClusterForEntity(
	clusters: Map<Int, List<Node.Entity>>,
	entity: Node.Entity
) = clusters.entries.singleOrNull { entity in it.value }