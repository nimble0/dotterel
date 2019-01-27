// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import java.text.ParseException

import kotlin.math.min

private val TRANSLATION_PARTS_PATTERN = Regex("\\A(?:(?:[^{}\\\\]|(?:\\\\.))+|(?:\\{(?:[^{}\\\\]|(?:\\\\.))*\\}))")
// META_FORMATTING_PATTERN groups
// 1 - Formatting start
// 2 - No space start/glue
// 3 - Carry transform
// 4 - Text
// 5 - Formatting end
// 6 - No space end
private val META_FORMATTING_PATTERN = Regex("(((?:\\^|&)?)((?:~\\|)?))(.*[^\\^])((\\^?))")

private fun parseTranslationParts(translation: String): List<String>
{
	val parts = mutableListOf<String>()
	var i = 0
	while(true)
	{
		val match = TRANSLATION_PARTS_PATTERN.find(translation.substring(i)) ?: break
		i += match.range.endInclusive + 1
		val s = match.value.trim(' ')
		if(s.isNotEmpty())
			parts.add(s)
	}

	if(i != translation.length)
		throw ParseException("Error parsing translation", i)

	return parts
}

sealed class AliasTranslation
class Actions(val actions: List<Any>) : AliasTranslation()
class TranslationString(val string: String) : AliasTranslation()

private val BASE_ALIASES = mapOf(
	Pair("", Actions(listOf(UnformattedText(formatting = Formatting(
		spaceStart = Formatting.Space.NONE,
		orthography = NULL_ORTHOGRAPHY,
		transformState = Formatting.TransformState.MAIN
	))))),
	Pair("^", Actions(listOf(UnformattedText(formatting = Formatting(
		spaceStart = Formatting.Space.NONE,
		spaceEnd = Formatting.Space.NONE,
		orthographyStart = false,
		orthographyEnd = false
	))))),
	Pair("^^", Actions(listOf(UnformattedText(formatting = Formatting(
		spaceStart = Formatting.Space.NONE,
		spaceEnd = Formatting.Space.NONE,
		orthographyStart = false,
		orthographyEnd = false
	)))))
)

data class TranslationPart(
	val actions: List<Any> = listOf(),
	val replaces: List<HistoryTranslation> = listOf())
{
	val empty: Boolean
		get() = actions.isEmpty() && replaces.isEmpty()

	operator fun plus(b: TranslationPart): TranslationPart =
		TranslationPart(this.actions + b.actions, b.replaces + this.replaces)
}

class TranslationProcessor
{
	val transforms = mutableMapOf<
		String,
		(FormattedText, UnformattedText, Boolean) -> UnformattedText>()
	val commands = mutableMapOf<String, (Translator, String) -> TranslationPart>()
	val aliases: MutableMap<String, AliasTranslation> = BASE_ALIASES.toMutableMap()

	fun resetTransforms()
	{
		this.transforms.clear()
	}
	fun resetCommands()
	{
		this.commands.clear()
	}
	fun resetAliases()
	{
		this.aliases.clear()
		this.aliases.putAll(BASE_ALIASES)
	}

	private fun parseCommand(translator: Translator, commandStr: String)
		: TranslationPart
	{
		var i = commandStr.length
		while(i != -1)
		{
			val name = commandStr.substring(0, i).toLowerCase()
			val arg = commandStr.substring(min(i + 1, commandStr.length))
			val command = this.commands[name]
			if(command != null)
				return command(translator, arg)

			i = commandStr.lastIndexOf(":", i - 1)
		}

		return TranslationPart()
	}

	@Throws(ParseException::class)
	private fun parseTranslationPart(translator: Translator, part: String)
		: TranslationPart
	{
		if(part[0] == '{')
		{
			val inner = part.substring(1, part.length - 1)

			val a = this.aliases[inner]
			when(a)
			{
				is Actions -> return TranslationPart(a.actions)
				is TranslationString -> return this.process(translator, a.string)
			}

			if(inner.isNotEmpty() && inner[0] == '#')
				return TranslationPart(parseKeyCombos(inner.substring(1)))

			val formattingMatch = META_FORMATTING_PATTERN.matchEntire(inner)
			if(formattingMatch != null)
			{
				// If no formatting tokens, it's a command
				if(formattingMatch.groupValues[1].isEmpty()
					&& formattingMatch.groupValues[5].isEmpty())
					return this.parseCommand(translator, inner)

				var spaceStart = Formatting.Space.NORMAL
				var spaceEnd = Formatting.Space.NORMAL
				var orthographyStart = true
				var orthographyEnd = true
				var singleTransformState = Formatting.TransformState.NORMAL

				when(formattingMatch.groupValues[2])
				{
					"^" ->
						spaceStart = Formatting.Space.NONE
					"&" ->
					{
						spaceStart = Formatting.Space.GLUE
						spaceEnd = Formatting.Space.GLUE
						orthographyStart = false
					}
				}

				if(formattingMatch.groupValues[3] == "~|")
					singleTransformState = Formatting.TransformState.CARRY

				val text = formattingMatch.groupValues[4]

				if(formattingMatch.groupValues[6] == "^")
					spaceEnd = Formatting.Space.NONE

				val formatting = Formatting(
					spaceStart = spaceStart,
					spaceEnd = spaceEnd,
					orthography = translator.system.orthography,
					orthographyStart = orthographyStart,
					orthographyEnd = orthographyEnd,
					transformState = singleTransformState
				)

				return TranslationPart(listOf(UnformattedText(0, text, formatting)))
			}

			return TranslationPart()
		}
		else
			return TranslationPart(listOf(UnformattedText(
				0,
				part,
				Formatting(orthography = translator.system.orthography))))
	}

	fun process(translator: Translator, translation: String): TranslationPart
	{
		return parseTranslationParts(translation)
			.map({ this.parseTranslationPart(translator, it) })
			.fold(TranslationPart(), { acc, it -> acc + it })
	}
}
