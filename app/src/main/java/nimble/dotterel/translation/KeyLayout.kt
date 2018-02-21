// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import kotlin.math.*

data class KeyLayout(val keys: String, val breakKeysStart: Int, val breakKeysEnd: Int)
{
	constructor(leftKeys: String, breakKeys: String, rightKeys: String) :
		this(
			leftKeys + breakKeys + rightKeys,
			leftKeys.length,
			leftKeys.length + breakKeys.length)

	fun parse(strokeStr: String): Stroke
	{
		var strokeKeys: Long = 0

		var i = 0
		var j = 0
		var jEnd = this.breakKeysEnd
		while(i < strokeStr.length && j < jEnd)
		{
			if(strokeStr[i] == this.keys[j])
			{
				++i
				strokeKeys = strokeKeys or (1L shl j)

				// Only allow right side keys after break key
				if(j >= this.breakKeysStart && j < this.breakKeysEnd)
					jEnd = this.keys.length
			}
			else if(strokeStr[i] == '-' && j < this.breakKeysEnd)
			{
				++i
				j = max(j, this.breakKeysStart)
				jEnd = this.keys.length
			}

			++j
		}

		// Bad stroke_string (not ordered properly or invalid keys
		if(i < strokeStr.length)
			strokeKeys = 0

		return Stroke(this, strokeKeys)
	}

	fun parse(strokeStrs: List<String>): List<Stroke> =
		strokeStrs.map({ this.parse(it) })

	fun parseKeys(keys: List<String>): Stroke
	{
		var strokeKeys: Long = 0
		for(key in keys)
		{
			if(key.length == 1 || (key.length == 2 && key[1] == '-'))
			{
				val i = this.keys.lastIndexOf(key[0], breakKeysEnd)
				if(i != -1)
					strokeKeys = strokeKeys or (1L shl i)
			}
			else if(key.length == 2 && key[0] == '-')
			{
				val i = this.keys.indexOf(key[1], breakKeysStart)
				if(i != -1)
					strokeKeys = strokeKeys or (1L shl i)
			}
		}

		return Stroke(this, strokeKeys)
	}

	fun keyString(keys: Long): String
	{
		var string = ""
		for(i in 0 until this.keys.length)
			string += if(keys and (1L shl i) > 0) this.keys[i] else ' '
		return string
	}

	fun rtfcre(keys: Long): String
	{
		var left = ""
		var i = 0
		while(i < this.breakKeysStart)
		{
			if(keys and (1L shl i) > 0)
				left += this.keys[i]
			++i
		}

		var middle = ""
		while(i < this.breakKeysEnd)
		{
			if(keys and (1L shl i) > 0)
				middle += this.keys[i]
			++i
		}

		var right = ""
		while(i < this.keys.length)
		{
			if(keys and (1L shl i) > 0)
				right += this.keys[i]
			++i
		}

		if(middle.length == 0 && right.length > 0)
			middle = "-"

		return left + middle + right
	}
}
