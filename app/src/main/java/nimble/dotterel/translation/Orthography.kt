// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

val NULL_ORTHOGRAPHY = Orthography(listOf())

data class Orthography(var replacements: List<Replacement>)
{
	data class Replacement(val pattern: Regex, var replacement: String)

	data class Result(val backspaces: Int, val text: String)

	fun apply(a: String, b: String): Result?
	{
		// uffff is an non-character (shouldn't appear in any real unicode strings)
		val s = a + '\uffff' + b
		for(r in this.replacements)
		{
			val match = r.pattern.find(s)
			if(match != null)
				return Result(
					a.length - match.range.start,
					r.pattern.replace(s, r.replacement).substring(match.range.start))
		}

		return null
	}
}
