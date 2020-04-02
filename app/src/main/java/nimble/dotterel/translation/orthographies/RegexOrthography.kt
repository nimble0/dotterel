// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.orthographies

import com.eclipsesource.json.Json

import java.io.InputStream

import nimble.dotterel.translation.FileParseException
import nimble.dotterel.translation.Orthography

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
		fun fromJson(input: InputStream): RegexOrthography =
			try
			{
				val json = input.bufferedReader()
					.use({ Json.parse(it) })
					.asArray()

				RegexOrthography(json.map({
					it.asObject().let({ it2 ->
						Replacement(
							Regex(
								it2.get("l").asString()
									+ "\uffff"
									+ it2.get("r").asString(),
								RegexOption.IGNORE_CASE),
							it2.get("s").asString())
					})
				}))
			}
			catch(e: com.eclipsesource.json.ParseException)
			{
				throw FileParseException("Invalid JSON", e)
			}
			catch(e: java.lang.NullPointerException)
			{
				throw FileParseException("Missing value", e)
			}
			catch(e: java.lang.UnsupportedOperationException)
			{
				throw FileParseException("Invalid type", e)
			}
	}
}
