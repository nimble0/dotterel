// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.util

import com.eclipsesource.json.*

@JvmName("toJsonArray")
fun List<JsonValue>.toJson() = JsonArray()
	.also({
		for(v in this)
			it.add(v)
	})
@JvmName("toBooleanJsonArray")
fun List<Boolean>.toJson() = JsonArray()
	.also({
		for(v in this)
			it.add(v)
	})
@JvmName("toIntJsonArray")
fun List<Int>.toJson() = JsonArray()
	.also({
		for(v in this)
			it.add(v)
	})
@JvmName("toLongJsonArray")
fun List<Long>.toJson() = JsonArray()
	.also({
		for(v in this)
			it.add(v)
	})
@JvmName("toFloatJsonArray")
fun List<Float>.toJson() = JsonArray()
	.also({
		for(v in this)
			it.add(v)
	})
@JvmName("toDoubleJsonArray")
fun List<Double>.toJson() = JsonArray()
	.also({
		for(v in this)
			it.add(v)
	})
@JvmName("toStringJsonArray")
fun List<String>.toJson() = JsonArray()
	.also({
		for(v in this)
			it.add(v)
	})


@JvmName("toJsonObject")
fun Map<String, JsonValue>.toJson() = JsonObject()
	.also({
		for(kv in this)
			it.add(kv.key, kv.value)
	})
@JvmName("toBooleanJsonObject")
fun Map<String, Boolean>.toJson() = JsonObject()
	.also({
		for(kv in this)
			it.add(kv.key, kv.value)
	})
@JvmName("toIntJsonObject")
fun Map<String, Int>.toJson() = JsonObject()
	.also({
		for(kv in this)
			it.add(kv.key, kv.value)
	})
@JvmName("toLongJsonObject")
fun Map<String, Long>.toJson() = JsonObject()
	.also({
		for(kv in this)
			it.add(kv.key, kv.value)
	})
@JvmName("toFloatJsonObject")
fun Map<String, Float>.toJson() = JsonObject()
	.also({
		for(kv in this)
			it.add(kv.key, kv.value)
	})
@JvmName("toDoubleJsonObject")
fun Map<String, Double>.toJson() = JsonObject()
	.also({
		for(kv in this)
			it.add(kv.key, kv.value)
	})
@JvmName("toStringJsonObject")
fun Map<String, String>.toJson() = JsonObject()
	.also({
		for(kv in this)
			it.add(kv.key, kv.value)
	})


fun <T> JsonObject.mapKeys(transform: (JsonObject.Member) -> T) =
	this.associateBy({ transform(it) }, { it.value })
fun <T> JsonObject.mapValues(transform: (JsonObject.Member) -> T) =
	this.associateBy({ it.name }, { transform(it) })


fun JsonObject.getOrNull(name: String) =
	this.get(name)?.let({ if(it.isNull) null else it })

fun JsonObject.setNotNull(name: String, value: JsonValue?)
{
	if(value == null)
		this.remove(name)
	else
		this.set(name, value)
}
fun JsonObject.setNotNull(name: String, value: Boolean?)
{
	if(value == null)
		this.remove(name)
	else
		this.set(name, value)
}
fun JsonObject.setNotNull(name: String, value: Int?)
{
	if(value == null)
		this.remove(name)
	else
		this.set(name, value)
}
fun JsonObject.setNotNull(name: String, value: Long?)
{
	if(value == null)
		this.remove(name)
	else
		this.set(name, value)
}
fun JsonObject.setNotNull(name: String, value: Float?)
{
	if(value == null)
		this.remove(name)
	else
		this.set(name, value)
}
fun JsonObject.setNotNull(name: String, value: Double?)
{
	if(value == null)
		this.remove(name)
	else
		this.set(name, value)
}
fun JsonObject.setNotNull(name: String, value: String?)
{
	if(value == null)
		this.remove(name)
	else
		this.set(name, value)
}


private fun JsonValue.createPathStructure(path: List<String>) =
	path.fold(this.asObject(), { acc, it ->
		(acc.getOrNull(it)
			?: JsonObject().also({ empty -> acc.set(it, empty) })
			).asObject()
	})


fun JsonValue.get(path: List<String>): JsonValue? =
	path.fold(
		this as JsonValue?,
		{ acc, it -> acc
			?.let({ v -> if(v.isNull) null else v })
			?.asObject()
			?.get(it)
		})

fun JsonValue.set(path: List<String>, value: JsonValue)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.set(path.last(), value)
}
fun JsonValue.set(path: List<String>, value: Boolean)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.set(path.last(), value)
}
fun JsonValue.set(path: List<String>, value: Int)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.set(path.last(), value)
}
fun JsonValue.set(path: List<String>, value: Long)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.set(path.last(), value)
}
fun JsonValue.set(path: List<String>, value: Float)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.set(path.last(), value)
}
fun JsonValue.set(path: List<String>, value: Double)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.set(path.last(), value)
}
fun JsonValue.set(path: List<String>, value: String?)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.set(path.last(), value)
}


fun JsonObject.getOrNull(path: List<String>) =
	this.get(path)?.let({ if(it.isNull) null else it })

fun JsonValue.setNotNull(path: List<String>, value: JsonValue?)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.setNotNull(path.last(), value)
}
fun JsonValue.setNotNull(path: List<String>, value: Boolean?)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.setNotNull(path.last(), value)
}
fun JsonValue.setNotNull(path: List<String>, value: Int?)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.setNotNull(path.last(), value)
}
fun JsonValue.setNotNull(path: List<String>, value: Long?)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.setNotNull(path.last(), value)
}
fun JsonValue.setNotNull(path: List<String>, value: Float?)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.setNotNull(path.last(), value)
}
fun JsonValue.setNotNull(path: List<String>, value: Double?)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.setNotNull(path.last(), value)
}
fun JsonValue.setNotNull(path: List<String>, value: String?)
{
	this.createPathStructure(path.subList(0, path.size - 1))
		.setNotNull(path.last(), value)
}
