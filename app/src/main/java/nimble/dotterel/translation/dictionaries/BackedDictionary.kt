// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.dictionaries

import nimble.dotterel.translation.*
import nimble.dotterel.util.CaseInsensitiveString

open class BackedDictionary(
	override val keyLayout: KeyLayout,
	val backingDictionary: BackingDictionary
) :
	Dictionary,
	MutableDictionary,
	ReverseDictionary
{
	override val longestKey: Int get() = this.backingDictionary.longestKey

	override fun get(k: List<Stroke>): String? = this.backingDictionary[k.rtfcre]

	override fun set(k: List<Stroke>, v: String)
	{
		this.backingDictionary[k.rtfcre] = v
	}
	override fun remove(k: List<Stroke>)
	{
		this.backingDictionary.remove(k.rtfcre)
	}

	operator fun get(k: String): String? = this.backingDictionary[k]
	operator fun set(k: String, v: String)
	{
		this.backingDictionary[k] = v
	}
	fun remove(k: String) = this.backingDictionary.remove(k)

	override fun reverseGet(s: String): Set<List<Stroke>> =
		this.backingDictionary.reverseGet(s)
			.map({ this.keyLayout.parse(it.split("/")) })
			.toSet()
	override fun findTranslations(s: CaseInsensitiveString): Set<String> =
		this.backingDictionary.findTranslations(s)
}
