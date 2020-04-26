import com.femastudios.entity.entities.AttributeType
import com.femastudios.entity.entities.Locale
import com.femastudios.entity.exceptions.AttributeNotFoundException
import com.femastudios.entity.Entity as DbEntity

sealed class Node {
	data class Entity(val id: Long) : Node() {
		override fun serialize() = "E:$id"

		fun name(): String {
			val entity = DbEntity.Get.byId<DbEntity>(connection, id)
			return try {
				entity
					.get().values().attr<String>(connection, AttributeType.List.FEMAENTITIES_NAME_NAME)
					.getBest(connection, Locale.List.IT)
					.getValue(connection)
			} catch (e: AttributeNotFoundException) {
				entity.uid()
			}
		}

		companion object {
			fun deserialize(str: String): Entity {
				return Node.deserialize(str) as Entity
			}
		}
	}

	data class User(val id: Long) : Node() {
		override fun serialize() = "U:$id"

		companion object {
			fun deserialize(str: String): User {
				return Node.deserialize(str) as User
			}
		}
	}

	abstract fun serialize(): String

	companion object {
		fun deserialize(str: String): Node {
			val split = str.split(":")
			return when (split.first()) {
				"E" -> Entity(split[1].toLong())
				"U" -> User(split[1].toLong())
				else -> throw IllegalArgumentException("Unexpected char ${split.first()}")
			}
		}
	}
}