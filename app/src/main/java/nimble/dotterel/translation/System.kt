// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import nimble.dotterel.util.CaseInsensitiveString

data class System(
	val keyLayout: KeyLayout,
	val orthography: Orthography,
	val transforms: Map<
		CaseInsensitiveString,
		(FormattedText, UnformattedText, Boolean) -> UnformattedText>,
	val commands: Map<CaseInsensitiveString, (Translator, String) -> TranslationPart>,
	val aliases: Map<CaseInsensitiveString, String>,
	val prefixStrokes: List<Stroke>,
	val suffixStrokes: List<Stroke>,
	val defaultDictionaries: List<String>,
	val defaultFormatting: Formatting
)
