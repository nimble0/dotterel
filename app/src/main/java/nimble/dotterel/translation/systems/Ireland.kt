// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.systems

import nimble.dotterel.*
import nimble.dotterel.translation.*
import nimble.dotterel.translation.RegexOrthography.Replacement
import nimble.dotterel.util.CaseInsensitiveString

val IRELAND_LAYOUT = KeyLayout(
	"#1S2TK3PW4HR-5A0O*EU-6FR7PB8LG9TSDZ",
	mapOf(
		Pair("1-", listOf("#-", "S-")),
		Pair("2-", listOf("#-", "T-")),
		Pair("3-", listOf("#-", "P-")),
		Pair("4-", listOf("#-", "H-")),
		Pair("5", listOf("#-", "A-")),
		Pair("0", listOf("#-", "O-")),
		Pair("-6", listOf("#-", "-F")),
		Pair("-7", listOf("#-", "-P")),
		Pair("-8", listOf("#-", "-L")),
		Pair("-9", listOf("#-", "-T")))
)

val ENGLISH_ORTHOGRAPHY = RegexOrthography(mutableListOf(
	Replacement(Regex("(?<=[bcdfghjklmnpqrstvwxz] ?)y\uffffs"), "ies"),
	Replacement(Regex("(?<=s|sh|x|z|zh ?)\uffffs"), "es"),
	Replacement(Regex("ie\uffffing"), "ying")
))

val IRELAND_SYSTEM = System(
	keyLayout = IRELAND_LAYOUT,

	orthography = ENGLISH_ORTHOGRAPHY,

	transforms = mapOf(
		Pair("none", ::noneTransform),
		Pair("capitalise", ::capitaliseTransform),
		Pair("uncapitalise", ::uncapitaliseTransform),
		Pair("uppercase", ::upperCaseTransform),
		Pair("lowercase", ::lowerCaseTransform),
		Pair("titlecase", ::titleCaseTransform)
	).mapKeys({ CaseInsensitiveString(it.key) }),

	commands = mapOf(
		Pair("retro:undo", ::undoStroke),
		Pair("mode:transform", ::transform),
		Pair("mode:single_transform", ::singleTransform),

		Pair("retro:repeat_last_stroke", ::repeatLastStroke),
		Pair("retro:last_translation", ::lastTranslation),
		Pair("retro:last_cluster", ::lastCluster),
		Pair("retro:move_last_cluster", ::moveLastCluster),
		Pair("retro:break_translation", ::retroBreakTranslation),
		Pair("retro:toggle_asterisk", ::retroToggleAsterisk),

		Pair("mode:set_space", ::setSpace),
		Pair("mode:reset_case", ::resetTransform),
		Pair("mode:reset_space", ::resetSpace),

		Pair("ime:editor_action", ::editorAction),
		Pair("ime:switch_previous", ::switchPreviousIme),
		Pair("ime:switch_next", ::switchNextIme),
		Pair("ime:switch", ::switchIme),
		Pair("ime:show_picker", ::showImePicker)
	).mapKeys({ CaseInsensitiveString(it.key) }),

	aliases = mapOf(
		Pair("-|", "{MODE:SINGLE_TRANSFORM:CAPITALISE}"),
		Pair("<", "{MODE:SINGLE_TRANSFORM:UPPERCASE}"),
		Pair(">", "{MODE:SINGLE_TRANSFORM:UNCAPITALISE}"),

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
		Pair("*!", "{^}{RETRO:MOVE_LAST_CLUSTER}"),

		Pair("MODE:RESET", "{MODE:RESET_CASE}{MODE:RESET_SPACE}"),
		Pair("MODE:CAPS", "{MODE:TRANSFORM:UPPERCASE}"),
		Pair("MODE:LOWER", "{MODE:TRANSFORM:LOWERCASE}"),
		Pair("MODE:TITLE", "{MODE:TRANSFORM:TITLECASE}"),
		Pair("MODE:CAMEL", "{MODE:TRANSFORM:TITLECASE}{MODE:SET_SPACE:}{^}"),
		Pair("MODE:SNAKE", "{MODE:SET_SPACE:_}")
	).mapKeys({ CaseInsensitiveString(it.key) }),

	prefixStrokes = listOf(),
	suffixStrokes = IRELAND_LAYOUT.parse(listOf("-Z", "-D", "-S", "-G")),

	defaultDictionaries = listOf(
		"asset://dictionaries/main.json",
		"asset://dictionaries/commands.json",
		"code://dictionaries/Numbers"),

	defaultFormatting = Formatting(
		space = " ",
		spaceStart = Formatting.Space.NORMAL,
		spaceEnd = Formatting.Space.NORMAL,
		orthography = null,
		transform = ::noneTransform)
)
