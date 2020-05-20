// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import nimble.dotterel.util.CaseInsensitiveString

interface Dictionary
{
	val keyLayout: KeyLayout
	val longestKey: Int
	operator fun get(k: List<Stroke>): String?
}

interface MutableDictionary : Dictionary
{
	operator fun set(k: List<Stroke>, v: String)
	fun remove(k: List<Stroke>)
}

interface ReverseDictionary : Dictionary
{
	fun reverseGet(s: String): Set<List<Stroke>>
	fun findTranslations(s: CaseInsensitiveString): Set<String>
}
