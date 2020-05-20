// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.dictionaries

import nimble.dotterel.translation.*

class NumbersDictionary(override val keyLayout: KeyLayout) : Dictionary
{
	private val numbers = (
		"123450".map({ Pair(it, keyLayout.parseKeys(listOf(it.toString()))) })
		+ "6789".map({ Pair(it, keyLayout.parseKeys(listOf("-$it"))) }))

	override val longestKey: Int = 1
	override fun get(k: List<Stroke>): String?
	{
		if(k.size != 1)
			return null

		val s = k[0]
		if(s.layout != this.keyLayout || s.isEmpty)
			return null

		val pressedKeys = this.numbers.fold(0L, { acc, it ->
			if(s.keys and it.second.keys == it.second.keys)
				acc or it.second.keys
			else
				acc
		})
		if(pressedKeys != s.keys)
			return null

		val numbersString = this.numbers.joinToString("", transform = {
			if(s.keys and it.second.keys == it.second.keys)
				it.first.toString()
			else
				""
		})

		return "{&$numbersString}"
	}
}
