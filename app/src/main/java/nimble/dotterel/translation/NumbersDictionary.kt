// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

class NumbersDictionary : Dictionary
{
	override val longestKey: Int = 1
	override fun get(k: List<Stroke>): String?
	{
		if(k.size != 1)
			return null

		val s = k[0]
		if(s.keys == 0L)
			return null

		val numbers = (
			"123450".map({ Pair(it, s.layout.parseKeys(listOf(it.toString()))) })
			+ "6789".map({ Pair(it, s.layout.parseKeys(listOf("-$it"))) }))

		val pressedKeys = numbers.fold(0L, { acc, it ->
			if(s.keys and it.second.keys == it.second.keys)
				acc or it.second.keys
			else
				acc
		})
		if(pressedKeys != s.keys)
			return null

		val numbersString = numbers.joinToString("", transform = {
			if(s.keys and it.second.keys == it.second.keys)
				it.first.toString()
			else
				""
		})

		return "{&$numbersString}"
	}
}
