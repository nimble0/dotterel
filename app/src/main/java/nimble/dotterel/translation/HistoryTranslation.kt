// This file is part of Dotterel which is released under GPL-2.0-or-later.
// See file <LICENSE.txt> or go to <http://www.gnu.org/licenses/> for full license details.

package nimble.dotterel.translation

import kotlin.math.max

class HistoryTranslation(
	val strokes: List<Stroke>,
	val replaces: List<HistoryTranslation>,
	val actions: List<Any>,
	context: FormattedText)
{
	val formattedActions = format(context, actions)
	val text = actionsToText(formattedActions)
		?: FormattedText(0, "", context.formatting)
	val replacesText = context.text
		.substring(max(0, context.text.length - this.text.backspaces))

	val undoable: Boolean = !(this.replacesText.isEmpty() && this.text.text.isEmpty())

	val undoAction = FormattedText(this.text.text.length, this.replacesText)
	val redoAction = this.text
	val undoReplacedAction: FormattedText =
		this.replaces.fold(FormattedText(),
			{ acc, it -> acc + it.undoAction })
	val redoReplacedAction: FormattedText =
		this.replaces.fold(FormattedText(),
			{ acc, it -> acc + it.redoAction })
}