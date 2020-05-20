// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.orthographies

import com.eclipsesource.json.Json

import java.io.InputStream

import nimble.dotterel.translation.FileParseException
import nimble.dotterel.translation.Orthography
import nimble.dotterel.util.mapValues

class RegexWithWordListOrthography(
	var replacements: List<Replacement>,
	var suffixAliases: Map<String, String>,
	var wordList: Map<String, Int>
) : Orthography
{
	data class Replacement(val pattern: Regex, var replacement: String)

	override fun match(a: String, b: String): Orthography.Result?
	{
		// uffff is an non-character (shouldn't appear in any real unicode strings)
		val s = a + '\uffff' + b

		val leftWordPattern = Regex("(\\S*)$")
		val simpleJoin = leftWordPattern.find(a)?.value + b

		val candidates = (
			listOfNotNull(
				if(simpleJoin in this.wordList)
					Orthography.Result(a.length, simpleJoin)
				else
					null,
				this.suffixAliases[b]?.let({ this.match(a, it) })
			)
			+ this.replacements.mapNotNull({
				val match = it.pattern.find(s)
				if(match != null)
					Orthography.Result(
						a.length - match.range.start,
						it.pattern.replace(s, it.replacement).substring(match.range.start))
				else
					null
			}))

		return candidates.minBy({ this.wordList[it.text] ?: Int.MAX_VALUE })
	}

	companion object
	{
		fun fromJson(input: InputStream): RegexWithWordListOrthography =
			try
			{
				val json = input.bufferedReader()
					.use({ Json.parse(it) })
					.asObject()

				RegexWithWordListOrthography(
					json.get("regex").asArray().map({
						it.asObject().let({ it2 ->
							Replacement(
								Regex(
									it2.get("l").asString()
										+ "\uffff"
										+ it2.get("r").asString(),
									RegexOption.IGNORE_CASE),
								it2.get("s").asString())
						})
					}),
					json.get("suffixAliases").asObject()
						.mapValues({ it.value.asString() }),
					json.get("wordList").asObject()
						.mapValues({ it.value.asInt() })
				)
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
