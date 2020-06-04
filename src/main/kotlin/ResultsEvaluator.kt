import com.femastudios.download.HttpDownloader
import com.femastudios.entity.Entity
import com.femastudios.entity.EntityUtils
import com.femastudios.entity.entities.Movie
import com.femastudios.entity.entities.User
import com.femastudios.json.toJsonString
import com.femastudios.utils.mysql.asIterable
import java.io.File
import java.util.*
import javax.imageio.ImageIO

fun readCluster(file: File): List<Node.Entity> {
	return file.useLines { line ->
		line.map { Node.Entity(it.split(":")[0].toLong()) }.toList()
	}
}

fun main() {
	val clusterAlgorithm = "spectral.8192"
	//val clusterAlgorithm = "ds.8192"

	val clusters = File(outputDir, clusterAlgorithm).listFiles()!!.mapNotNull { file ->
		val matchResult = "cluster-(\\d+)\\.txt".toRegex().matchEntire(file.name)
		if (matchResult != null) {
			matchResult.groupValues[1].toInt() to readCluster(file)
		} else null
	}.toMap()

	/*val graph = FileLoader.loadGraph(CosinDistance)
	println("Average distances:")
	val distances = graph.subClusterDistances(clusters.values.toList())
	ImageIO.write(distances.asList().toImage(), "png", File(outputDir, "distances.png"))

	println(distances.map { it.average() }.average())

	println("Densities:")
	clusters.values.forEach { entities ->
		println(graph.intersection(entities.toSet()).density())
	}
	println("\n\n\n")*/

	println("Sizes:")
	clusters.values.forEach { entities ->
		println(entities.size)
	}
	println("\n\n\n")



	similarToPromped(clusters)
	//similarToAlreadyWatched(clusters)
}

private fun similarToPromped(clusters: Map<Int, List<Node.Entity>>) {
	val scanner = Scanner(System.`in`)
	while (true) {
		println("Insert an movie name, id or uid:")
		val line = scanner.nextLine()
		val entity = when {
			line.toLongOrNull() != null -> Node.Entity(line.toLong())
			EntityUtils.isUID(line) -> Node.Entity(Entity.Get.byUID<Entity>(connection, line).id())
			else -> search(line)?.let { Node.Entity(it.id()) }
		}

		if (entity != null) {
			println("Searching for ${entity.name()}")
			val cluster = getClusterForEntity(clusters, entity)
			if (cluster == null) {
				println("Not found :(")
			} else {
				println("Found in cluster ${cluster.key}. Neighbors:")
				getSimilarEntities(cluster, entity).take(50).forEachIndexed { index, otherEntity ->
					println("$index) ${otherEntity.name()}")
				}
			}
		}
	}
}

private fun search(query: String): Movie? {
	val movieUid = HttpDownloader("https://api.everyfad.com/explore/block").apply {
		addParam("start", 0)
		addParam(
			"explore_params", mapOf(
				"country" to "ITA",
				"include_adult" to false
			).toJsonString()
		)
		addParam(
			"explore_block", mapOf(
				"fema_explore_block_type_id" to "search",
				"entity_types" to listOf("Movie"),
				"extra" to mapOf(
					"query" to query,
					"context" to "MOVIESFAD",
					"locale" to "ita-IT"
				)
			).toJsonString()
		)
		addParam("count", 1)
		putHeader("Fema-Device-Id", "movie_clustering_tester")
		putHeader("Fema-Device-Name", "Movie Clustering Tester")
		putHeader("Fema-Device-Codename", "movie_clustering_tester")
		putHeader("Fema-Device-Type", "computer")
		putHeader("Fema-OS-Name", "Android")
		putHeader("Fema-OS-Version", "5")
		putHeader("Fema-Client-ID", "tvseries")
		putHeader("Fema-Client-Version", "179")
		putHeader("Fema-To-Array", "Version:LAST")
	}.downloadJsonObject().jsonObjectAt("items").keys.firstOrNull()
	return movieUid?.let { Movie.Get.byUID(connection, it) }
}

private fun similarToAlreadyWatched(clusters: Map<Int, List<Node.Entity>>) {
	val skipFirst = false
	val sawMovies = connection.prepareStatement("SELECT ce.entity FROM ConsumedEntity ce INNER JOIN ConsumedMovie cm ON ce.idEntity = cm.idEntity WHERE ce.user = ?").use { ps ->
		ps.setLong(1, User.Get.byCurrentUsername(connection, "MMarco").id())
		ps.executeQuery().use { rs ->
			rs.asIterable().map {
				Node.Entity(it.getLong("entity"))
			}
		}
	}
	sawMovies.distinct().associateWith {
		getClusterForEntity(clusters, it)
	}.filterValues { it != null && (!skipFirst || it.key > 0) }.mapValues { it.value!! }.forEach { (entity, cluster) ->
		println("${entity.name()} (cluster ${cluster.key}):")
		getSimilarEntities(cluster, entity).take(50).forEachIndexed { index, otherEntity ->
			println("\t$index) ${otherEntity.name()} (${otherEntity.id})")
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