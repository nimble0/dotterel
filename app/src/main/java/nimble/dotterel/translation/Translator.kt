// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

private const val TRANSLATION_HISTORY_SIZE = 100

class Translator(var dictionary: Dictionary = MultiDictionary())
{
	private val history = mutableListOf<Translation>()

	private fun lookup(
		strokes: List<Stroke>,
		replaces: List<Translation>
	): Translation?
	{
		var raw = this.dictionary[strokes]
		if(raw != null)
			return Translation(strokes, replaces, raw)

		return null
	}

	fun translate(s: Stroke): Translation
	{
		var translation = this.lookup(listOf(s), listOf())
			?: Translation(listOf(s), listOf(), s.rtfcre)

		val strokes = mutableListOf(s)
		val replaces = mutableListOf<Translation>()
		for(h in this.history.reversed())
		{
			strokes.addAll(0, h.strokes)
			replaces.add(0, h)

			if(strokes.size > this.dictionary.longestKey)
				break

			translation = this.lookup(strokes.toList(), replaces.toList())
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
