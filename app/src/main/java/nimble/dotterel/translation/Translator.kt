// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

private const val TRANSLATION_HISTORY_SIZE = 100
private val PREFIX_STROKES = listOf<String>()
private val SUFFIX_STROKES = listOf("-Z", "-D", "-S", "-G")

class Translator(var dictionary: Dictionary = MultiDictionary())
{
	private val history = mutableListOf<Translation>()

	private data class Affix(val prefix: Stroke, val suffix: Stroke)

	private fun permutateAffixes(prefixes: List<Stroke>, suffixes: List<Stroke>)
		: List<Affix>
	{
		val affixes = mutableListOf<Affix>()
		affixes.addAll(prefixes.map({ Affix(it, Stroke(it.layout, 0)) }))
		affixes.addAll(suffixes.map({ Affix(Stroke(it.layout, 0), it) }))
		for(s in prefixes)
			for(s2 in suffixes)
				affixes.add(Affix(s, s2))

		return affixes
	}

	private val keyLayout = KeyLayout("STKPWHR", "AO*EU", "FRPBLGTSDZ")
	private val affixStrokes = permutateAffixes(
		PREFIX_STROKES.map({ this.keyLayout.parse(it) }),
		SUFFIX_STROKES.map({ this.keyLayout.parse(it) }))

	private fun lookupWithAffixFolding(strokes: List<Stroke>): String?
	{
		val tryStrokes = strokes.toMutableList()
		for(a in this.affixStrokes)
		{
			tryStrokes[0] = strokes[0]
			tryStrokes[tryStrokes.lastIndex] = strokes.last() - a.suffix
			tryStrokes[0] = tryStrokes[0] - a.prefix

			val rawTranslation = this.dictionary[tryStrokes]
			if(rawTranslation != null)
			{
				val prefix = this.dictionary[listOf(a.prefix)] ?: ""
				val suffix = this.dictionary[listOf(a.suffix)] ?: ""
				return "$prefix $rawTranslation $suffix"
			}
		}

		return null
	}
	private fun lookup(
		strokes: List<Stroke>,
		replaces: List<Translation>,
		fullMatch: Boolean
	): Translation?
	{
		var raw = this.dictionary[strokes]
		if(raw != null)
			return Translation(strokes, replaces, raw, true)

		if(!fullMatch)
			raw = this.lookupWithAffixFolding(strokes)
		if(raw != null)
			return Translation(strokes, replaces, raw, false)

		return null
	}

	fun translate(s: Stroke): Translation
	{
		var translation = this.lookup(listOf(s), listOf(), false)
			?: Translation(listOf(s), listOf(), s.rtfcre, false)

		val strokes = mutableListOf(s)
		val replaces = mutableListOf<Translation>()
		for(h in this.history.reversed())
		{
			strokes.addAll(0, h.strokes)
			replaces.add(0, h)

			if(strokes.size > this.dictionary.longestKey)
				break

			translation = this.lookup(
					strokes.toList(),
					replaces.toList(),
					translation.fullMatch)
				?: translation
		}

		return translation
	}

	fun apply(s: Stroke): List<Any>
	{
		val translation = this.translate(s)
		this.history.subList(
			this.history.size - translation.replaces.size,
			this.history.size).clear()
		this.history.add(translation)
		val drop = this.history.size - TRANSLATION_HISTORY_SIZE
		if(drop > 0)
			this.history.subList(0, drop).clear()

		return listOf(FormattedText(
			translation.replaces.joinToString(
				separator = "", transform = { " ${it.raw}" }).length,
			" ${translation.raw}"))
	}
}
