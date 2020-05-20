// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import com.eclipsesource.json.JsonObject

import kotlin.math.*

import nimble.dotterel.util.toJson

val EMPTY_KEY_LAYOUT = KeyLayout("", mapOf())

data class KeyLayout(
	val keysString: String,
	// Keys that represent combinations of other keys
	// eg/ "1-" = ["#", "S-"], "2-" = ["#", "T-"]
	val mappedKeys: Map<String, List<String>> = mapOf())
{
	data class Key(
		val char: Char,
		var keys: Long,
		// This key isn't outputted in rtfcre when these keys are also active
		var excludeKeys: Long,
		// This key represents a single key, not a combination of keys
		val pure: Boolean)
	{
		fun test(keys: Long) =
			(this.keys and keys) == this.keys && (this.excludeKeys and keys) == 0L
		fun excludeConflictingKeys(keys: Long)
		{
			if(this.pure
				&& this.keys and keys != 0L
				&& this.keys != keys)
				this.excludeKeys = this.excludeKeys or (keys and this.keys.inv())
		}
	}

	val keys: List<Key>
	val breakKeys: Pair<Int, Int>

	init
	{
		var keyI = 0
		fun toRtfcreKey(key: Char, keyString: String) =
			if(keyString in mappedKeys)
				Key(key, 0L, 0L, false)
			else
				Key(key, 1L shl keyI++, 0L, true)

		val keySections = keysString.split('-')

		if(keySections.size > 3)
			throw(IllegalArgumentException("keysString cannot contain more than 2 hyphens"))

		val leftKeys = keySections[0].map({ toRtfcreKey(it, "$it-") })
		val breakKeys = keySections.getOrNull(1)
			?.map({ toRtfcreKey(it, "$it") }) ?: listOf()
		val rightKeys = keySections.getOrNull(2)
			?.map({ toRtfcreKey(it, "-$it") }) ?: listOf()

		this.keys = leftKeys + breakKeys + rightKeys
		this.breakKeys = Pair(leftKeys.size, leftKeys.size + breakKeys.size)

		for(mappedKey in mappedKeys)
		{
			val i = this.findKey(mappedKey.key)
			if(i == -1)
				throw(IllegalArgumentException("mappedKeys contains key not in keysString"))
			val combinationKey = mappedKey.value.fold(
				0L,
				{ acc, it -> acc or this.parseKey(it) })
			this.keys[i].keys = combinationKey

			for(key in this.keys)
				key.excludeConflictingKeys(combinationKey)
		}
	}

	val sections: List<List<Key>> get() =
		listOf(this.keys.subList(0, this.breakKeys.first),
			this.keys.subList(this.breakKeys.first, this.breakKeys.second),
			this.keys.subList(this.breakKeys.second, this.keys.size))

	// All keys that aren't combinations of other keys
	val pureKeysString: String get() =
		this.sections.map({ keys ->
			keys.mapNotNull({ if(it.pure) it.char else null }).joinToString("")
		}).joinToString("-")

	val pureKeysList: List<String> get()
	{
		val keySections = this.sections.map({ keys ->
			keys.mapNotNull({ if(it.pure) it.char.toString() else null })
		})

		return (keySections[0].map({ "$it-" })
			+ keySections[1]
			+ keySections[2].map({ "-$it" }))
	}

	fun parse(strokeString: String): Stroke
	{
		var strokeKeys: Long = 0

		var strokeKeyI = 0
		var keyI = 0
		var keyEnd = this.breakKeys.second
		while(strokeKeyI < strokeString.length && keyI < keyEnd)
		{
			if(strokeString[strokeKeyI] == this.keys[keyI].char)
			{
				++strokeKeyI
				strokeKeys = strokeKeys or this.keys[keyI].keys

				// Only allow right side keys after break key
				if(keyI >= this.breakKeys.first && keyI < this.breakKeys.second)
					keyEnd = this.keys.size
			}
			else if(strokeString[strokeKeyI] == '-' && keyI < this.breakKeys.second)
			{
				++strokeKeyI
				keyI = max(keyI, this.breakKeys.first)
				keyEnd = this.keys.size
			}

			++keyI
		}

		// Bad stroke_string (excludeKeys ordered properly or invalid keys
		if(strokeKeyI < strokeString.length)
			strokeKeys = 0

		return Stroke(this, strokeKeys)
	}

	fun parse(strokeStrings: List<String>): List<Stroke> =
		strokeStrings.map({ this.parse(it) })

	private fun findKey(key: String): Int
	{
		return when
		{
			key.length == 1 || (key.length == 2 && key[1] == '-') ->
				this.keys.subList(0, breakKeys.second)
					.indexOfFirst({ it.char == key[0] })
			key.length == 2 && key[0] == '-' ->
			{
				val i = this.keys
					.subList(breakKeys.first, this.keys.size)
					.indexOfFirst({ it.char == key[1] })
				i + if(i != -1) breakKeys.first else 0
			}
			else ->
				-1
		}
	}
	private fun parseKey(key: String): Long =
		this.keys.getOrNull(this.findKey(key))?.keys ?: 0L

	fun parseKeys(keys: List<String>): Stroke
	{
		var strokeKeys: Long = 0
		for(key in keys)
			strokeKeys = strokeKeys or parseKey(key)

		return Stroke(this, strokeKeys)
	}

	fun pureKeysString(keys: Long): String =
		this.keys.joinToString(separator = "", transform =
		{
			if(it.pure && it.keys and keys != 0L)
				it.char.toString()
			else if(it.pure)
				" "
			else
				""
		})

	fun rtfcre(keys: Long): String
	{
		val result = StringBuilder(this.keys.size)
		for(i in 0 until this.breakKeys.first)
		{
			val k = this.keys[i]
			if(k.test(keys))
				result.append(k.char)
		}

		var addDivider = true
		for(i in this.breakKeys.first until this.breakKeys.second)
		{
			val k = this.keys[i]
			if(k.test(keys))
			{
				result.append(k.char)
				addDivider = false
			}
		}

		if(addDivider)
			result.append('-')

		for(i in this.breakKeys.second until this.keys.size)
		{
			val k = this.keys[i]
			if(k.test(keys))
				result.append(k.char)
		}

		if(result.last() == '-')
			result.deleteCharAt(result.length - 1)

		return result.toString()
	}

	constructor(json: JsonObject) :
		this(
			keysString = json.getString("keysString", ""),
			mappedKeys = json.get("mappedKeys")?.asObject()?.let({ map ->
				val mappedKeys = mutableMapOf<String, List<String>>()
				for(kv in map)
					mappedKeys[kv.name] = kv.value.asArray().map({ it.asString() })
				mappedKeys.toMap()
			}) ?: mapOf()
		)

	fun toJson() = JsonObject().also({ jsonKeyLayout ->
			jsonKeyLayout.set("keysString", this.keysString)
			jsonKeyLayout.set("mappedKeys", JsonObject().also({ jsonMappedKeys ->
				for((key, mapped) in this.mappedKeys)
					jsonMappedKeys.add(key, mapped.toJson())
			}))
		})
}
