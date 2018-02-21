// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import kotlin.math.max

class MultiDictionary(dictionaries: List<Dictionary> = listOf()) : Dictionary
{
	var dictionaries: List<Dictionary> = listOf()
		set(v)
		{
			field = v
			this.longestKey = this.dictionaries
				.fold(0, { total, it -> max(total, it.longestKey) })
		}
	override var longestKey: Int = 0
		private set

	init { this.dictionaries = dictionaries }

	override fun get(k: List<Stroke>): String? =
		this.dictionaries.map({ it[k] }).firstOrNull({ it != null })
}
