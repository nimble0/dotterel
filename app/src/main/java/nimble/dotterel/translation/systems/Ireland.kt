// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation.systems

import nimble.dotterel.translation.*

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

val IRELAND_SYSTEM = System(
	keyLayout = IRELAND_LAYOUT,

	prefixStrokes = listOf(),
	suffixStrokes = listOf("-Z", "-D", "-S", "-G"),

	defaultDictionaries = listOf(
		"dictionaries/main.json",
		"dictionaries/commands.json"),

	defaultFormatting = Formatting(
		spaceStart = Formatting.Space.NORMAL,
		spaceEnd = Formatting.Space.NORMAL,
		space = " ",
		transform = ::noneTransform)
)
