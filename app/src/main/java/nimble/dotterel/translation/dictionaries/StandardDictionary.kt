// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.dictionaries

import nimble.dotterel.translation.Dictionary
import nimble.dotterel.translation.Stroke
import nimble.dotterel.translation.rtfcre

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

open class StandardDictionary : Dictionary
{
	private val entries = mutableMapOf<String, String>()
	private val keySizeCounts = mutableListOf<Int>()
	override val longestKey: Int get() = this.keySizeCounts.size

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

	override fun get(k: List<Stroke>): String? = this.entries[k.rtfcre]
	operator fun set(k: List<Stroke>, v: String)
	{
		val rtfcre = k.rtfcre
		if(rtfcre !in this.entries)
			this.incrementKeySizeCount(k.size)
		this.entries[rtfcre] = v
	}
	fun remove(k: List<Stroke>)
	{
		if(this.entries.remove(k.rtfcre) != null)
			this.decrementKeySizeCount(k.size)
	}

	// Accessing with List<Stroke> keys is slow due to converting to rtfcre.
	// Use these methods if strokes are already in rftcre form.
	operator fun get(rtfcre: String): String? = this.entries[rtfcre]
	operator fun set(rtfcre: String, v: String)
	{
		if(rtfcre !in this.entries)
			this.incrementKeySizeCount(countStrokes(rtfcre))
		this.entries[rtfcre] = v
	}
	fun remove(rtfcre: String)
	{
		if(this.entries.remove(rtfcre) != null)
			this.decrementKeySizeCount(countStrokes(rtfcre))
	}

	val size: Int get() = this.entries.size
}
