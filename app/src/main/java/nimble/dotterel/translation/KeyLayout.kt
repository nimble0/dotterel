// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import kotlin.math.*

class KeyLayout(
	leftKeys: String,
	breakKeys: String,
	rightKeys: String,
	// Keys that represent combinations of other keys
	// eg/ "1-" = ["#", "S-"], "2-" = ["#", "T-"]
	mappedKeys: Map<String, List<String>>)
{
	data class RtfcreKey(
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

	val rtfcreKeys: List<RtfcreKey>
	val breakKeys: Pair<Int, Int>

	init
	{
		val rtfcreKeys = mutableListOf<RtfcreKey>()

		fun parseKeys(keys: String, startKeyI: Int, side: Boolean): Int
		{
			var keyI = startKeyI
			for(key in keys)
			{
				val keyStr = (if(side) "-" else "") + key + (if(!side) "-" else "")
				if(keyStr in mappedKeys)
					rtfcreKeys.add(RtfcreKey(key, 0L, 0L, false))
				else
					rtfcreKeys.add(RtfcreKey(key, 1L shl keyI++, 0L, true))
			}

			return keyI
		}

		var keyI = parseKeys(leftKeys, 0, false)
		keyI = parseKeys(breakKeys, keyI, false)
		parseKeys(rightKeys, keyI, true)

		this.rtfcreKeys = rtfcreKeys
		this.breakKeys = Pair(leftKeys.length, leftKeys.length + breakKeys.length)

		for(mappedKey in mappedKeys)
		{
			val i = this.findKey(mappedKey.key)
			var combinationKey = mappedKey.value.fold(
				0L,
				{ acc, it -> acc or this.parseKey(it) })
			this.rtfcreKeys[i].keys = combinationKey

			for(key in this.rtfcreKeys)
				key.excludeConflictingKeys(combinationKey)
		}
	}

	fun parse(strokeStr: String): Stroke
	{
		var strokeKeys: Long = 0

		var strokeKeyI = 0
		var keyI = 0
		var keyEnd = this.breakKeys.second
		while(strokeKeyI < strokeStr.length && keyI < keyEnd)
		{
			if(strokeStr[strokeKeyI] == this.rtfcreKeys[keyI].char)
			{
				++strokeKeyI
				strokeKeys = strokeKeys or this.rtfcreKeys[keyI].keys

				// Only allow right side keys after break key
				if(keyI >= this.breakKeys.first && keyI < this.breakKeys.second)
					keyEnd = this.rtfcreKeys.size
			}
			else if(strokeStr[strokeKeyI] == '-' && keyI < this.breakKeys.second)
			{
				++strokeKeyI
				keyI = max(keyI, this.breakKeys.first)
				keyEnd = this.rtfcreKeys.size
			}

			++keyI
		}

		// Bad stroke_string (excludeKeys ordered properly or invalid keys
		if(strokeKeyI < strokeStr.length)
			strokeKeys = 0

		return Stroke(this, strokeKeys)
	}

	fun parse(strokeStrs: List<String>): List<Stroke> =
		strokeStrs.map({ this.parse(it) })

	private fun findKey(key: String): Int
	{
		return when
		{
			key.length == 1 || (key.length == 2 && key[1] == '-') ->
				this.rtfcreKeys.subList(0, breakKeys.second)
					.indexOfFirst({ it.char == key[0] })
			key.length == 2 && key[0] == '-' ->
			{
				val i = this.rtfcreKeys
					.subList(breakKeys.first, this.rtfcreKeys.size)
					.indexOfFirst({ it.char == key[1] })
				i + if(i != -1) breakKeys.first else 0
			}
			else ->
				-1
		}
	}
	private fun parseKey(key: String): Long =
		this.rtfcreKeys.getOrNull(this.findKey(key))?.keys ?: 0L

	fun parseKeys(keys: List<String>): Stroke
	{
		var strokeKeys: Long = 0
		for(key in keys)
			strokeKeys = strokeKeys or parseKey(key)

		return Stroke(this, strokeKeys)
	}

	fun keyString(keys: Long): String =
		this.rtfcreKeys.joinToString(separator = "", transform =
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
		var left = this.rtfcreKeys
			.subList(0, breakKeys.first)
			.filter({ it.test(keys) })
			.map({ it.char })
		var middle = this.rtfcreKeys
			.subList(breakKeys.first, breakKeys.second)
			.filter({ it.test(keys) })
			.map({ it.char })
		var right = this.rtfcreKeys
			.subList(breakKeys.second, this.rtfcreKeys.size)
			.filter({ it.test(keys) })
			.map({ it.char })

		return (left
				+ (if(middle.isEmpty() && right.isNotEmpty()) listOf('-') else middle)
				+ right)
			.joinToString(separator = "")
	}

	override fun equals(other: Any?) =
		this === other
			|| (other is KeyLayout
			&& this.rtfcreKeys == other.rtfcreKeys
			&& this.breakKeys == other.breakKeys)

	override fun hashCode(): Int
	{
		val prime = 31
		var result = 1
		result = prime * result + this.rtfcreKeys.hashCode()
		result = prime * result + this.breakKeys.hashCode()
		return result
	}
}
