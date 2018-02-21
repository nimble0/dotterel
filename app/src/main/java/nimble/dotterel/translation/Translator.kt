// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

class Translator(var dictionary: Dictionary = MultiDictionary())
{
	fun apply(s: Stroke): List<Any>
	{
		val translation = this.dictionary[listOf(s)] ?: s.rtfcre
		return listOf(FormattedText(0, " $translation"))
	}
}
