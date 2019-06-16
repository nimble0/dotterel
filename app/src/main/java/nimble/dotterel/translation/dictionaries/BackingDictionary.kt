// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.dictionaries

import java.util.Locale

import nimble.collections.BasicMultiMap
import nimble.dotterel.util.CaseInsensitiveString

private fun countStrokes(s: String): Int
{
	var n = 0
	var i = 0
	while(i != -1)
	{
		++n
		i = s.indexOf('/', i + 1)
	}
	return n
}

// Steno dictionary that uses String keys instead of List<Stroke>
// so dictionaries can be loaded more quickly by lazily converting
// String keys to List<Stroke>.
open class BackingDictionary
{
	private val entries = mutableMapOf<String, String>()
	private val keySizeCounts = mutableListOf<Int>()
	val longestKey: Int get() = this.keySizeCounts.size

	private val reverseEntries = BasicMultiMap<String, String>()
	private val caseInsensitiveReverseEntries =
		BasicMultiMap<CaseInsensitiveString, String>()

	private fun incrementKeySizeCount(s: Int)
	{
		while(this.keySizeCounts.size < s)
			this.keySizeCounts.add(0)
		++this.keySizeCounts[s - 1]
	}
	private fun decrementKeySizeCount(s: Int)
	{
		--this.keySizeCounts[s - 1]
		while(this.keySizeCounts.size > 0 && this.keySizeCounts.last() == 0)
			this.keySizeCounts.removeAt(this.keySizeCounts.lastIndex)
	}

	private fun removeReverseTranslation(strokes: String, translation: String)
	{
		this.reverseEntries.remove(translation, strokes)
		if(this.reverseEntries[translation].isEmpty())
			this.caseInsensitiveReverseEntries.remove(
				CaseInsensitiveString(translation),
				translation)
	}

	operator fun get(k: String): String? = this.entries[k]

	operator fun set(k: String, v: String)
	{
		val oldTranslation = this.entries.put(k, v)
		if(oldTranslation == null)
			this.incrementKeySizeCount(countStrokes(k))
		else
			this.removeReverseTranslation(k, oldTranslation)

		this.reverseEntries.put(v, k)
		if(v != v.toLowerCase(Locale.getDefault()))
			this.caseInsensitiveReverseEntries
				.put(CaseInsensitiveString(v.toLowerCase(Locale.getDefault())), v)
	}

	fun remove(k: String)
	{
		val translation = this.entries.remove(k) ?: return

		this.decrementKeySizeCount(countStrokes(k))
		this.removeReverseTranslation(k, translation)
	}

	fun reverseGet(s: String): Set<String> = this.reverseEntries[s].toSet()
	fun findTranslations(s: CaseInsensitiveString): Set<String> =
		(this.caseInsensitiveReverseEntries[s]
			+ s.value.toLowerCase(Locale.getDefault()).let({
				if(this.reverseEntries.containsKey(it))
					setOf(it)
				else
					setOf()
			}))
}
