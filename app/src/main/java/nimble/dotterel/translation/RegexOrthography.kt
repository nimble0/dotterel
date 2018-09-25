// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import com.eclipsesource.json.JsonArray

class RegexOrthography(
	var replacements: List<Replacement>
) : Orthography
{
	data class Replacement(val pattern: Regex, var replacement: String)

	override fun match(a: String, b: String): Orthography.Result?
	{
		// uffff is an non-character (shouldn't appear in any real unicode strings)
		val s = a + '\uffff' + b
		for(r in this.replacements)
		{
			val match = r.pattern.find(s)
			if(match != null)
				return Orthography.Result(
					a.length - match.range.start,
					r.pattern.replace(s, r.replacement).substring(match.range.start))
		}

		return null
	}

	companion object
	{
		fun fromJson(json: JsonArray) =
			RegexOrthography(json.map({
				it.asObject().let({ it2 ->
					Replacement(
						Regex(it2.get("l").asString()
							+ "\uffff"
							+ it2.get("r").asString()),
						it2.get("s").asString())
				})
			}))
	}
}
