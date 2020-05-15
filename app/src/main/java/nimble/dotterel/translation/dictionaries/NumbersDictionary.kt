// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.dictionaries

import nimble.dotterel.translation.*

class NumbersDictionary(
	override val keyLayout: KeyLayout,
	private val reverseKeys: Stroke = keyLayout.parse("-EU"),
	private val doubleKeys: Stroke = keyLayout.parse("-D"),
	private val hundredKeys: Stroke = keyLayout.parse("-Z")
) : Dictionary
{
	private val numbers = (
		"123450".map({ Pair(it, keyLayout.parseKeys(listOf(it.toString()))) })
		+ "6789".map({ Pair(it, keyLayout.parseKeys(listOf("-$it"))) }))


	fun isNumberStroke(s: Stroke): Boolean
	{
		fun combineKeys(keys: Long, combine: Long): Long =
			if(s.keys and combine == combine)
				keys or combine
			else
				keys

		val numberKeys = this.numbers.fold(0L, { acc, it -> combineKeys(acc, it.second.keys) })
		val modifierKeys = 0L
			.let({ combineKeys(it, this.reverseKeys.keys) })
			.let({ combineKeys(it, this.doubleKeys.keys) })
			.let({ combineKeys(it, this.hundredKeys.keys) })

		return numberKeys != 0L && (numberKeys or modifierKeys == s.keys)
	}

	override val longestKey: Int = 1
	override fun get(k: List<Stroke>): String?
	{
		if(k.size != 1)
			return null

		val s = k[0]
		if(s.layout != this.keyLayout || s.isEmpty || !isNumberStroke(s))
			return null

		val numbersString = StringBuilder()
		for(n in this.numbers)
			if(s.test(n.second))
				numbersString.append(n.first.toString())

		if(!this.reverseKeys.isEmpty && s.test(this.reverseKeys) && numbersString.length > 1)
			numbersString.reverse()

		if(!this.doubleKeys.isEmpty && s.test(this.doubleKeys))
			numbersString.insert(0, Character.toChars(numbersString.codePointAt(0)))

		if(!this.hundredKeys.isEmpty && s.test(this.hundredKeys))
			numbersString.append("00")

		return "{&$numbersString}"
	}
}
