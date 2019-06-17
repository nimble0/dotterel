// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.dictionaries

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

	operator fun get(k: String): String? = this.entries[k]

	operator fun set(k: String, v: String)
	{
		val translation = this.entries[k]

		if(translation == null)
			this.incrementKeySizeCount(countStrokes(k))

		this.entries[k] = v
	}
	fun remove(k: String)
	{
		this.entries.remove(k) ?: return

		this.decrementKeySizeCount(countStrokes(k))
	}
}
