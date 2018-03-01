// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import kotlin.math.max

class HistoryTranslation(
	val translation: Translation,
	val actions: List<Any>,
	context: FormattedText,
	val text: FormattedText)
{
	val replacesText = context.text
		.substring(max(0, context.text.length - text.backspaces))

	val undoable: Boolean = !(this.replacesText.isEmpty() && this.text.text.isEmpty())

	val undoAction = FormattedText(this.text.text.length, this.replacesText)
	val redoAction = this.text
	val undoReplacedAction: FormattedText =
		this.translation.replaces.fold(FormattedText(),
			{ acc, it -> acc + it.undoAction })
	val redoReplacedAction: FormattedText =
		this.translation.replaces.fold(FormattedText(),
			{ acc, it -> acc + it.redoAction })
}
