import com.femastudios.utils.mysql.MySQLUtils
import java.awt.Color
import java.awt.image.BufferedImage

val connection by lazy {
	MySQLUtils.createConnection(
		"127.0.0.1:3306",
		getEnv("FEMA_DATABASE_USERNAME"),
		getEnv("FEMA_DATABASE_PASSWORD")
	).apply {
		MySQLUtils.setSchema(this, "FemaEntities")
	}
}

fun getEnv(key: String): String {
	return System.getProperty(key) ?: System.getenv(key) ?: throw IllegalStateException("System variable '$key' not found!")
}

fun List<DoubleArray>.mapOnEach(f: (Double) -> Double): List<DoubleArray> {
	return map {
		it.map(f).toDoubleArray()
	}
}

fun List<DoubleArray>.mapOnEachIndexed(f: (Double, i1: Int, i2: Int) -> Double): List<DoubleArray> {
	return mapIndexed { index1, arr ->
		arr.mapIndexed { index2, d ->
			f(d, index1, index2)
		}.toDoubleArray()
	}
}

fun <T> List<T>.sortUsing(indexes: List<Int>): List<T> {
	require(size == indexes.size)
	return indexes.map { get(it) }
}

operator fun DoubleArray.minus(another: DoubleArray): DoubleArray {
	require(size == another.size)
	return DoubleArray(size) {
		get(it) - another[it]
	}
}

inline fun List<DoubleArray>.toImage(hue: (row: Int, col: Int) -> Float = { _, _ -> 303F / 360F }): BufferedImage {
	val img = BufferedImage(size, first().size, BufferedImage.TYPE_INT_RGB)
	forEachIndexed { y, row ->
		row.forEachIndexed { x, col ->
			assert(col in 0.0..1.0)
			if (col > 0) {
				img.setRGB(x, y, Color.HSBtoRGB(hue(x, y), 1F, col.toFloat()))
			}
		}
	}
	return img
}