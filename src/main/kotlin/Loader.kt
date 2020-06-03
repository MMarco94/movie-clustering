import java.io.File
import kotlin.math.log2

interface Loader {
	fun loadGraph(distance: Distance): ClusterInput
}

private val GRAPH_FILE = File("graph.20000")
private const val USER_LIMIT = 20000
private const val ENTITY_LIMIT = 200

object CachedLoader : Loader {
	override fun loadGraph(distance: Distance): ClusterInput {
		return if (!GRAPH_FILE.exists()) {
			DBLoader.loadGraph(distance).apply {
				save(GRAPH_FILE)
			}
		} else {
			FileLoader.loadGraph(distance)
		}
	}

}

object FileLoader : Loader {
	override fun loadGraph(distance: Distance): ClusterInput {
		return ClusterInput.load(GRAPH_FILE).reduce(ENTITY_LIMIT).apply {
			require(this.distanceAlgorithm == distance)
		}
	}
}

object DBLoader : Loader {

	override fun loadGraph(distance: Distance): ClusterInput {
		connection.prepareStatement(
			"""
                CREATE TEMPORARY TABLE TopUsers (PRIMARY KEY (user)) SELECT user, COUNT(*) cnt
                FROM ConsumedEntity ce
          		INNER JOIN ConsumedMovie cm ON ce.idEntity = cm.idEntity
				GROUP BY user
                ORDER BY cnt DESC
                LIMIT ?
            """
		).use { ps ->
			ps.setInt(1, USER_LIMIT)
			ps.execute()
		}
		connection.prepareStatement(
			"""
                CREATE TEMPORARY TABLE TopEntities (PRIMARY KEY (entity)) SELECT entity, COUNT(*) cnt
                FROM ConsumedEntity ce
				INNER JOIN ConsumedMovie cm ON ce.idEntity = cm.idEntity
                GROUP BY entity
                ORDER BY cnt DESC
                LIMIT ?
            """
		).use { ps ->
			ps.setInt(1, ENTITY_LIMIT)
			ps.execute()
		}
		val users = connection.map("SELECT user FROM TopUsers") { Node.User(getLong("user")) }
		val entities = connection.map("SELECT entity FROM TopEntities") { Node.Entity(getLong("entity")) }
		val ret = ClusterInput.Builder(distance, users, entities)

		connection.forEach(
			"""
                SELECT ce.entity, ce.user, COUNT(*) as cnt
                FROM ConsumedEntity ce
                INNER JOIN TopUsers tu ON tu.user = ce.user
                INNER JOIN TopEntities te ON te.entity = ce.entity
                GROUP BY ce.user, ce.entity
            """
		) {
			val count = getDouble("cnt")
			ret.addRelation(
				Node.Entity(getLong("entity")),
				Node.User(getLong("user")),
				log2(count + 1)//0=>0; 1=>1; 2=>log2(3); 3=>2; 7=>3; 15=>4
			)
		}
		return ret.build()
	}

}