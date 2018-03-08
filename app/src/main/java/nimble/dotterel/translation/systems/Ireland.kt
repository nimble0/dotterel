// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.systems

import nimble.dotterel.translation.*
import nimble.dotterel.translation.Orthography.Replacement

val IRELAND_LAYOUT = KeyLayout(
	"#1S2TK3PW4HR",
	"5A0O*EU",
	"6FR7PB8LG9TSDZ",
	mapOf(
		Pair("1-", listOf("#-", "S-")),
		Pair("2-", listOf("#-", "T-")),
		Pair("3-", listOf("#-", "P-")),
		Pair("4-", listOf("#-", "H-")),
		Pair("5-", listOf("#-", "A-")),
		Pair("0-", listOf("#-", "O-")),
		Pair("-6", listOf("#-", "-F")),
		Pair("-7", listOf("#-", "-P")),
		Pair("-8", listOf("#-", "-L")),
		Pair("-9", listOf("#-", "-T")))
)

val ENGLISH_ORTHOGRAPHY = Orthography(listOf(
	Replacement(Regex("(?<=[bcdfghjklmnpqrstvwxz] ?)y\uffffs"), "ies"),
	Replacement(Regex("(?<=s|sh|x|z|zh ?)\uffffs"), "es"),
	Replacement(Regex("ie\uffffing"), "ying")
))

val IRELAND_SYSTEM = System(
	keyLayout = IRELAND_LAYOUT,

	orthography = ENGLISH_ORTHOGRAPHY,

	transforms = mapOf(
		Pair("NONE", ::noneTransform),
		Pair("CAPITALISE", ::capitialiseTransform),
		Pair("UPPERCASE", ::upperCaseTransform),
		Pair("LOWERCASE", ::lowerCaseTransform)
	),

	commands = mapOf(
		Pair("RETRO:UNDO", ::undoStroke),
		Pair("MODE:TRANSFORM", ::transform),
		Pair("MODE:SINGLE_TRANSFORM", ::singleTransform),

		Pair("RETRO:REPEAT_LAST_STROKE", ::repeatLastStroke),
		Pair("RETRO:LAST_TRANSLATION", ::lastTranslation),
		Pair("RETRO:LAST_CLUSTER", ::lastCluster),
		Pair("RETRO:MOVE_LAST_CLUSTER", ::moveLastCluster),
		Pair("RETRO:BREAK_TRANSLATION", ::retroBreakTranslation),
		Pair("RETRO:TOGGLE_ASTERISK", ::retroToggleAsterisk)
	),

	aliases = mapOf(
		Pair("-|", "{MODE:SINGLE_TRANSFORM:CAPITALISE}"),
		Pair("<", "{MODE:SINGLE_TRANSFORM:UPPERCASE}"),
		Pair(">", "{MODE:SINGLE_TRANSFORM:LOWERCASE}"),

		Pair(".", "{^.}{-|}"),
		Pair("?", "{^?}{-|}"),
		Pair("!", "{^!}{-|}"),
		Pair(",", "{^~|,}"),
		Pair(";", "{^~|;}"),
		Pair(":", "{^~|:}"),

		Pair("*>", "{>}{RETRO:MOVE_LAST_CLUSTER}"),
		Pair("*<", "{<}{RETRO:MOVE_LAST_CLUSTER}"),
		Pair("*-|", "{-|}{RETRO:MOVE_LAST_CLUSTER}"),
		Pair("*+", "{RETRO:REPEAT_LAST_STROKE}"),
		Pair("*", "{RETRO:TOGGLE_ASTERISK}"),
		Pair("*?", "{RETRO:BREAK_TRANSLATION}"),
		Pair("*!", "{^}{RETRO:MOVE_LAST_CLUSTER}")
	),

	prefixStrokes = listOf(),
	suffixStrokes = listOf("-Z", "-D", "-S", "-G"),

	defaultDictionaries = listOf(
		"dictionaries/main.json",
		"dictionaries/commands.json"),

	defaultFormatting = Formatting(
		spaceStart = Formatting.Space.NORMAL,
		spaceEnd = Formatting.Space.NORMAL,
		space = " ",
		orthography = null,
		transform = ::noneTransform)
)
