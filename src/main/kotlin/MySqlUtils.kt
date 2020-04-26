import com.femastudios.utils.mysql.MySQLConnection
import java.sql.ResultSet

inline fun <T> MySQLConnection<*>.map(query: String, f: ResultSet.() -> T): List<T> {
	return prepareStatement(query).use {
		it.executeQuery().map(f)
	}
}

inline fun MySQLConnection<*>.forEach(query: String, f: ResultSet.() -> Unit) {
	return prepareStatement(query).use {
		it.executeQuery().forEach(f)
	}
}

inline fun <T> ResultSet.map(f: ResultSet.() -> T): List<T> {
	val list = mutableListOf<T>()
	forEach { list.add(f(this)) }
	return list
}

inline fun ResultSet.forEach(f: ResultSet.() -> Unit) {
	return use {
		while (next()) f(this)
	}
}
